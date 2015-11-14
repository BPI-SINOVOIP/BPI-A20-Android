/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#include <CDX_LogNDebug.h>
//#define LOG_NDEBUG 0


#define LOG_TAG "LiveSession"

#include "include/LiveSession.h"

#include "include/LiveDataSource.h"

#include "include/M3UParser.h"
#include "include/HTTPBase.h"
#include "include/ChromiumHTTPDataSource.h"

#include <cutils/properties.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>

#include <ctype.h>
#include <openssl/aes.h>
#include <openssl/md5.h>

#include <sys/time.h>

namespace android
{

	#define NETWORK_RETRY_TIME			(10*60*1000*1000)//(60*1000*1000)

	static const int32_t				MAX_CONNECT_FAIL_RETRY_COUNT = 0;
	static int32_t						mConnectRetryCount			 = 0;

	LiveSession::LiveSession(uint32_t flags, bool uidValid, uid_t uid)
	{
		mFlags						= flags;
		mUIDValid					= uidValid;
		mUID						= uid;
		mDataSource					= new LiveDataSource;
		mHTTPDataSource				= HTTPBase::Create((mFlags & kFlagIncognito) ? HTTPBase::kFlagIncognito | HTTPBase::kFlagUAIPAD : HTTPBase::kFlagUAIPAD);
		mDataSourceConnected        = false;
		mPrevBandwidthIndex			= -1;
		mLastPlaylistFetchTimeUs	= -1;
		mSeqNumber					= -1;
		mLastSeqNumberBase			= -1;
		mSeekTimeUs					= -1;
		mNumRetries					= 0;
		mDurationUs					= -1;
		mDurationFixed				= true;
		mSeekDone					= false;
		mDisconnectPending			= false;
		mMonitorQueueGeneration		= 0;
		mRefreshState				= INITIAL_MINIMUM_RELOAD_DELAY;
		mHasSeekMsg					= false;
		mLastDownloadTobeContinue	= false;
		mLastDownloadOffset			= 0;
		mLastSubSeqNumber			= 0;
		mConnectRetryCount			= 0;
		mIsPlaylistRedirected		= false;
		mediaPlaylistRedirect		= -1;
		mTimeout					= false;
		mPlaylistRedirectURL		= NULL;
		mCurUri						= NULL;

		if(mUIDValid)
		{
			mHTTPDataSource->setUID(mUID);
		}

		mPlaylistRedirectURL = new char[2048];
	}

	LiveSession::~LiveSession()
	{
		if(mPlaylistRedirectURL)
		{
			delete[] mPlaylistRedirectURL;
			mPlaylistRedirectURL = NULL;
		}
		if(mCurUri) {
			delete mCurUri;
		}
	}

	sp<DataSource> LiveSession::getDataSource()
	{
		return mDataSource;
	}

	void LiveSession::connect(const char *url, const KeyedVector<String8, String8> *headers)
	{
		sp<AMessage> msg = new AMessage(kWhatConnect, id());
		msg->setString("url", url);

		if(headers != NULL)
		{
			msg->setPointer("headers", new KeyedVector<String8, String8>(*headers));
		}

		msg->post();
	}

	void LiveSession::disconnect()
	{
		ALOGD("disconnect ");
		Mutex::Autolock autoLock(mLock);
		
		mDisconnectPending = true;

		mHTTPDataSource->forceDisconnect();
		mDataSourceConnected = false;

		(new AMessage(kWhatDisconnect, id()))->post();
	}

	int64_t LiveSession::seekTo(int64_t timeUs)
	{
		Mutex::Autolock autoLock(mLock);
		
		mSeekDone = false;

		sp<AMessage> msg = new AMessage(kWhatSeek, id());
    
		msg->setInt64("timeUs", timeUs);
		msg->post();

		mHasSeekMsg = true;    //* set this field to stop data downloading.

//		ALOGV("====================: send kWhatSeek message, wait for processing.");
		while (!mSeekDone)
		{
			mCondition.wait(mLock);
		}
		

//		ALOGV("====================: wait kWhatSeek message process finish.");
		return mSeekTargetStartUs;
	}

	void LiveSession::onMessageReceived(const sp<AMessage> &msg)
	{
		switch(msg->what())
		{
        case kWhatConnect:
			ALOGV("====================: onMessageReceived, kWhatConnect.");
            onConnect(msg);
            break;

        case kWhatDisconnect:
			ALOGV("====================: onMessageReceived, kWhatDisconnect.");
            onDisconnect();
            break;

        case kWhatMonitorQueue:
        {
            int32_t generation;

			ALOGV("====================: onMessageReceived, kWhatMonitorQueue.");

            CHECK(msg->findInt32("generation", &generation));

            if (generation != mMonitorQueueGeneration)
			{
                // Stale event
                break;
            }

            onMonitorQueue();
            break;
        }

        case kWhatSeek:
			ALOGV("====================: onMessageReceived, kWhatSeek.");
            onSeek(msg);
            break;

        default:
			ALOGV("====================: onMessageReceived, unknown.");
            TRESPASS();
            break;
		}

		ALOGV("================ onMessageReceive return.");
	}

	int LiveSession::SortByBandwidth(const BandwidthItem *a, const BandwidthItem *b)
	{
		if(a->mBandwidth < b->mBandwidth)
		{
			return -1;
		}
		else if(a->mBandwidth == b->mBandwidth)
		{
			return 0;
		}

		return 1;
	}

	void LiveSession::onConnect(const sp<AMessage> &msg)
	{
		AString url;
		CHECK(msg->findString("url", &url));

		KeyedVector<String8, String8> *headers = NULL;

		if(!msg->findPointer("headers", (void **)&headers))
		{
			mExtraHeaders.clear();
		}
		else
		{
			mExtraHeaders = *headers;
			delete headers;
			headers = NULL;
		}

		if(!(mFlags & kFlagIncognito))
		{
			ALOGI("onConnect '%s'", url.c_str());
		}
		else
		{
			ALOGI("onConnect <URL suppressed>");
		}

		mMasterURL = url;

		bool dummy;

//		ALOGV("xxxxxxxxxxxxxxxxxxxx onConnect: fetchPlaylist xxxxxxxxxxxxxxxxxx");
		sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &dummy);

		if(playlist == NULL)
		{
			ALOGE("unable to fetch master playlist '%s'.", url.c_str());

			mDataSource->queueEOS(ERROR_IO);
			return;
		}

