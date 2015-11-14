
#include "CameraDebug.h"
#if DBG_CALLBACK
#define LOG_NDEBUG 0
#endif
#define LOG_TAG "CallbackNotifier"
#include <cutils/log.h>

#include <cutils/properties.h>

#include "V4L2CameraDevice.h"
#include "CallbackNotifier.h"

extern "C" int JpegEnc(void * pBufOut, int * bufSize, JPEG_ENC_t *jpeg_enc);

namespace android {

/* String representation of camera messages. */
static const char* lCameraMessages[] =
{
    "CAMERA_MSG_ERROR",
    "CAMERA_MSG_SHUTTER",
    "CAMERA_MSG_FOCUS",
    "CAMERA_MSG_ZOOM",
    "CAMERA_MSG_PREVIEW_FRAME",
    "CAMERA_MSG_VIDEO_FRAME",
    "CAMERA_MSG_POSTVIEW_FRAME",
    "CAMERA_MSG_RAW_IMAGE",
    "CAMERA_MSG_COMPRESSED_IMAGE",
    "CAMERA_MSG_RAW_IMAGE_NOTIFY",
    "CAMERA_MSG_PREVIEW_METADATA",
    "CAMERA_MSG_CONTINUOUSSNAP",
    "CAMERA_MSG_SNAP"
    "CAMERA_MSG_SNAP_THUMB"
};
static const int lCameraMessagesNum = sizeof(lCameraMessages) / sizeof(char*);

/* Builds an array of strings for the given set of messages.
 * Param:
 *  msg - Messages to get strings for,
 *  strings - Array where to save strings
 *  max - Maximum number of entries in the array.
 * Return:
 *  Number of strings saved into the 'strings' array.
 */
static int GetMessageStrings(uint32_t msg, const char** strings, int max)
{
    int index = 0;
    int out = 0;
    while (msg != 0 && out < max && index < lCameraMessagesNum) {
        while ((msg & 0x1) == 0 && index < lCameraMessagesNum) {
            msg >>= 1;
            index++;
        }
        if ((msg & 0x1) != 0 && index < lCameraMessagesNum) {
            strings[out] = lCameraMessages[index];
            out++;
            msg >>= 1;
            index++;
        }
    }

    return out;
}

/* Logs messages, enabled by the mask. */
#if DEBUG_MSG
static void PrintMessages(uint32_t msg)
{
    const char* strs[lCameraMessagesNum];
    const int translated = GetMessageStrings(msg, strs, lCameraMessagesNum);
    for (int n = 0; n < translated; n++) {
        LOGV("    %s", strs[n]);
    }
}
#else
#define PrintMessages(msg)   (void(0))
#endif

static void formatToNV21(void *dst,
		               void *src,
		               int width,
		               int height,
		               size_t stride,
		               uint32_t offset,
		               unsigned int bytesPerPixel,
		               size_t length,
		               int pixelFormat)
{
    unsigned int alignedRow, row;
    unsigned char *bufferDst, *bufferSrc;
    unsigned char *bufferDstEnd, *bufferSrcEnd;
    uint16_t *bufferSrc_UV;

    unsigned int y_uv[2];
    y_uv[0] = (unsigned int)src;
	y_uv[1] = (unsigned int)src + width*height;

	// NV12 -> NV21
    if (pixelFormat == V4L2_PIX_FMT_NV12) {
        bytesPerPixel = 1;
        bufferDst = ( unsigned char * ) dst;
        bufferDstEnd = ( unsigned char * ) dst + width*height*bytesPerPixel;
        bufferSrc = ( unsigned char * ) y_uv[0] + offset;
        bufferSrcEnd = ( unsigned char * ) ( ( size_t ) y_uv[0] + length + offset);
        row = width*bytesPerPixel;
        alignedRow = stride-width;
        int stride_bytes = stride / 8;
        uint32_t xOff = offset % stride;
        uint32_t yOff = offset / stride;

        // going to convert from NV12 here and return
        // Step 1: Y plane: iterate through each row and copy
        for ( int i = 0 ; i < height ; i++) {
            memcpy(bufferDst, bufferSrc, row);
            bufferSrc += stride;
            bufferDst += row;
            if ( ( bufferSrc > bufferSrcEnd ) || ( bufferDst > bufferDstEnd ) ) {
                break;
            }
        }

        bufferSrc_UV = ( uint16_t * ) ((uint8_t*)y_uv[1] + (stride/2)*yOff + xOff);

        uint16_t *bufferDst_UV;

        // Step 2: UV plane: convert NV12 to NV21 by swapping U & V
        bufferDst_UV = (uint16_t *) (((uint8_t*)dst)+row*height);

        for (int i = 0 ; i < height/2 ; i++, bufferSrc_UV += alignedRow/2) {
            int n = width;
            asm volatile (
            "   pld [%[src], %[src_stride], lsl #2]                         \n\t"
            "   cmp %[n], #32                                               \n\t"
            "   blt 1f                                                      \n\t"
            "0: @ 32 byte swap                                              \n\t"
            "   sub %[n], %[n], #32                                         \n\t"
            "   vld2.8  {q0, q1} , [%[src]]!                                \n\t"
            "   vswp q0, q1                                                 \n\t"
            "   cmp %[n], #32                                               \n\t"
            "   vst2.8  {q0,q1},[%[dst]]!                                   \n\t"
            "   bge 0b                                                      \n\t"
            "1: @ Is there enough data?                                     \n\t"
            "   cmp %[n], #16                                               \n\t"
            "   blt 3f                                                      \n\t"
            "2: @ 16 byte swap                                              \n\t"
            "   sub %[n], %[n], #16                                         \n\t"
            "   vld2.8  {d0, d1} , [%[src]]!                                \n\t"
            "   vswp d0, d1                                                 \n\t"
            "   cmp %[n], #16                                               \n\t"
            "   vst2.8  {d0,d1},[%[dst]]!                                   \n\t"
            "   bge 2b                                                      \n\t"
            "3: @ Is there enough data?                                     \n\t"
            "   cmp %[n], #8                                                \n\t"
            "   blt 5f                                                      \n\t"
            "4: @ 8 byte swap                                               \n\t"
            "   sub %[n], %[n], #8                                          \n\t"
            "   vld2.8  {d0, d1} , [%[src]]!                                \n\t"
            "   vswp d0, d1                                                 \n\t"
            "   cmp %[n], #8                                                \n\t"
            "   vst2.8  {d0[0],d1[0]},[%[dst]]!                             \n\t"
            "   bge 4b                                                      \n\t"
            "5: @ end                                                       \n\t"
#ifdef NEEDS_ARM_ERRATA_754319_754320
            "   vmov s0,s0  @ add noop for errata item                      \n\t"
#endif
            : [dst] "+r" (bufferDst_UV), [src] "+r" (bufferSrc_UV), [n] "+r" (n)
            : [src_stride] "r" (stride_bytes)
            : "cc", "memory", "q0", "q1"
            );
        }
    }
}

static bool yuv420spDownScale(void* psrc, void* pdst, int src_w, int src_h, int dst_w, int dst_h)
{
	char * psrc_y = (char *)psrc;
	char * pdst_y = (char *)pdst;
	char * psrc_uv = (char *)psrc + src_w * src_h;
	char * pdst_uv = (char *)pdst + dst_w * dst_h;
	
	int scale = 1;
	int scale_w = src_w / dst_w;
	int scale_h = src_h / dst_h;
	int h, w;
	
	if (dst_w > src_w
		|| dst_h > src_h)
	{
		LOGE("error size, %dx%d -> %dx%d\n", src_w, src_h, dst_w, dst_h);
		return false;
	}
	
	if (scale_w == scale_h)
	{
		scale = scale_w;
	}
	
	LOGV("scale = %d\n", scale);

	if (scale == 1)
	{
		if ((src_w == dst_w)
			&& (src_h == dst_h))
		{
			memcpy((char*)pdst, (char*)psrc, dst_w * dst_h * 3/2);
		}
		else
		{
			// crop
			for (h = 0; h < dst_h; h++)
			{
				memcpy((char*)pdst + h * dst_w, (char*)psrc + h * src_w, dst_w);
			}
			for (h = 0; h < dst_h / 2; h++)
			{
				memcpy((char*)pdst_uv + h * dst_w, (char*)psrc_uv + h * src_w, dst_w);
			}
		}
		
		return true;
	}
	
	for (h = 0; h < dst_h; h++)
	{
		for (w = 0; w < dst_w; w++)
		{
			*(pdst_y + h * dst_w + w) = *(psrc_y + h * scale * src_w + w * scale);
		}
	}
	for (h = 0; h < dst_h / 2; h++)
	{
		for (w = 0; w < dst_w; w+=2)
		{
			*((short*)(pdst_uv + h * dst_w + w)) = *((short*)(psrc_uv + h * scale * src_w + w * scale));
		}
	}
	
	return true;
}

CallbackNotifier::CallbackNotifier()
    : mNotifyCB(NULL),
      mDataCB(NULL),
      mDataCBTimestamp(NULL),
      mGetMemoryCB(NULL),
      mCallbackCookie(NULL),
      mMessageEnabler(0),
      mVideoRecEnabled(false),
      mSavePictureCnt(0),
      mSavePictureMax(0),
      mJpegQuality(90),
      mJpegRotate(0),
      mPictureWidth(640),
      mPictureHeight(480),
	  mThumbWidth(0),
	  mThumbHeight(0),
      mGpsLatitude(0.0),
	  mGpsLongitude(0.0),
	  mGpsAltitude(0),
	  mGpsTimestamp(0),
	  mFocalLength(0.0),
	  mWhiteBalance(0),
	  mCBWidth(0),
	  mCBHeight(0),
	  mBufferList(NULL),
	  mSaveThreadExited(false)
{
	LOGV("CallbackNotifier construct");
	
	memset(mGpsMethod, 0, sizeof(mGpsMethod));
	memset(mCallingProcessName, 0, sizeof(mCallingProcessName));
	
	strcpy(mExifMake, "MID MAKE");		// default
	strcpy(mExifModel, "MID MODEL");	// default
	memset(mDateTime, 0, sizeof(mDateTime));
	strcpy(mDateTime, "2012:10:24 17:30:00");

	memset(mFolderPath, 0, sizeof(mFolderPath));
	memset(mSnapPath, 0, sizeof(mSnapPath));
}

CallbackNotifier::~CallbackNotifier()
{
	LOGV("CallbackNotifier disconstruct");
}

/****************************************************************************
 * Camera API
 ***************************************************************************/

void CallbackNotifier::setCallbacks(camera_notify_callback notify_cb,
                                    camera_data_callback data_cb,
                                    camera_data_timestamp_callback data_cb_timestamp,
                                    camera_request_memory get_memory,
                                    void* user)
{
    LOGV("%s: %p, %p, %p, %p (%p)",
         __FUNCTION__, notify_cb, data_cb, data_cb_timestamp, get_memory, user);

    Mutex::Autolock locker(&mObjectLock);
    mNotifyCB = notify_cb;
    mDataCB = data_cb;
    mDataCBTimestamp = data_cb_timestamp;
    mGetMemoryCB = get_memory;
    mCallbackCookie = user;

	mBufferList = new BufferListManager();
	if (mBufferList == NULL)
	{
		LOGE("create BufferListManager failed");
	}

	mSaveThreadExited = false;
	
	// init picture thread
	mSavePictureThread = new DoSavePictureThread(this);
	pthread_mutex_init(&mSavePictureMutex, NULL);
	pthread_cond_init(&mSavePictureCond, NULL);
	mSavePictureThread->startThread();
}

void CallbackNotifier::enableMessage(uint msg_type)
{
    LOGV("%s: msg_type = 0x%x", __FUNCTION__, msg_type);
    PrintMessages(msg_type);

    Mutex::Autolock locker(&mObjectLock);
    mMessageEnabler |= msg_type;
    LOGV("**** Currently enabled messages:");
    PrintMessages(mMessageEnabler);
}

void CallbackNotifier::disableMessage(uint msg_type)
{
    LOGV("%s: msg_type = 0x%x", __FUNCTION__, msg_type);
    PrintMessages(msg_type);

    Mutex::Autolock locker(&mObjectLock);
    mMessageEnabler &= ~msg_type;
    LOGV("**** Currently enabled messages:");
    PrintMessages(mMessageEnabler);
}

status_t CallbackNotifier::enableVideoRecording()
{
    F_LOG;

    Mutex::Autolock locker(&mObjectLock);
    mVideoRecEnabled = true;

    return NO_ERROR;
}

void CallbackNotifier::disableVideoRecording()
{
    F_LOG;

    Mutex::Autolock locker(&mObjectLock);
    mVideoRecEnabled = false;
}

status_t CallbackNotifier::storeMetaDataInBuffers(bool enable)
{
    /* Return INVALID_OPERATION means HAL does not support metadata. So HAL will
     * return actual frame data with CAMERA_MSG_VIDEO_FRAME. Return
     * INVALID_OPERATION to mean metadata is not supported. */

	return UNKNOWN_ERROR;
}

/****************************************************************************
 * Public API
 ***************************************************************************/

void CallbackNotifier::cleanupCBNotifier()
{
	if (mSavePictureThread != NULL)
	{
		mSavePictureThread->stopThread();
		pthread_cond_signal(&mSavePictureCond);
		
		pthread_mutex_lock(&mSavePictureMutex);
		if (!mSaveThreadExited)
		{
			struct timespec timeout;
			timeout.tv_sec=time(0) + 1;		// 1s timeout
			timeout.tv_nsec = 0;
			pthread_cond_timedwait(&mSavePictureCond, &mSavePictureMutex, &timeout);
			//pthread_cond_wait(&mSavePictureCond, &mSavePictureMutex);
		}
		pthread_mutex_unlock(&mSavePictureMutex);
		
		mSavePictureThread.clear();
		mSavePictureThread = 0;
		
		pthread_mutex_destroy(&mSavePictureMutex);
		pthread_cond_destroy(&mSavePictureCond);
	}
	
    Mutex::Autolock locker(&mObjectLock);
    mMessageEnabler = 0;
    mNotifyCB = NULL;
    mDataCB = NULL;
    mDataCBTimestamp = NULL;
    mGetMemoryCB = NULL;
    mCallbackCookie = NULL;
    mVideoRecEnabled = false;
    mJpegQuality = 90;
	mJpegRotate = 0;
	mPictureWidth = 640;
	mPictureHeight = 480;
	mThumbWidth = 0;
	mThumbHeight = 0;
	mGpsLatitude = 0.0;
	mGpsLongitude = 0.0;
	mGpsAltitude = 0;
	mGpsTimestamp = 0;
	mFocalLength = 0.0;
	mWhiteBalance = 0;

	if (mBufferList != NULL)
	{
		delete mBufferList;
		mBufferList = NULL;
	}
}

void CallbackNotifier::onNextFrameAvailable(const void* frame,
                                         	bool hw)
{
    if (hw)
    {
    	onNextFrameHW(frame);
    }
	else
	{
    	onNextFrameSW(frame);
	}
}

void CallbackNotifier::onNextFrameHW(const void* frame)
{
	V4L2BUF_t * pbuf = (V4L2BUF_t*)frame;
	
	if (isMessageEnabled(CAMERA_MSG_VIDEO_FRAME) && isVideoRecordingEnabled()) 
	{
        camera_memory_t* cam_buff = mGetMemoryCB(-1, sizeof(V4L2BUF_t), 1, NULL);
        if (NULL != cam_buff && NULL != cam_buff->data) 
		{
			pbuf->refCnt++;
            memcpy(cam_buff->data, frame, sizeof(V4L2BUF_t));
            mDataCBTimestamp(pbuf->timeStamp, CAMERA_MSG_VIDEO_FRAME,
                               cam_buff, 0, mCallbackCookie);
			cam_buff->release(cam_buff);
        } 
		else 
		{
            LOGE("%s: Memory failure in CAMERA_MSG_VIDEO_FRAME", __FUNCTION__);
        }
    }

    if (isMessageEnabled(CAMERA_MSG_PREVIEW_FRAME)) 
	{
        camera_memory_t* cam_buff = mGetMemoryCB(-1, sizeof(V4L2BUF_t), 1, NULL);
        if (NULL != cam_buff && NULL != cam_buff->data) 
		{
            memcpy(cam_buff->data, frame, sizeof(V4L2BUF_t));
			mDataCB(CAMERA_MSG_PREVIEW_FRAME, cam_buff, 0, NULL, mCallbackCookie);
			cam_buff->release(cam_buff);
        } 
		else 
		{
            LOGE("%s: Memory failure in CAMERA_MSG_PREVIEW_FRAME", __FUNCTION__);
        }
    }
}

void CallbackNotifier::onNextFrameSW(const void* frame)
{
	V4L2BUF_t * pbuf = (V4L2BUF_t*)frame;
	int framesize =0;
	int src_format = 0;
	int src_addr_phy = 0;
	int src_addr_vir = 0;
	int src_width = 0;
	int src_height = 0;
	RECT_t src_crop;

	if ((pbuf->isThumbAvailable == 1)
		&& (pbuf->thumbUsedForPreview == 1))
	{
		src_format			= pbuf->thumbFormat;
		src_addr_phy		= pbuf->thumbAddrPhyY;
		src_addr_vir		= pbuf->thumbAddrVirY;
		src_width			= pbuf->thumbWidth;
		src_height			= pbuf->thumbHeight;
		memcpy((void*)&src_crop, (void*)&pbuf->thumb_crop_rect, sizeof(RECT_t));
	}
	else
	{
		src_format			= pbuf->format;
		src_addr_phy		= pbuf->addrPhyY;
		src_addr_vir		= pbuf->addrVirY;
		src_width			= pbuf->width;
		src_height			= pbuf->height;
		memcpy((void*)&src_crop, (void*)&pbuf->crop_rect, sizeof(RECT_t));
	}
	
	framesize = ALIGN_16B(src_width) * src_height * 3/2;
	
	if (isMessageEnabled(CAMERA_MSG_VIDEO_FRAME) && isVideoRecordingEnabled()) 
	{
        camera_memory_t* cam_buff = mGetMemoryCB(-1, framesize, 1, NULL);
        if (NULL != cam_buff && NULL != cam_buff->data) 
		{
            memcpy(cam_buff->data, (void *)src_addr_vir, framesize);
            mDataCBTimestamp(pbuf->timeStamp, CAMERA_MSG_VIDEO_FRAME,
                               cam_buff, 0, mCallbackCookie);
			cam_buff->release(cam_buff);		// star add
        } else {
            LOGE("%s: Memory failure in CAMERA_MSG_VIDEO_FRAME", __FUNCTION__);
        }
    }

    if (isMessageEnabled(CAMERA_MSG_PREVIEW_FRAME)) 
	{
		if (strcmp(mCallingProcessName, "com.android.facelock") == 0)
		{
 			camera_memory_t* cam_buff = mGetMemoryCB(-1, 160 * 120 * 3 / 2, 1, NULL);
	        if (NULL != cam_buff && NULL != cam_buff->data) 
			{
				yuv420spDownScale((void*)src_addr_vir, cam_buff->data, 
								ALIGN_16B(src_width), src_height,
								160, 120);
	            mDataCB(CAMERA_MSG_PREVIEW_FRAME, cam_buff, 0, NULL, mCallbackCookie);
	            cam_buff->release(cam_buff);
	        }
			else 
			{
	            LOGE("%s: Memory failure in CAMERA_MSG_PREVIEW_FRAME", __FUNCTION__);
	        }
		}
		else
		{
			// LOGD("src size: %dx%d, cb size: %dx%d,", src_width, src_height, mCBWidth, mCBHeight);
			framesize = mCBWidth * mCBHeight * 3/2;
	        camera_memory_t* cam_buff = mGetMemoryCB(-1, framesize, 1, NULL);

			if (NULL == cam_buff 
				|| NULL == cam_buff->data) 
			{
				LOGE("%s: Memory failure in CAMERA_MSG_PREVIEW_FRAME", __FUNCTION__);
				return;
			}
			char * src = (char *)src_addr_vir;
			char * dst = (char *)cam_buff->data;
			 // y
			memcpy(dst, src, ALIGN_16B(mCBWidth) * mCBHeight);
			
			// uv
			src = (char *)src_addr_vir + ALIGN_16B(mCBWidth) * mCBHeight;
			dst = (char *)cam_buff->data + (ALIGN_16B(mCBWidth)*mCBHeight);
			if (src_format == V4L2_PIX_FMT_NV12)
			{
				memcpy(dst, src+1, (ALIGN_16B(mCBWidth) * mCBHeight >> 1)-1);
			}
			else if(src_format == V4L2_PIX_FMT_NV21)
			{
				memcpy(dst, src, (ALIGN_16B(mCBWidth) * mCBHeight >> 1));
			}

			/*
			yuv420spDownScale((void*)src_addr_vir, cam_buff->data, 
								ALIGN_16B(src_width), src_height,
								mCBWidth, mCBHeight);

	        if (src_format == V4L2_PIX_FMT_NV12)
			{
				// NV12 <--> NV21
				formatToNV21(cam_buff->data, 
							cam_buff->data, 
							mCBWidth, 
							mCBHeight,
							ALIGN_16B(mCBWidth),
							0,
							2,
							ALIGN_16B(mCBWidth) * mCBHeight * 3/2,
							src_format);
			}
			*/
            mDataCB(CAMERA_MSG_PREVIEW_FRAME, cam_buff, 0, NULL, mCallbackCookie);
            cam_buff->release(cam_buff);
		}
    }
}

status_t CallbackNotifier::autoFocusMsg(bool success)
{
	if (isMessageEnabled(CAMERA_MSG_FOCUS))
	{
        mNotifyCB(CAMERA_MSG_FOCUS, success, 0, mCallbackCookie);
    }
	return NO_ERROR;
}

status_t CallbackNotifier::autoFocusContinuousMsg(bool success)
{
	if (isMessageEnabled(CAMERA_MSG_FOCUS_MOVE))
	{
		// in continuous focus mode
		// true for starting focus, false for focus ok
		mNotifyCB(CAMERA_MSG_FOCUS_MOVE, success, 0, mCallbackCookie);
	}
    return NO_ERROR;
}

status_t CallbackNotifier::faceDetectionMsg(camera_frame_metadata_t *face)
{
	if (isMessageEnabled(CAMERA_MSG_PREVIEW_METADATA))
	{
		camera_memory_t *cam_buff = mGetMemoryCB(-1, 1, 1, NULL);
		mDataCB(CAMERA_MSG_PREVIEW_METADATA, cam_buff, 0, face, mCallbackCookie);
		cam_buff->release(cam_buff); 
	}
    return NO_ERROR;
}

void CallbackNotifier::notifyPictureMsg(const void* frame)
{
	F_LOG;

	V4L2BUF_t * pbuf = (V4L2BUF_t*)frame;
	int framesize = pbuf->width * pbuf->height * 3/2;

	// shutter msg
    if (isMessageEnabled(CAMERA_MSG_SHUTTER)) 
	{
		F_LOG;
        mNotifyCB(CAMERA_MSG_SHUTTER, 0, 0, mCallbackCookie);
    }

	// raw image msg
	if (isMessageEnabled(CAMERA_MSG_RAW_IMAGE)) 
	{
		camera_memory_t *dummyRaw = mGetMemoryCB(-1, 1, 1, NULL);
		if ( NULL == dummyRaw ) 
		{
			LOGE("%s: Memory failure in CAMERA_MSG_PREVIEW_FRAME", __FUNCTION__);
			return;
		}
		mDataCB(CAMERA_MSG_RAW_IMAGE, dummyRaw, 0, NULL, mCallbackCookie);
		dummyRaw->release(dummyRaw);
	}
	else if (isMessageEnabled(CAMERA_MSG_RAW_IMAGE_NOTIFY)) 
	{
		mNotifyCB(CAMERA_MSG_RAW_IMAGE_NOTIFY, 0, 0, mCallbackCookie);
	}
	
	// postview msg
	if (0 && isMessageEnabled(CAMERA_MSG_POSTVIEW_FRAME) )
	{
		F_LOG;
		camera_memory_t* cam_buff = mGetMemoryCB(-1, framesize, 1, NULL);
        if (NULL != cam_buff && NULL != cam_buff->data) 
		{
            memset(cam_buff->data, 0xff, framesize);
			mDataCB(CAMERA_MSG_POSTVIEW_FRAME, cam_buff, 0, NULL, mCallbackCookie);
            cam_buff->release(cam_buff);
        } 
		else 
		{
            LOGE("%s: Memory failure in CAMERA_MSG_PREVIEW_FRAME", __FUNCTION__);
			return;
        }
	}
}

void CallbackNotifier::startContinuousPicture()
{
	F_LOG;
	
	// 
	mSavePictureCnt = 0;
}

void CallbackNotifier::stopContinuousPicture()
{
	// do nothing
}

void CallbackNotifier::setContinuousPictureCnt(int cnt)
{
	mSavePictureMax = cnt;
}

bool CallbackNotifier::takePicture(const void* frame, bool is_continuous)
{
	buffer_node * pNode = NULL;
	V4L2BUF_t * pbuf = (V4L2BUF_t *)frame;
	void * pOutBuf = NULL;
	int bufSize = 0;

	int src_format = 0;
	int src_addr_phy = 0;
	int src_addr_vir = 0;
	int src_width = 0;
	int src_height = 0;
	RECT_t src_crop;

	DBG_TIME_BEGIN("CallbackNotifier taking picture", 0);

	if ((pbuf->isThumbAvailable == 1)
		&& (pbuf->thumbUsedForPhoto == 1))
	{
		src_format			= pbuf->thumbFormat;
		src_addr_phy		= pbuf->thumbAddrPhyY;
		src_addr_vir		= pbuf->thumbAddrVirY;
		src_width			= pbuf->thumbWidth;
		src_height			= pbuf->thumbHeight;
		memcpy((void*)&src_crop, (void*)&pbuf->thumb_crop_rect, sizeof(RECT_t));
	}
	else
	{
		src_format			= pbuf->format;
		src_addr_phy		= pbuf->addrPhyY;
		src_addr_vir		= pbuf->addrVirY;
		src_width			= pbuf->width;
		src_height			= pbuf->height;
		memcpy((void*)&src_crop, (void*)&pbuf->crop_rect, sizeof(RECT_t));
	}

	JPEG_ENC_t jpeg_enc;
	memset(&jpeg_enc, 0, sizeof(jpeg_enc));
	jpeg_enc.addrY			= src_addr_phy;
	jpeg_enc.addrC			= src_addr_phy + ALIGN_16B(src_width) * src_height;
	jpeg_enc.src_w			= src_width;
	jpeg_enc.src_h			= src_height;
	jpeg_enc.pic_w			= mPictureWidth;
	jpeg_enc.pic_h			= mPictureHeight;
	jpeg_enc.colorFormat	= (src_format == V4L2_PIX_FMT_NV21) ? JPEG_COLOR_YUV420_NV21 : JPEG_COLOR_YUV420_NV12;
	jpeg_enc.quality		= mJpegQuality;
	jpeg_enc.rotate			= mJpegRotate;

	getCurrentDateTime();

	// 
	strcpy(jpeg_enc.CameraMake, mExifMake);
	strcpy(jpeg_enc.CameraModel, mExifModel);
	strcpy(jpeg_enc.DateTime, mDateTime);
	
	jpeg_enc.thumbWidth		= mThumbWidth;
	jpeg_enc.thumbHeight	= mThumbHeight;
	jpeg_enc.whitebalance   = mWhiteBalance;
	jpeg_enc.focal_length	= mFocalLength;

	if (0 != strlen(mGpsMethod))
	{
		jpeg_enc.enable_gps			= 1;
		jpeg_enc.gps_latitude		= mGpsLatitude;
		jpeg_enc.gps_longitude		= mGpsLongitude;
		jpeg_enc.gps_altitude		= mGpsAltitude;
		jpeg_enc.gps_timestamp		= mGpsTimestamp;
		strcpy(jpeg_enc.gps_processing_method, mGpsMethod);
		memset(mGpsMethod, 0, sizeof(mGpsMethod));
	}
	else
	{
		jpeg_enc.enable_gps			= 0;
	}

	if ((src_crop.width != jpeg_enc.src_w)
		|| (src_crop.height != jpeg_enc.src_h))
	{
		jpeg_enc.enable_crop		= 1;
		jpeg_enc.crop_x				= src_crop.left;
		jpeg_enc.crop_y				= src_crop.top;
		jpeg_enc.crop_w				= src_crop.width;
		jpeg_enc.crop_h				= src_crop.height;
	}
	else
	{
		jpeg_enc.enable_crop		= 0;
	}
	
	LOGV("addrY: %x, src: %dx%d, pic: %dx%d, quality: %d, rotate: %d, Gps method: %s, \
		thumbW: %d, thumbH: %d, thubmFactor: %d, crop: [%d, %d, %d, %d]", 
		jpeg_enc.addrY, 
		jpeg_enc.src_w, jpeg_enc.src_h,
		jpeg_enc.pic_w, jpeg_enc.pic_h,
		jpeg_enc.quality, jpeg_enc.rotate,
		jpeg_enc.gps_processing_method,
		jpeg_enc.thumbWidth,
		jpeg_enc.thumbHeight,
		jpeg_enc.scale_factor,
		jpeg_enc.crop_x,
		jpeg_enc.crop_y,
		jpeg_enc.crop_w,
		jpeg_enc.crop_h);
	
	pNode = mBufferList->allocBuffer(-1, mPictureWidth * mPictureHeight);
	if (pNode == NULL)
	{
		LOGE("malloc picture node failed");
		return false;
	}
	pOutBuf = pNode->data;
	if (pOutBuf == NULL)
	{
		LOGE("malloc picture memory failed");
		return false;
	}

	//int64_t lasttime = systemTime();
	int ret = JpegEnc(pOutBuf, &bufSize, &jpeg_enc);
	if (ret < 0)
	{
		LOGE("JpegEnc failed");
		return false;
	}
	//LOGV("hw enc time: %lld(ms), size: %d", (systemTime() - lasttime)/1000000, bufSize);

	DBG_TIME_DIFF("enc");

	if (is_continuous)
	{
		pNode->id = mSavePictureCnt;
		pNode->size = bufSize;
		mBufferList->push(pNode);

		// cb number of pictures
		if (isMessageEnabled(CAMERA_MSG_CONTINUOUSSNAP)) 
		{
			mNotifyCB(CAMERA_MSG_CONTINUOUSSNAP, mSavePictureCnt, 0, mCallbackCookie);
	    }
		
		pthread_cond_signal(&mSavePictureCond);

		mSavePictureCnt++;
	}
	else
	{
		if (strlen(mSnapPath) > 0)
		{
			camera_memory_t* cb_buff;
			
			strcpy(pNode->priv, mSnapPath);
			pNode->id = -1;
			pNode->size = bufSize;
			mBufferList->push(pNode);

			mNotifyCB(CAMERA_MSG_SNAP, 0, 0, mCallbackCookie);
			pthread_cond_signal(&mSavePictureCond);
		}
		else
		{
			camera_memory_t* jpeg_buff = mGetMemoryCB(-1, bufSize, 1, NULL);
			if (NULL != jpeg_buff && NULL != jpeg_buff->data) 
			{
				memcpy(jpeg_buff->data, (uint8_t *)pOutBuf, bufSize); 
				mDataCB(CAMERA_MSG_COMPRESSED_IMAGE, jpeg_buff, 0, NULL, mCallbackCookie);
				jpeg_buff->release(jpeg_buff);
			} 
			else 
			{
				LOGE("%s: Memory failure in CAMERA_MSG_COMPRESSED_IMAGE", __FUNCTION__);
			}

			mBufferList->releaseBuffer(pNode);
		}
	}
	
	DBG_TIME_DIFF("photo end");
	LOGV("taking photo end");
	return true;
}

bool CallbackNotifier::savePictureThread()
{
	if (mBufferList->isListEmpty())
	{
		pthread_mutex_lock(&mSavePictureMutex);
		
		if (mSavePictureThread->getThreadStatus() == THREAD_STATE_EXIT)
		{
			mSaveThreadExited = true;
			pthread_mutex_unlock(&mSavePictureMutex);
			
			pthread_cond_signal(&mSavePictureCond);
			
			LOGD("savePictureThread exit");
			return false;
		}
		
		LOGV("wait for picture to save");
		pthread_cond_wait(&mSavePictureCond, &mSavePictureMutex);
		pthread_mutex_unlock(&mSavePictureMutex);
		return true;
	}
	
	DBG_TIME_BEGIN("save picture", 0);

	char fname[128];
	FILE *pf = NULL;
	buffer_node * pNode = mBufferList->pop();
	if (pNode == NULL)
	{
		LOGE("list head is null");
		goto SAVE_PICTURE_END;
	}
	
	if (pNode->id >= 0)
	{
		//LOGV("mFolderPath %s , id: %d", mFolderPath, pNode->id);
		sprintf(fname, "%s%03d.jpg", mFolderPath, pNode->id);
	}
	else
	{
		//LOGV("pNode->priv %s ", pNode->priv);
		strcpy(fname, pNode->priv);
	}
	
	pf = fopen(fname, "wb+");
	if (pf != NULL)
	{
		LOGV("open %s ok", fname);
		fwrite((uint8_t *)pNode->data, pNode->size, 1, pf);
		fflush(pf);
		fclose(pf);
	}
	else
	{
		LOGE("open %s failed, %s", fname, strerror(errno));
		goto SAVE_PICTURE_END;
	}
	
	DBG_TIME_DIFF("write file");

	if (pNode->id < 0)
	{
		camera_memory_t* cb_buff;
		cb_buff = mGetMemoryCB(-1, strlen(pNode->priv), 1, NULL);
		if (NULL != cb_buff && NULL != cb_buff->data) 
		{
			memcpy(cb_buff->data, (uint8_t *)pNode->priv, strlen(pNode->priv));
			mDataCB(CAMERA_MSG_SNAP_THUMB, cb_buff, 0, NULL, mCallbackCookie);
			cb_buff->release(cb_buff);
		} 
		else 
		{
			LOGE("%s: Memory failure in CAMERA_MSG_SNAP_THUMB", __FUNCTION__);
		}
	}

SAVE_PICTURE_END:
	if (pNode != NULL)
	{
		mBufferList->releaseBuffer(pNode);
	}
	
	return true;
}


void CallbackNotifier::getCurrentDateTime()
{
	time_t t;
	struct tm *tm_t;
	time(&t);
	tm_t = localtime(&t);
	sprintf(mDateTime, "%4d:%02d:%02d %02d:%02d:%02d", 
		tm_t->tm_year+1900, tm_t->tm_mon+1, tm_t->tm_mday,
		tm_t->tm_hour, tm_t->tm_min, tm_t->tm_sec);
}

void CallbackNotifier::onCameraDeviceError(int err)
{
    if (isMessageEnabled(CAMERA_MSG_ERROR) && mNotifyCB != NULL) {
        mNotifyCB(CAMERA_MSG_ERROR, err, 0, mCallbackCookie);
    }
}

}; /* namespace android */