		if(playlist->isVariantPlaylist())
		{
			for (size_t i = 0; i < playlist->size(); ++i)
			{
				BandwidthItem item;

				sp<AMessage> meta;
				playlist->itemAt(i, &item.mURI, &meta);

				unsigned long bandwidth;
				CHECK(meta->findInt32("bandwidth", (int32_t *)&item.mBandwidth));

				mBandwidthItems.push(item);
			}

			CHECK_GT(mBandwidthItems.size(), 0u);

			mBandwidthItems.sort(SortByBandwidth);
		}
		else
		{
			Mutex::Autolock autoLock(mLock);

			mPlaylist = playlist;

			if (!mPlaylist->isComplete())
			{
				mDurationUs = -1;
//				ALOGV("xxxxxxxxxxx mPlaylist->isComplete() return 0");
			}
			else
			{
				mDurationUs = 0;
				for (size_t i = 0; i < mPlaylist->size(); ++i)
				{
					sp<AMessage> itemMeta;
					CHECK(mPlaylist->itemAt(i, NULL /* uri */, &itemMeta));

					int64_t itemDurationUs;

					CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

					mDurationUs += itemDurationUs;
//					ALOGV("xxxxxxxx mDurationUs = %lld", mDurationUs);
				}
			}

//			ALOGV("xxxxxxxxxxxxx calculate duration finish xxxxxxxxxxxxxxx");

			mLastPlaylistFetchTimeUs = ALooper::GetNowUs();
			mPrevBandwidthIndex = getBandwidthIndex();
		}

		mLastDownloadTobeContinue = false;
		mConnectRetryCount		  = 0;

		postMonitorQueue();
	}

	void LiveSession::onDisconnect()
	{
//		ALOGV("xxxxxxxxxxxxxxxxxx onDisconnect xxxxxxxxxxxxxx");

		mDataSource->queueEOS(ERROR_END_OF_STREAM);

		Mutex::Autolock autoLock(mLock);
//		mDisconnectPending = false;     //* abort time out when the download thread is retrying reconnect.

		mLastDownloadTobeContinue = false;
	}

	status_t LiveSession::fetchFile(
	        const char *url, sp<ABuffer> *out,
	        int64_t range_offset, int64_t range_length)
	{
		int64_t        startTime;
		int64_t        curTime;
		int64_t        connectTimeCost;
		struct timeval tv;

		*out = NULL;

		sp<DataSource> source;

//		ALOGV("xxxxxxxxxxxxxxxxx LiveSession::fetchFile xxxxxxxxxxxxxxxx");
		ALOGV("xxxxxxxxxxxxxxxxx fetchFile: url = %s xxxxxxxxxxxxxxx", url);

		if (!strncasecmp(url, "file://", 7))
		{
			source = new FileSource(url + 7);
		}
		else if (strncasecmp(url, "http://", 7) && strncasecmp(url, "https://", 8))
		{
			return ERROR_UNSUPPORTED;
		}
		else
		{
			{
				Mutex::Autolock autoLock(mLock);

				if (mDisconnectPending)
				{
					return ERROR_IO;
				}
			}

			connectTimeCost = 0;
_fetch_file_connect_again:
			if(mDataSourceConnected)
			{
				mHTTPDataSource->disconnect();
				mDataSourceConnected = false;
			}

			if(mDisconnectPending) {
				return -1;
			}

			{
				gettimeofday(&tv, NULL);
				startTime = tv.tv_sec * 1000000ll + tv.tv_usec;
			}

			status_t err = mHTTPDataSource->connect(url, mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders);

			{
				gettimeofday(&tv, NULL);
				curTime = tv.tv_sec * 1000000ll + tv.tv_usec;
				connectTimeCost += (curTime - startTime);
			}

			//* add by chenxiaochuan for QQ live streaming.
			{
				mIsPlaylistRedirected = mHTTPDataSource->isRedirected();
				if(mIsPlaylistRedirected)
				{
					if(mPlaylistRedirectURL != NULL)
					{
						mMasterURL = mHTTPDataSource->getRedirectUri(true);
						strcpy(mPlaylistRedirectURL, mHTTPDataSource->getRedirectUri(true).c_str());
						ALOGV("xxxxxx playlist redirect to %s", mMasterURL.c_str());
						ALOGV("xxxxxx host and port is %s", mPlaylistRedirectURL);
					}
				}
			}
			//* end.

			mLastDownloadTobeContinue = false;

			if (err != OK)
			{
				if(connectTimeCost < NETWORK_RETRY_TIME)
				{
					ALOGV(" xxxxxxxxxxxxxxxxxxxx: fetchFile, fail to connect m3u8 playlist file, retry time %lld.", connectTimeCost);
					usleep(100*1000);
					connectTimeCost += 100*1000;
					goto _fetch_file_connect_again;
				}
				else
				{
					ALOGV(" xxxxxxxxxxxxxxxxxxxx: fetchFile, fail to connect m3u8 playlist file, quit.");
					mDataSource->queueEOS(ERROR_IO);
				}

				return err;
			}

			mDataSourceConnected = true;

			source = mHTTPDataSource;
		}

		off64_t size;
		status_t err = source->getSize(&size);

		if (err != OK)
		{
			size = 65536;
		}

		mBufferForFetchFile.clear();
		mBufferForFetchFile = new ABuffer(size);
		sp<ABuffer> buffer = mBufferForFetchFile;
		buffer->setRange(0, 0);

		for (;;)
		{
			size_t bufferRemaining = buffer->capacity() - buffer->size();

			if (bufferRemaining == 0)
			{
				bufferRemaining = 32768;

//				ALOGV("increasing download buffer to %d bytes", buffer->size() + bufferRemaining);

				sp<ABuffer> copy = new ABuffer(buffer->size() + bufferRemaining);

				memcpy(copy->data(), buffer->data(), buffer->size());

				copy->setRange(0, buffer->size());

				buffer = copy;
			}

			if(mHasSeekMsg)
			{
//				ALOGV("====================: fetchFile, detect a seek msg when downloading data, break downloading.");
				break;
			}

			{
				Mutex::Autolock autoLock(mLock);

				if (mDisconnectPending)
				{
					return ERROR_IO;
				}
			}

			ssize_t n = source->readAt(buffer->size(), buffer->data() + buffer->size(), bufferRemaining);

			if (n < 0)
			{
				return n;
			}

			if (n == 0)
			{
				break;
			}

			buffer->setRange(0, buffer->size() + (size_t)n);

			if(mHasSeekMsg)
			{
//				ALOGV("====================: fetchFile, detect a seek msg when downloading data, break downloading.");
				break;
			}

		}

		*out = buffer;

		return OK;
	}

	int32_t LiveSession::fetchTsData(const char *url, bool continueLast)
	{
		sp<DataSource> source;
		status_t	   err;
		DataNode*	   node = NULL;
		int64_t        startTime;
		int64_t        curTime;
		int64_t        connectTimeCost;
		struct timeval tv;
		sp<ABuffer> buf;
		bool           isCryptedStream;
		AString        method;
		size_t         playlistIndex = 0;
		int32_t        firstSeqNumberInPlaylist;
	    bool           found;
	    sp<AMessage>   itemMeta;
	    unsigned char* dataPtr;
	    size_t         dataSize;
		int            connectRetryCnt;

//		ALOGV("xxxxxxxxxxxxxxxxx LiveSession::fetchTsData xxxxxxxxxxxxxxxx");

		if(url != NULL)
			ALOGV("xxxxxxxxxxxxxxxxx url = %s xxxxxxxxxxxxxxx", url);
		else
			ALOGV("xxxxxxxxxxxxxxxxx url = NULL xxxxxxxxxxxxxx");

		{
			Mutex::Autolock autoLock(mLock);

			if (mDisconnectPending)
			{
				return -1;
			}
		}

		if(continueLast && mLastDownloadTobeContinue)
		{
			source = mHTTPDataSource;
			mLastDownloadTobeContinue = false;
		}
		else
		{
			if(url == NULL)
			{
				err = ERROR_CANNOT_CONNECT;
				return -1;
			}

			//***************************************
			//* chech if it is a AES crypted stream.
			//***************************************
			if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32("media-sequence", &firstSeqNumberInPlaylist))
				firstSeqNumberInPlaylist = 0;
			playlistIndex = mSeqNumber - firstSeqNumberInPlaylist;
			found = false;

		    for (ssize_t i = playlistIndex; i >= 0; --i)
		    {
		        AString uri;
		        mPlaylist->itemAt(i, &uri, &itemMeta);

		        if(itemMeta != NULL)
		        {
			        if (itemMeta->findString("cipher-method", &method))
			        {
			            found = true;
			            break;
			        }
		        }
		    }

		    if (!found)
		        method = "NONE";


		    if (!(method == "AES-128"))
		    {
		    	connectTimeCost = 0;
_fetch_tsdata_connect_again:
				if(mDataSourceConnected)
				{
					mHTTPDataSource->disconnect();
					mDataSourceConnected = false;
				}

				if(mDisconnectPending) {
					return -1;
				}

				{
					gettimeofday(&tv, NULL);
					startTime = tv.tv_sec * 1000000ll + tv.tv_usec;
				}

				err = mHTTPDataSource->connect(url, mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders);

				mLastDownloadTobeContinue = false;
				mLastDownloadOffset		  = 0;
				mLastSubSeqNumber		  = 0;

				{
					gettimeofday(&tv, NULL);
					curTime = tv.tv_sec * 1000000ll + tv.tv_usec;
					connectTimeCost += (curTime - startTime);
				}

				if (err != OK)
				{
					if(connectTimeCost < NETWORK_RETRY_TIME)
					{
						ALOGV(" xxxxxxxxxxxxxxxxxxxx: fetchTsData, fail to connect ts file, retry time %lld.", connectTimeCost);
						usleep(100*1000);
						//connectTimeCost += 100*1000;
						connectTimeCost = 0;
						goto _fetch_tsdata_connect_again;
					}
					else
					{
						ALOGV(" xxxxxxxxxxxxxxxxxxxx: fetchTsData, fail to connect ts file, quit.");
						mDataSource->queueEOS(ERROR_IO);
					}

					return err;
				}

				{
					gettimeofday(&tv, NULL);
					curTime = tv.tv_sec * 1000000ll + tv.tv_usec;
					mDataSource->updataBWEstimator(0, curTime - startTime);
				}

				mDataSourceConnected = true;

				source = mHTTPDataSource;
		    }
		}

		for (;;)
		{
#if 0
			bool           isCryptedStream;
			AString        method;
			size_t         playlistIndex;
			int32_t        firstSeqNumberInPlaylist;
		    bool           found;
		    sp<AMessage>   itemMeta;
		    unsigned char* dataPtr;
		    size_t         dataSize;

			if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32("media-sequence", &firstSeqNumberInPlaylist))
				firstSeqNumberInPlaylist = 0;
			playlistIndex = mSeqNumber - firstSeqNumberInPlaylist;
			found = false;

		    for (ssize_t i = playlistIndex; i >= 0; --i)
		    {
		        AString uri;
		        mPlaylist->itemAt(i, &uri, &itemMeta);

		        if(itemMeta != NULL)
		        {
			        if (itemMeta->findString("cipher-method", &method))
			        {
			            found = true;
			            break;
			        }
		        }
		    }

		    if (!found)
		        method = "NONE";
#endif

		    if (method == "AES-128")
		    {
		    	ALOGD("xxxxxxxxxxxxxxxx AES crypted stream.");

				startTime = ALooper::GetNowUs();

		        status_t err = fetchFile(url, &buf);
		        if (err != OK || buf == NULL)
		        {
		            ALOGE("failed to fetch .ts segment at url '%s'", url);
		            return -1;
		        }

				if(mDataSourceConnected)
				{
					mHTTPDataSource->disconnect();
					mDataSourceConnected = false;
				}

		        if (decryptBuffer(playlistIndex, buf) != OK)
		        {
			        ALOGD("xxxxxxxxxxxx read key error.");
		            ALOGE("decryptBuffer failed w/ error %d", err);
		            return -1;
		        }

				{
					curTime =  ALooper::GetNowUs();
					ALOGV("read data time %lld", curTime - startTime);

					if(buf->size() > 0)
						mDataSource->updataBWEstimator(buf->size(), curTime - startTime);
					else
						mDataSource->updataBWEstimator(0, curTime - startTime);
				}

		        dataPtr  = (unsigned char*)(buf->data());
		        dataSize = buf->size();
		        mLastSubSeqNumber = 0;
		        while(dataSize > 0)
		        {
					node = mDataSource->getBuffer();
					if(node == NULL)
						return 0;

					node->hasBeenRead   = 0;
					node->seqNum		= mSeqNumber;
					node->subSeqNum		= mLastSubSeqNumber;
					node->offset		= 0;

					if(dataSize > (size_t)node->bufSize)
					{
						memcpy(node->data, dataPtr, node->bufSize);
						dataPtr += node->bufSize;
						dataSize -= node->bufSize;
						node->dataSize = node->bufSize;
					}
					else
					{
						memcpy(node->data, dataPtr, dataSize);
						dataPtr += dataSize;
						node->dataSize = dataSize;
						dataSize = 0;
					}

					if(node->subSeqNum == 0)
					{
						//* for QQ's live streaming, there are 8 bytes at the beginning of a ts file,
						//* representing the file id and file size.
						//* we should discard these 8 bytes.
						int offset;
						for(offset = 0; offset < node->dataSize; offset++)
						{
							if(((unsigned char*)node->data)[offset] == 0x47)
								break;
						}

						if(offset < node->dataSize)
							node->offset = offset;
					}

					mDataSource->queueBuffer(node);

					mLastSubSeqNumber++;
		        }

		        return 0;
		    }
		    else
			{
				if(mHasSeekMsg)
				{
					ALOGV("====================: fetchFile, detect a seek msg when downloading data, break downloading.");
					mLastDownloadTobeContinue = true;
					return 1;	//* return a positive value to tell that download process is brocken by seek message.
				}

				node = mDataSource->getBuffer();
				if(node == NULL)
				{
					ALOGV("====================: fetchTsData, can not get node buffer, return to wait.");
					mLastDownloadTobeContinue = true;

					postMonitorQueue();

					return 1;	//*	not really brocken by seek message, but to wait for buffer.
				}

				node->hasBeenRead   = 0;
				node->seqNum		= mSeqNumber;
				node->subSeqNum		= mLastSubSeqNumber;
				node->offset		= 0;

				startTime = ALooper::GetNowUs();
				//add by weihongqiang, to handle case of timeout.
				ssize_t n = 0;
				buf.clear();
				if(mDisconnectPending) {
					return -1;
				}
				if(mTimeout) {
					ALOGI("read data timeout, data to flush %d", mLastDownloadOffset);
					if(mLastDownloadOffset > 0) {
						//keep the buffer valid util we reconnect successfully,
						//to avoid chrashing in case that
						// webkit callback immediatly after we get
						// a timeout error.
						buf = new ABuffer(mLastDownloadOffset);


						n = source->readAt(0, buf->data(), buf->size());

						CHECK((n < 0) || (n == mLastDownloadOffset));
					}
					mTimeout = false;
				}

				if(n >= 0) {
					n = source->readAt(mLastDownloadOffset, node->data, node->bufSize);
				}

				{
					curTime =  ALooper::GetNowUs();
					ALOGV("read data time %lld", curTime - startTime);

					if(n >= 0)
						mDataSource->updataBWEstimator(n, curTime - startTime);
					else
						mDataSource->updataBWEstimator(0, curTime - startTime);
				}

				if(n > 0)
				{
					node->dataSize		 = n;
					mLastDownloadOffset += n;
					mLastSubSeqNumber++;

					if(node->subSeqNum == 0)
					{
						//* for QQ's live streaming, there are 8 bytes at the beginning of a ts file,
						//* representing the file id and file size.
						//* we should discard these 8 bytes.
						int offset;
						for(offset = 0; offset < n; offset++)
						{
							if(((unsigned char*)node->data)[offset] == 0x47)
								break;
						}

						if(offset < n)
							node->offset = offset;
					}

					mDataSource->queueBuffer(node);
				}
				else if (n < 0)
				{
					ALOGV("====================: fetchTsData, read from http return error code %ld.", n);
					if(mDataSourceConnected)
					{
						mHTTPDataSource->disconnect();
						mDataSourceConnected = false;
					}

					if(n == -ETIMEDOUT)
					{
						ALOGI("read data timeout @ %d", mLastDownloadOffset);
						if(mHTTPDataSource->connect(mCurUri->c_str(),
								mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders) != OK) {
							ALOGW("connect to %s failed", url);
							return -1;
						}
						mTimeout = true;
						mDataSourceConnected = true;
					}
					else
					{
						return n;
					}
				}
				else if (n == 0)
				{
					ALOGV("====================: fetchTsData, read from http return zero.");
					if(mDataSourceConnected)
					{
						mHTTPDataSource->disconnect();
						mDataSourceConnected = false;
					}
					break;
				}

				if(mHasSeekMsg)
				{
					ALOGV("====================: fetchFile, detect a seek msg when downloading data, break downloading.");
					mLastDownloadTobeContinue = true;
					return 1;	//* return a positive value to tell that download process is brocken by seek message.
				}
			}
		}

		return OK;
	}

	sp<M3UParser> LiveSession::fetchPlaylist(const char *url, bool *unchanged)
	{
		*unchanged = false;

		sp<ABuffer> buffer;
		if(mDisconnectPending) {
			return NULL;
		}

		ALOGV("xxxxxxxxxxxxxxxx start fetch m3u8 file xxxxxxxxxxxxxxxxxx");
		status_t err = fetchFile(url, &buffer);

#if 0
		//* this code block is added by ChenXiaoChuan.
		{
			FILE* fp = NULL;
			fp = fopen("/data/camera/youku_m3u8.txt", "wb");
			if(fp != NULL)
			{
				ALOGV("xxxxxxxxxxxxxxx save m3u8 data to file, file length = %d.", buffer->size());
				fwrite(buffer->data(), 1, buffer->size(), fp);
				fclose(fp);
			}
			else
			{
				ALOGV("can not create a file to save m3u8 playlist.");
			}
		}
#endif

		ALOGV("xxxxxxxxxxxxxxxx finish fetch m3u8 file xxxxxxxxxxxxxxxxx");

		if (err != OK)
		{
			return NULL;
		}

		// MD5 functionality is not available on the simulator, treat all
		// playlists as changed.

#if defined(HAVE_ANDROID_OS)
		uint8_t hash[16];

		MD5_CTX m;
		MD5_Init(&m);
		MD5_Update(&m, buffer->data(), buffer->size());

		MD5_Final(hash, &m);

		if (mPlaylist != NULL && !memcmp(hash, mPlaylistHash, 16))
		{
			// playlist unchanged

			if(mRefreshState != THIRD_UNCHANGED_RELOAD_ATTEMPT)
			{
				mRefreshState = (RefreshState)(mRefreshState + 1);
			}

			*unchanged = true;

			ALOGV("Playlist unchanged, refresh state is now %d", (int)mRefreshState);

			return NULL;
		}

		memcpy(mPlaylistHash, hash, sizeof(hash));

		mRefreshState = INITIAL_MINIMUM_RELOAD_DELAY;
#endif

		if(mIsPlaylistRedirected && mPlaylistRedirectURL != NULL)
		{
			url = mPlaylistRedirectURL;
			mediaPlaylistRedirect = 0;
			
		}

		sp<M3UParser> playlist = new M3UParser(url, buffer->data(), buffer->size());

		if (playlist->initCheck() != OK)
		{
			ALOGE("failed to parse .m3u8 playlist");
			return NULL;
		}
		
		if(mIsPlaylistRedirected && mPlaylistRedirectURL != NULL 
			&& !playlist->isVariantPlaylist() && mediaPlaylistRedirect == 0)
		{
			mediaPlaylistRedirect = 1;
		}
	return playlist;
	}
	
#if (CEDARX_ANDROID_VERSION == 9)
    int64_t LiveSession::getSegmentStartTimeUs(int32_t seqNumber) const {
        CHECK(mPlaylist != NULL);

        int32_t firstSeqNumberInPlaylist;
        if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                    "media-sequence", &firstSeqNumberInPlaylist)) {
            firstSeqNumberInPlaylist = 0;
        }

        int32_t lastSeqNumberInPlaylist =
            firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;

        CHECK_GE(seqNumber, firstSeqNumberInPlaylist);
        CHECK_LE(seqNumber, lastSeqNumberInPlaylist);

        int64_t segmentStartUs = 0ll;
        for (int32_t index = 0;
                index < seqNumber - firstSeqNumberInPlaylist; ++index) {
            sp<AMessage> itemMeta;
            CHECK(mPlaylist->itemAt(
                        index, NULL /* uri */, &itemMeta));

            int64_t itemDurationUs;
            CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

            segmentStartUs += itemDurationUs;
        }

        return segmentStartUs;
    }	
#endif

	static double uniformRand()
	{
		return (double)rand() / RAND_MAX;
	}

	size_t LiveSession::getBandwidthIndex()
	{
		static int init_flag;
		if (mBandwidthItems.size() == 0)
		{
			return 0;
		}

#if 1
		int32_t bandwidthBps;

		if (mHTTPDataSource != NULL && mHTTPDataSource->estimateBandwidth(&bandwidthBps)) 
		{
//			ALOGV("bandwidth estimated at %.2f kbps", bandwidthBps / 1024.0f);
		}
		else
		{
//			ALOGV("no bandwidth estimate.");
			return 0;  // Pick the lowest bandwidth stream by default.
		}

		char value[PROPERTY_VALUE_MAX];
/*
		if (property_get("media.httplive.max-bw", value, NULL))
		{
			char *end;
			long maxBw = strtoul(value, &end, 10);
			
			if (end > value && *end == '\0')
			{
				if (maxBw > 0 && bandwidthBps > maxBw)
				{
//					ALOGV("bandwidth capped to %ld bps", maxBw);
					bandwidthBps = maxBw;
				}
			}
		}*/

		// Consider only 80% of the available bandwidth usable.
		bandwidthBps = (bandwidthBps * 8) / 10;

		// Pick the highest bandwidth stream below or equal to estimated bandwidth.

		int index = mBandwidthItems.size() - 1;

		while ( index > 0 &&(( mBandwidthItems.itemAt(index).mBandwidth > (size_t)bandwidthBps) ||(mBandwidthItems.itemAt(index).mBandwidth == mBandwidthItems.itemAt(index-1).mBandwidth)))
		{
			--index;
			ALOGV("mBandwidthItems.itemAt(%d).mBandwidth=%lu",index,mBandwidthItems.itemAt(index).mBandwidth);
		} 
		if( bandwidthBps == 0 )
			index = mBandwidthItems.size()-1;
		if( index == 0 && mBandwidthItems.size()>1 && mBandwidthItems.itemAt(0).mBandwidth != mBandwidthItems.itemAt(1).mBandwidth )
			index = mBandwidthItems.size()-1;
		ALOGV("yf index=%d bandwidthBps=%d",index,bandwidthBps);
#elif 0
		// Change bandwidth at random()
		size_t index = uniformRand() * mBandwidthItems.size();
#elif 0
		// There's a 50% chance to stay on the current bandwidth and
		// a 50% chance to switch to the next higher bandwidth (wrapping around
		// to lowest)
		const size_t kMinIndex = 0;

		size_t index;
		if (mPrevBandwidthIndex < 0)
		{
			index = kMinIndex;
		}
		else if (uniformRand() < 0.5)
		{
			index = (size_t)mPrevBandwidthIndex;
		}
		else
		{
			index = mPrevBandwidthIndex + 1;
			if (index == mBandwidthItems.size())
			{
				index = kMinIndex;
			}
		}
#elif 0
		// Pick the highest bandwidth stream below or equal to 1.2 Mbit/sec

		size_t index = mBandwidthItems.size() - 1;
		while (index > 0 && mBandwidthItems.itemAt(index).mBandwidth > 1200000) 
		{
			--index;
		}
#else
//		size_t index = mBandwidthItems.size() - 1;  // Highest bandwidth stream
//		size_t index = 0;  							// Lowest bandwidth stream
//			  size_t index = mBandwidthItems.size()/2;	  // Middle level bandwidth stream


#if 0
			  size_t index=0;

              if( init_flag == 0 )
              {
			int brandwidth;
							
				int i;
				
				mHTTPDataSource->estimateBandwidth(&brandwidth);//mDataSource->getBandwidth()*1000;
				for( i = 0 ; i < mBandwidthItems.size() ; i++ )
				{
					ALOGV("mBandwidthItems.size=%d  (%d).Bandwidth=%d",mBandwidthItems.size(),i,mBandwidthItems.editItemAt(i).mBandwidth);
					if((( mBandwidthItems.editItemAt(i).mBandwidth >= brandwidth) && (brandwidth>= mBandwidthItems.editItemAt(i-1).mBandwidth) ) && i >0 )
					{
						index= i-1; 		
						break;
					}
					if(mBandwidthItems.editItemAt(i).mBandwidth >= brandwidth&&i==0)
					{
						index=0;			
						break;
					}	
				}
				if( i>=mBandwidthItems.size() )
				  index=mBandwidthItems.size()-1;
				if( index < mBandwidthItems.size()/2)
					index = mBandwidthItems.size()/2;
				
				init_flag =index;
				ALOGV("getBandwidthIndex i=%d  index=%d brandwidth=%d",i,index,brandwidth);


          	}
		else 
		     index = init_flag;
			ALOGV("getBandwidthIndex index=%d size=%d",index,mBandwidthItems.size());
			#endif
			/*
			       int brandwidth;
				int i;
				size_t index=0;
				//mHTTPDataSource->estimateBandwidth(&brandwidth);
				brandwidth = mDataSource->getBandwidth()*1000;
				
				for( i = 0 ; i < mBandwidthItems.size() ; i++ )
				{
					ALOGV("mBandwidthItems.size=%d  (%d).Bandwidth=%d   brandwidth=%d",mBandwidthItems.size(),i,mBandwidthItems.editItemAt(i).mBandwidth,brandwidth);
					if( i>0 && (( mBandwidthItems.editItemAt(i).mBandwidth >= brandwidth ) && (brandwidth >= mBandwidthItems.editItemAt(i-1).mBandwidth ) ) )
					{
						index= i-1; 		
						break;
					}
					if(mBandwidthItems.editItemAt(i).mBandwidth  >= brandwidth&&i==0)
					{
						index=0;			
						break;
					}	
				}
				if( i>=mBandwidthItems.size() )
				     index=mBandwidthItems.size()-1;
				if( index == mBandwidthItems.size()-1)
				{
				     init_flag++;
                                 if( index > 0 && init_flag <= 10)
    	                              index--;
				     if( init_flag >10)
					   	init_flag = 0;
				}
				else 
				{
				      init_flag = 0;
				    
				}
				
				ALOGV("getBandwidthIndex i=%d  index=%d brandwidth=%d",i,index,brandwidth);
			*/


			
#if 1   //* PPTV give invalid url which contains two port number ":80:80" like
		//* "http://124.232.155.249:80:80/live/5/45/2ff29891287248c7aeb557129fd2b772.m3u8?chid=300175&pre=ikan&type=m3u8.web.pad&"
		//* this code is temporarily added two solve this problem until pptv fix this problem.
		{
			AString url;
			if(mBandwidthItems.size() > 1)
			{
				url = mBandwidthItems.editItemAt(index).mURI;
				if(strstr(url.c_str(), ":80:80") != NULL)
				{
					index = 0;
				}
			}
		}
#endif

#endif

		return index;
	}

	bool LiveSession::timeToRefreshPlaylist(int64_t nowUs) const
	{
		if (mPlaylist == NULL)
		{
			CHECK_EQ((int)mRefreshState, (int)INITIAL_MINIMUM_RELOAD_DELAY);
			return true;
		}

		int32_t targetDurationSecs;
		CHECK(mPlaylist->meta()->findInt32("target-duration", &targetDurationSecs));

		int64_t targetDurationUs = targetDurationSecs * 1000000ll;

		int64_t minPlaylistAgeUs;
#if 1
		switch (mRefreshState)
		{
			case INITIAL_MINIMUM_RELOAD_DELAY:
			{
//				ALOGV(" $$$$$$$$$$$$$$$$$$$$: mRefreshState = INITIAL_MINIMUM_RELOAD_DELAY");
				size_t n = mPlaylist->size();
            
				if (n > 0)
				{
					sp<AMessage> itemMeta;
					CHECK(mPlaylist->itemAt(n - 1, NULL /* uri */, &itemMeta));

					int64_t itemDurationUs;
					CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

					minPlaylistAgeUs = itemDurationUs;
					break;
				}

				// fall through
			}

			case FIRST_UNCHANGED_RELOAD_ATTEMPT:
			{
//				ALOGV(" $$$$$$$$$$$$$$$$$$$$: mRefreshState = FIRST_UNCHANGED_RELOAD_ATTEMPT");
				minPlaylistAgeUs = targetDurationUs / 2;
				break;
			}

			case SECOND_UNCHANGED_RELOAD_ATTEMPT:
			{
//				ALOGV(" $$$$$$$$$$$$$$$$$$$$: mRefreshState = SECOND_UNCHANGED_RELOAD_ATTEMPT");
				minPlaylistAgeUs = (targetDurationUs * 3) / 2;
				break;
			}

			case THIRD_UNCHANGED_RELOAD_ATTEMPT:
			{
//				ALOGV(" $$$$$$$$$$$$$$$$$$$$: mRefreshState = THIRD_UNCHANGED_RELOAD_ATTEMPT");
				minPlaylistAgeUs = targetDurationUs * 3;
				break;
			}

			default:
				TRESPASS();
				break;
		}
#else
		//* this code is added by chenxiaochuan.
		minPlaylistAgeUs = targetDurationUs * 3;
#endif
//		ALOGV(" $$$$$$$$$$$$$$$$$$$$: minPlaylistAgeUs = %lld", minPlaylistAgeUs);

		return mLastPlaylistFetchTimeUs + minPlaylistAgeUs <= nowUs;
	}

	void LiveSession::onDownloadNext()
	{
		int64_t startTime;
		int64_t curTime;
		struct timeval tv;

		size_t bandwidthIndex = getBandwidthIndex();

//		ALOGV("xxxxxxxxxxxxxx onDownloadNext xxxxxxxxxxxxxxx");

rinse_repeat:
		int64_t nowUs = ALooper::GetNowUs();

		ALOGV("mLastPlaylistFetchTimeUs = %lld, bandwidthIndex = %d, mPrevBandwidthIndex = %d", mLastPlaylistFetchTimeUs, bandwidthIndex, (int)mPrevBandwidthIndex);

		if ((mLastPlaylistFetchTimeUs < 0  ||
			(ssize_t)bandwidthIndex != mPrevBandwidthIndex ||
			(!mPlaylist->isComplete() && timeToRefreshPlaylist(nowUs))) &&
			!mLastDownloadTobeContinue)
		{
			AString url;

//			ALOGV("xxxxxxxxxxxxx onDownloadNext: reload the playlist xxxxxxxxxxxxx");

			if (mBandwidthItems.size() > 0 && bandwidthIndex < mBandwidthItems.size())
			{
				url = mBandwidthItems.editItemAt(bandwidthIndex).mURI;
			}
			else
			{
				url = mMasterURL;
			}

			bool firstTime = (mPlaylist == NULL);

			if ((ssize_t)bandwidthIndex != mPrevBandwidthIndex)
			{
				// If we switch bandwidths, do not pay any heed to whether
				// playlists changed since the last time...
				mPlaylist.clear();
			}

			{
				gettimeofday(&tv, NULL);
				startTime = tv.tv_sec * 1000000ll + tv.tv_usec;
			}

			bool unchanged;
			sp<M3UParser> playlist = fetchPlaylist(url.c_str(), &unchanged);

			{
				gettimeofday(&tv, NULL);
				curTime = tv.tv_sec * 1000000ll + tv.tv_usec;
				if(mDataSource != NULL)
					mDataSource->updataBWEstimator(0, curTime - startTime);
			}

			if (playlist == NULL)
			{
				if (unchanged)
				{
					// We succeeded in fetching the playlist, but it was
					// unchanged from the last time we tried.
				}
				else
				{
//					ALOGV("failed to load playlist at url '%s'", url.c_str());
					mDataSource->queueEOS(ERROR_IO);
					return;
				}
			}
			else
			{
				if(mPlaylist != NULL)
					mPlaylist.clear();

				mPlaylist = playlist;

				if(mIsPlaylistRedirected && mPlaylistRedirectURL != NULL 
					&& mediaPlaylistRedirect == 1 && mBandwidthItems.size() > 0)
				{
					mBandwidthItems.editItemAt(bandwidthIndex).mURI = mPlaylistRedirectURL;
					mediaPlaylistRedirect = -1;
				}
			}

//			ALOGV("xxxxxxxxxxxxxxxxxxx start to calculate xxxxxxxxxxxxxxxxxxxx.");
			if (firstTime)
			{
				Mutex::Autolock autoLock(mLock);

				if (!mPlaylist->isComplete()) 
				{
					mDurationUs = -1;
//					ALOGV("xxxxxxxxxxx mPlaylist->isComplete() return 0");
				}
				else
				{
					mDurationUs = 0;
					for (size_t i = 0; i < mPlaylist->size(); ++i) 
					{
						sp<AMessage> itemMeta;
						CHECK(mPlaylist->itemAt(i, NULL /* uri */, &itemMeta));

						int64_t itemDurationUs;
						CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

						mDurationUs += itemDurationUs;
//						ALOGV("xxxxxxxx mDurationUs = %lld", mDurationUs);
					}
				}
			}
        
//			ALOGV("xxxxxxxxxxxxx calculate duration finish xxxxxxxxxxxxxxx");

			mLastPlaylistFetchTimeUs = ALooper::GetNowUs();
		}

		int32_t firstSeqNumberInPlaylist;

		if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32("media-sequence", &firstSeqNumberInPlaylist))
		{
			firstSeqNumberInPlaylist = 0;
		}

//		ALOGV(" $$$$$$$$$$$$$$$$$$$$: firstSeqNumberInPlaylist = %d", firstSeqNumberInPlaylist);

		bool seekDiscontinuity		= false;
		bool explicitDiscontinuity	= false;
		bool bandwidthChanged		= false;

		if (mSeekTimeUs >= 0)
		{
			mSeekTargetStartUs = 0;
        
			if (mPlaylist->isComplete())
			{
				size_t index			= 0;
				int64_t segmentStartUs	= 0;
				int64_t itemDurationUs;

				while (index < mPlaylist->size())
				{
					sp<AMessage> itemMeta;
					CHECK(mPlaylist->itemAt(index, NULL /* uri */, &itemMeta));

					CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

					if (mSeekTimeUs < segmentStartUs + itemDurationUs) 
					{
						break;
					}

					segmentStartUs		+= itemDurationUs;
					mSeekTargetStartUs	+= itemDurationUs;
					++index;
				}

				if(index >= mPlaylist->size())
				{
					index = mPlaylist->size() - 1;
					mSeekTargetStartUs -= itemDurationUs;
				}

				if (index < mPlaylist->size())
				{
					int32_t newSeqNumber = firstSeqNumberInPlaylist + index;
					int32_t firstSeqNumInBuffer = mDataSource->getFirstSeqNumInBuffer();
					int32_t lastSeqNumInBuffer  = mDataSource->getLastSeqNumInBuffer();

//					ALOGV("====================: onDownloadNext, seek to sequence no %d, firstSeqNumInBuffer = %d, lastSeqNumInBuffer = %d.",
//							newSeqNumber, firstSeqNumInBuffer, lastSeqNumInBuffer);

					if(newSeqNumber >= firstSeqNumInBuffer && newSeqNumber <= lastSeqNumInBuffer)
					{
						//* the seek position is already in buffer.
//						ALOGV("====================: onDownloadNext, seek to sequence number %d and continue download.", newSeqNumber);
						mDataSource->seekToSeqNum(newSeqNumber);
						postMonitorQueue();

						mSeekTimeUs		= -1;

						Mutex::Autolock autoLock(mLock);
						mSeekDone   = true;
						mHasSeekMsg = false;
						mCondition.broadcast();
						return;
					}
					else
					{
//						ALOGV("====================: onDownloadNext, reset mDataSource.");

						mSeqNumber = newSeqNumber;

						mDataSource->reset();
						
						mLastDownloadTobeContinue = false;
					}
				}
			}

			mSeekTimeUs = -1;
			mConnectRetryCount = 0;

			Mutex::Autolock autoLock(mLock);
			mSeekDone	= true;
			mHasSeekMsg = false;
			mCondition.broadcast();
		}
                mBufer_precent = (double )mSeqNumber /mPlaylist->size()*100.0;
		
		int32_t lastSeqNumberInPlaylist = firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;
		ALOGV("mBufer_precent=%d  mSeqNumber=%d  mPlaylist->size()=%d firstSeqNumberInPlaylist=%d  lastSeqNumberInPlaylist=%d",mBufer_precent,mSeqNumber,mPlaylist->size(),firstSeqNumberInPlaylist,lastSeqNumberInPlaylist);		

		if (mSeqNumber < 0)
		{
#ifndef TS_RESET_URL //it can't be open in shifttime function
			if(!mPlaylist->isComplete())
			{
				mSeqNumber = lastSeqNumberInPlaylist - 2;
				if(mSeqNumber < firstSeqNumberInPlaylist)
					mSeqNumber = firstSeqNumberInPlaylist;
			}
			else
#endif				
				mSeqNumber = firstSeqNumberInPlaylist;
		}

		if (mSeqNumber < firstSeqNumberInPlaylist || mSeqNumber > lastSeqNumberInPlaylist)
		{
			if (mPrevBandwidthIndex != (ssize_t)bandwidthIndex)
			{
				// Go back to the previous bandwidth.

//				ALOGI("new bandwidth does not have the sequence number "
//					 "we're looking for, switching back to previous bandwidth");

				ALOGV("bandwidth index error, mPrevBandwidthIndex = %d, bandwidthIndex = %d", mPrevBandwidthIndex, bandwidthIndex);
				mLastPlaylistFetchTimeUs = -1;
				bandwidthIndex = mPrevBandwidthIndex;
				goto rinse_repeat;
			}

			ALOGV("xxxxxxxxxxxxxxxx mNumRetries = %d", mNumRetries);
			ALOGV("xxxxxx cur = %d, first = %d, last = %d.", mSeqNumber, firstSeqNumberInPlaylist, lastSeqNumberInPlaylist);

			if (!mPlaylist->isComplete() && mNumRetries < kMaxNumRetries)
			{
				++mNumRetries;

				if (mSeqNumber > lastSeqNumberInPlaylist)
				{
					mLastPlaylistFetchTimeUs = -1;
					postMonitorQueue(3000000ll);
					return;
				}
                if((firstSeqNumberInPlaylist==0) && (lastSeqNumberInPlaylist==-1))
                {
                    ALOGD("*************firstSeqNumberInPlaylist=0, lastSeqNumberInPlaylist=-1\n");
                    mLastPlaylistFetchTimeUs = -1;
					postMonitorQueue(3000000ll);
					return;
                }
                

				// we've missed the boat, let's start from the lowest sequence
				// number available and signal a discontinuity.

				ALOGI("We've missed the boat, restarting playback.");
				mSeqNumber = lastSeqNumberInPlaylist;
				explicitDiscontinuity = true;

				// fall through
			}
			else
			{
				ALOGE("Cannot find sequence number %d in playlist "
					 "(contains %d - %d)", mSeqNumber, firstSeqNumberInPlaylist, firstSeqNumberInPlaylist + mPlaylist->size() - 1);

				if (!mPlaylist->isComplete())
				{
					mSeqNumber = lastSeqNumberInPlaylist;
					explicitDiscontinuity = true;
				}
				else
				{
					mDataSource->queueEOS(ERROR_END_OF_STREAM);
					return;
				}
			}
		}

		mNumRetries = 0;

		int32_t val;

		if(mLastDownloadTobeContinue)
		{
			ALOGV("====================: onDownloadNext, continue last download.");
			val = fetchTsData(NULL, true);
			if(val > 0)
			{
				//* download process break by seek message.
				ALOGV("====================: onDownloadNext, fetchTsData() return a code to tell download break by a message.");
				return;
			}
			else if(val < 0)
			{
				ALOGV("====================: onDownloadNext, failed to fetch .ts segment when continue last download process.");
				mConnectRetryCount++;
				if(mConnectRetryCount >= MAX_CONNECT_FAIL_RETRY_COUNT)
				{
					if(mPlaylist->isComplete())
					{
						mDataSource->queueEOS((status_t)val);
						return;
					}
				}
				else
				{
					usleep(100*1000);
					postMonitorQueue();
					return;
				}
			}
		}
		else
		{
			AString		 uri;
			sp<AMessage> itemMeta;

			ALOGV("====================: onDownloadNext, start to download a new file. mSeqNumber=%d  firstSeqNumberInPlaylist=%d",mSeqNumber,firstSeqNumberInPlaylist);
			if( mSeqNumber - firstSeqNumberInPlaylist > mPlaylist->size())
				return ;//yf
			mPlaylist->itemAt(mSeqNumber - firstSeqNumberInPlaylist, &uri, &itemMeta);
			//CHECK(mPlaylist->itemAt(mSeqNumber - firstSeqNumberInPlaylist, &uri, &itemMeta));
   
			if(itemMeta->findInt32("discontinuity", &val) && val != 0)
				explicitDiscontinuity = true;

			if(mCurUri) {
				delete mCurUri;
				mCurUri = NULL;
			}
			mCurUri = new AString(uri.c_str());
			val = fetchTsData(uri.c_str(), false);
			if(val > 0)
			{
				//* download process break by seek message.
				ALOGV("====================: onDownloadNext, fetchTsData() return a code to tell download break by a message.");
				return;
			}
			else if(val < 0)
			{
				ALOGV("====================: onDownloadNext, fail to fetch .ts segment at url '%s'.", uri.c_str());
				mConnectRetryCount++;
				if(mConnectRetryCount >= MAX_CONNECT_FAIL_RETRY_COUNT)
				{
					if(mPlaylist->isComplete())
					{
						mDataSource->queueEOS((status_t)val);
						return;
					}
				}
				else
				{
					usleep(100*1000);
					postMonitorQueue();
					return;
				}
			}
		}

		//* Connect ok, reset retry count.
		mConnectRetryCount = 0;

		mPrevBandwidthIndex = bandwidthIndex;
		++mSeqNumber;

		postMonitorQueue();
	}

	void LiveSession::onMonitorQueue()
	{
		bool needDownloadNewData;

		if(mSeekTimeUs >= 0)
			ALOGV("mSeekTimeUs = %lld", mSeekTimeUs);

		needDownloadNewData = mDataSource->needBufferNewData();

//		ALOGV("====================: onMonitorQueue, needDownloadNewData = %d", (int)needDownloadNewData);

		if (mSeekTimeUs >= 0 || needDownloadNewData)
		{
//			ALOGV("xxxxxxx onMonitorQueue: call onDownloadNext()");
			onDownloadNext();
		}
		else
		{
//			ALOGV("xxxxxxx onMonitorQueue: call postMonitorQueue()");
			postMonitorQueue(1000000ll);
		}
	}

	status_t LiveSession::decryptBuffer(size_t playlistIndex, const sp<ABuffer> &buffer)
	{
		sp<AMessage>	itemMeta;
		bool			found = false;
		AString			method;

		for (ssize_t i = playlistIndex; i >= 0; --i) 
		{
			AString uri;

			CHECK(mPlaylist->itemAt(i, &uri, &itemMeta));

			if (itemMeta->findString("cipher-method", &method)) 
			{
				found = true;
				break;
			}
		}

		if (!found)
		{
			method = "NONE";
		}

		if (method == "NONE")
		{
			return OK;
		}
		else if (!(method == "AES-128"))
		{
			ALOGE("Unsupported cipher method '%s'", method.c_str());
			return ERROR_UNSUPPORTED;
		}

		AString keyURI;
		
		if (!itemMeta->findString("cipher-uri", &keyURI))
		{
			ALOGE("Missing key uri");
			return ERROR_MALFORMED;
		}

		ssize_t index = mAESKeyForURI.indexOfKey(keyURI);

		sp<ABuffer> key;
		if (index >= 0)
		{
			key = mAESKeyForURI.valueAt(index);
		}
		else
		{
			key = new ABuffer(16);

			sp<HTTPBase> keySource = HTTPBase::Create((mFlags & kFlagIncognito) ? HTTPBase::kFlagIncognito | HTTPBase::kFlagUAIPAD : HTTPBase::kFlagUAIPAD);

			if (mUIDValid)
			{
				keySource->setUID(mUID);
			}

			status_t err = keySource->connect(keyURI.c_str(), mExtraHeaders.isEmpty() ? NULL : &mExtraHeaders);

			if (err == OK)
			{
				size_t offset = 0;
            
				while (offset < 16)
				{
					ssize_t n = keySource->readAt(offset, key->data() + offset, 16 - offset);
                
					if (n <= 0)
					{
						err = ERROR_IO;
						break;
					}

					offset += n;
				}
			}

			if (err != OK)
			{
				ALOGE("failed to fetch cipher key from '%s'.", keyURI.c_str());
				return ERROR_IO;
			}

			mAESKeyForURI.add(keyURI, key);
		}

		AES_KEY aes_key;
		if (AES_set_decrypt_key(key->data(), 128, &aes_key) != 0) 
		{
			ALOGE("failed to set AES decryption key.");
			return UNKNOWN_ERROR;
		}

		unsigned char aes_ivec[16];

		AString iv;
    
		if (itemMeta->findString("cipher-iv", &iv))
		{
			if ((!iv.startsWith("0x") && !iv.startsWith("0X")) || iv.size() != 16 * 2 + 2) 
			{
				ALOGE("malformed cipher IV '%s'.", iv.c_str());
				return ERROR_MALFORMED;
			}

			memset(aes_ivec, 0, sizeof(aes_ivec));
        
			for (size_t i = 0; i < 16; ++i)
			{
				char c1 = tolower(iv.c_str()[2 + 2 * i]);
				char c2 = tolower(iv.c_str()[3 + 2 * i]);
            
				if (!isxdigit(c1) || !isxdigit(c2))
				{
					ALOGE("malformed cipher IV '%s'.", iv.c_str());
					return ERROR_MALFORMED;
				}
            
				uint8_t nibble1 = isdigit(c1) ? c1 - '0' : c1 - 'a' + 10;
				uint8_t nibble2 = isdigit(c2) ? c2 - '0' : c2 - 'a' + 10;

				aes_ivec[i] = nibble1 << 4 | nibble2;
			}
		}
		else
		{
			memset(aes_ivec, 0, sizeof(aes_ivec));
			aes_ivec[15] = mSeqNumber & 0xff;
			aes_ivec[14] = (mSeqNumber >> 8) & 0xff;
			aes_ivec[13] = (mSeqNumber >> 16) & 0xff;
			aes_ivec[12] = (mSeqNumber >> 24) & 0xff;
		}

		AES_cbc_encrypt(buffer->data(), buffer->data(), buffer->size(), &aes_key, aes_ivec, AES_DECRYPT);

		// hexdump(buffer->data(), buffer->size());

		size_t n = buffer->size();
		CHECK_GT(n, 0u);

		size_t pad = buffer->data()[n - 1];

		CHECK_GT(pad, 0u);
		CHECK_LE(pad, 16u);
		CHECK_GE((size_t)n, pad);
    
		for (size_t i = 0; i < pad; ++i) 
		{
			CHECK_EQ((unsigned)buffer->data()[n - 1 - i], pad);
		}

		n -= pad;

		buffer->setRange(buffer->offset(), n);

		return OK;
	}

	void LiveSession::postMonitorQueue(int64_t delayUs) 
	{
		sp<AMessage> msg = new AMessage(kWhatMonitorQueue, id());
		msg->setInt32("generation", ++mMonitorQueueGeneration);
		msg->post(delayUs);
	}

	void LiveSession::onSeek(const sp<AMessage> &msg)
	{
		int64_t timeUs;
		CHECK(msg->findInt64("timeUs", &timeUs));

		mSeekTimeUs = timeUs;
		postMonitorQueue();
	}

	status_t LiveSession::getDuration(int64_t *durationUs) const
	{
		Mutex::Autolock autoLock(mLock);

    //ALOGV("xxxxxxxxxxxxx LiveSession::getDuration, mDuration = %lld", mDurationUs);
		*durationUs = mDurationUs;

		return OK;
	}

	bool LiveSession::isSeekable() const
	{
		int64_t durationUs;
		return getDuration(&durationUs) == OK && durationUs >= 0;
	}

    bool LiveSession::hasDynamicDuration()  const {
    return !mDurationFixed;
    }

}  // namespace android

