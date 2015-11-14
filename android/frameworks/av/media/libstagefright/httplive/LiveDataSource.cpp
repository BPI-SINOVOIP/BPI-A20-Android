#define LOG_TAG "LiveDataSource"
#include <utils/Log.h>
#include <include/LiveDataSource.h>

#include <media/stagefright/foundation/ADebug.h>


namespace android
{
	LiveDataSource::LiveDataSource()
    {
		mFinalResult			= OK;
		mFirstSeqNumInBuffer	= -1;
		mLastSeqNumInBuffer		= -1;
		
		mStartBufferPos			= 0;
		mReadPos				= 0;
		mWritePos				= 0;
		mValidNodeCount			= 0;
		mPassedNodeCount		= 0;
		mValidDataSize			= 0;
		
		mBandwidth              = 0;
		mBWWritePos             = 0;
		mBWCount                = 0;
		
		memset(mNodes, 0, sizeof(DataNode)*MAX_NODE_COUNT);
		
		//* allocate memory for the big buffer.
		int i = 0;
		do
		{
			mMaxNodeCount = MAX_NODE_COUNT>>i;
			mBigBuffer = malloc(mMaxNodeCount*NODE_BUFFER_SIZE);

			if(mBigBuffer != NULL || (mMaxNodeCount<=128))                   //原为128
				break;

		}while(mBigBuffer == NULL);

		if(mBigBuffer == NULL)
		{
			ALOGD("!!!!!!!!!!!!!!!!!!!!: LiveDataSource, allocate for the big buffer fail.");
			mFinalResult = NO_MEMORY;
		}

		ALOGD("!!!!!!!!!!!!!!!!!!!!: Http Live Stream can cache %d bytes at maximum.", mMaxNodeCount*NODE_BUFFER_SIZE);
	}

	LiveDataSource::~LiveDataSource()
	{
		if(mBigBuffer != NULL)
		{
			free(mBigBuffer);
			mBigBuffer = NULL;
		}
	}

	status_t LiveDataSource::initCheck() const  
	{
		if(mBigBuffer == NULL)
			return NO_MEMORY;
		return OK;
	}
	
	//可能释放0个节点或者多个节点，但最终返回的只是liveDataSource->mNodes[liveDataSource->mWritePos]（即当前写位置的节点）或者NULL. 
	//mWritePos的变化不在此函数内发生,当数据写入节点buf，并将节点node纳入可供播放的队列后，mWritePos++（见函数queueBuffer()）
	DataNode* LiveDataSource::getBuffer(void)
	{
		DataNode *node, *node1, *node2;
		Mutex::Autolock autoLock(mLock);
        node = node1 = &mNodes[mWritePos];
        node2 = &mNodes[mReadPos];

		ALOGV("xxx before getBuffer: mValidNodeCount = %d, mPassedNodeCount = %d, mValidDataSize = %d, mWritePos = %d",
				mValidNodeCount, mPassedNodeCount, mValidDataSize, mWritePos);

		if (mNodes[mWritePos].mBufferFlag != 0)//此时mWritePos等于mStartBufferPos（可读的起始位置）
		{
			if(node1->seqNum < node2->seqNum)
			{
				//* flush one passed nodes.
				int discardSeqNum = node1->seqNum;
				while(discardSeqNum == node1->seqNum)
				{
					node1->mBufferFlag=0;//  release node buffer.
					mPassedNodeCount--;
					mStartBufferPos++;
					if(mStartBufferPos ==  mMaxNodeCount)
						mStartBufferPos = 0;

					node1 = &mNodes[mStartBufferPos];
				}

				mFirstSeqNumInBuffer = node1->seqNum;

				ALOGV("xxxxxxxxx after release operation, mStartBufferPos = %d, mReadPos = %d, node1->seqNum = %d, node2->seqNum = %d.",
						mStartBufferPos, mReadPos, node1->seqNum, node2->seqNum);
			}
			else
			{
				//* 如出错，请先检查ts分片的大小

				ALOGW("xxxxxxxxx Please check the size of ts slice");

				if(mPassedNodeCount >= 64)//32
				{
					int i = 0;
					for( ; i<32; i++)//16
					{
						node1->mBufferFlag=0;//  release node buffer.
						mPassedNodeCount--;
						mStartBufferPos++;
						if(mStartBufferPos ==  mMaxNodeCount)
							mStartBufferPos = 0;

						node1 = &mNodes[mStartBufferPos];	
					}
					mFirstSeqNumInBuffer = node1->seqNum;
					ALOGV("xxxxxxxxx after release operation, mStartBufferPos = %d, mReadPos = %d, node1->seqNum = %d, node2->seqNum = %d.",
							mStartBufferPos, mReadPos, node1->seqNum, node2->seqNum);
				}
				else
				{
					return NULL;
				}
				

			}  
		}

		node->hasBeenRead	= 0;
		node->seqNum		= -1;
		node->subSeqNum 	= -1;
		node->bufSize		= NODE_BUFFER_SIZE;
		node->dataSize		= 0;
		node->offset		= 0;
		node->data			= (char*)mBigBuffer + mWritePos*NODE_BUFFER_SIZE;
		//memset(node->data, 0x00, NODE_BUFFER_SIZE);//可能不需要，因为有node->dataSize来控制解密、上层读取等。为防万一而添加
 		return node;

	}

	void LiveDataSource::queueBuffer(DataNode* node)//将节点node纳入可供播放的队列
	{
		Mutex::Autolock autoLock(mLock);	 

		node->mBufferFlag	= 1;     //已经写入数据
		
		mLastSeqNumInBuffer	= node->seqNum;
		if(mFirstSeqNumInBuffer == -1)
			mFirstSeqNumInBuffer = node->seqNum;

		mValidNodeCount++;
		mValidDataSize += node->dataSize;

		mWritePos++;
		if(mWritePos >= mMaxNodeCount)
			mWritePos = 0;
		mCondition.broadcast();
		
	}

	bool LiveDataSource::needBufferNewData(void) //是否需要下载新数据,1--需要，0--不需要
	{
		Mutex::Autolock autoLock(mLock);	 
		if(mPassedNodeCount>32 || (mValidNodeCount+32)<mMaxNodeCount)
			return 1;
		else
			return 0;
	}

	int LiveDataSource::getFirstSeqNumInBuffer(void)
	{
		Mutex::Autolock autoLock(mLock);	 
		return mFirstSeqNumInBuffer;
	}

	int LiveDataSource::getLastSeqNumInBuffer(void)
	{
		Mutex::Autolock autoLock(mLock);	 
		return mLastSeqNumInBuffer;
	}

	void LiveDataSource::reset(void)//重置的是节点的状态
	{
		Mutex::Autolock autoLock(mLock);	

		memset(mNodes, 0, sizeof(DataNode)*mMaxNodeCount);

		mStartBufferPos			= 0;
		mReadPos				= 0;
		mWritePos				= 0;
		mValidNodeCount			= 0;
		mPassedNodeCount		= 0;
		mValidDataSize			= 0;

		mFirstSeqNumInBuffer	= -1;
		mLastSeqNumInBuffer		= -1;

		mFinalResult			= OK;
		
		return;
	}

	void LiveDataSource::queueEOS(status_t finalResult)
	{
		CHECK_NE(finalResult, (status_t)OK);
		Mutex::Autolock autoLock(mLock);	 
		mFinalResult = finalResult;
		mCondition.broadcast();
		return;
	}

	ssize_t LiveDataSource::readAtNonBlocking(off64_t offset, void *data, size_t size) //如果不够size就不读，直接返回
	{
		Mutex::Autolock autoLock(mLock);
		if (mValidDataSize < size) {
			return mFinalResult == OK ? -EWOULDBLOCK : mFinalResult;
		}
		return readAtNonBlocking2(offset, data, size);
	}
	
	ssize_t LiveDataSource::readAtBlocking(off64_t offset, void *data, size_t size) 
	{
		Mutex::Autolock autoLock(mLock);
		while (mValidDataSize < size && mFinalResult == OK) 
		{
			mCondition.wait(mLock);
		}

		if (mValidDataSize < size) 
		{//意味着mFinalResult != OK,即mValidDataSize < size&&mFinalResult != OK
			return mFinalResult;
		}
		//mValidDataSize >= size
		return readAtNonBlocking2(offset, data, size);
	}


	ssize_t LiveDataSource::readAt(off64_t offset, void *data, size_t size)//也是NonBlocking的
	{
		Mutex::Autolock autoLock(mLock);
		return readAtNonBlocking2(offset, data, size);
	}
	
	//从liveDataSource中读size 数据到 data中供播放
	//若不足size,则有多少读多少
	//！！！这个函数没有加锁，不做外层调用
	ssize_t LiveDataSource::readAtNonBlocking2(off64_t offset, void* data, size_t size) 
	{
		int			sizeToRead		= size; //要读的数据大小
		int			readBytes		= 0;    //已读的数据大小
		int			nodeDataSize	= 0;   //节点中未读的数据
		DataNode*	node			= NULL;
		char*		ptr				= (char*) data;

		ALOGV("xxx before readAtNonBlocking: mValidNodeCount = %d, mPassedNodeCount = %d, mValidDataSize = %d, mReadPos = %d",
				mValidNodeCount, mPassedNodeCount, mValidDataSize, mReadPos);
		while(sizeToRead > 0 && mValidNodeCount > 0)
		{
			node		 = &mNodes[mReadPos];
			nodeDataSize = node->dataSize - node->offset;//dataSize是节点中写入数据的数量，offset是节点中读到的位置，也就是已读的数量

			if(sizeToRead >= nodeDataSize)
			{
				memcpy(ptr, (char*)node->data + node->offset, nodeDataSize);

				sizeToRead	-= nodeDataSize;
				readBytes	+= nodeDataSize;
				ptr			+= nodeDataSize;

                node->offset	  = 0;  //该节点被读完，offset置零。offset置零时，注意读的位置mReadPos，和节点hasBeenRead的变化                
				node->hasBeenRead = 1;

 				mPassedNodeCount++;
				mValidNodeCount--;
				mValidDataSize -= nodeDataSize;
				mReadPos++;
				if(mReadPos >= mMaxNodeCount)
					mReadPos = 0;
			}
			else
			{
				memcpy(ptr, (char*)node->data + node->offset, sizeToRead);

				readBytes	 	+= sizeToRead;
				ptr			 	+= sizeToRead;
				node->offset 	+= sizeToRead;
				mValidDataSize 	-= sizeToRead;
				sizeToRead	  	 = 0;
			}
		}

		ALOGV("xxx after readAtNonBlocking: mValidNodeCount = %d, mPassedNodeCount = %d, mValidDataSize = %d, mReadPos = %d",
				mValidNodeCount, mPassedNodeCount, mValidDataSize, mReadPos);

		if(readBytes == 0)
			return mFinalResult == OK ? -EWOULDBLOCK : mFinalResult;
		else
			return readBytes;
	}
	//return -1意味着seqNum不在当前bigbuf的片段范围之内。seekDir < 0等价于mNodes[liveDataSource->mReadPos].seqNum >= seqNum。
	//对于seqNum在当前bigbuf的片段范围之内，且seekDir < 0,seek的结果找到了seqNum，但subSeqNum可能不等于1，因为subSeqNum == 1的节点可能不在bigbuf的范围内
	//对于seqNum在当前bigbuf的片段范围之内，且seekDir >= 0（mNodes[liveDataSource->mReadPos].seqNum < seqNum）,seek的结果找到了seqNum，且 subSeqNum == 1
 	int LiveDataSource::seekToSeqNum(int seqNum) //按片段号seek,改变liveDataSource中读的位置 mReadPos
	{
		int			seekDir		= 0;		//* seek direction, -1 means backward, 1 means forward, 0 means no seek.

		Mutex::Autolock autoLock(mLock);	 

		if(mFirstSeqNumInBuffer == -1 || mLastSeqNumInBuffer == -1)
		{
			ALOGV("!!!!!!!!!!!!!!!!!!!!: seekToSeqNum, mFirstSeqNumInBuffer = %d, mLastSeqNumInBuffer = %d, seek fail.", mFirstSeqNumInBuffer, mLastSeqNumInBuffer);
			return -1;
		}
		
		if(seqNum < mFirstSeqNumInBuffer || seqNum > mLastSeqNumInBuffer)
		{
			ALOGV("!!!!!!!!!!!!!!!!!!!!: seekToSeqNum, mFirstSeqNumInBuffer = %d, mLastSeqNumInBuffer = %d, target seqNum = %d, seek fail.",
					mFirstSeqNumInBuffer, mLastSeqNumInBuffer, seqNum);
			return -1;
		}

		if(mNodes[mReadPos].seqNum >= seqNum)
		{
			//* seek backward.
			seekDir = -1;
		}
		else
		{
			//* seek forward.
			seekDir = 1;
		}

		if(seekDir < 0)
		{
			//* seek backward.

			ALOGV("++++++++++++++++++++, seekToSeqNum, seek backward.");
			
			mValidDataSize += mNodes[mReadPos].offset;
			mNodes[mReadPos].offset		 = 0;
			mNodes[mReadPos].hasBeenRead = 0;
			
			while(1)
			{
				if(mPassedNodeCount == 0)	//没有已经读过的节点数，所以不用mReadPos--；
				{
					ALOGV("!!!!!!!!!!!!!!!!!!!!, seekToSeqNum, mPassedNodeCount = 0, should not happen.");
					break;
				}
				
                //subSeqNum 取值从1开始//mReadPos == mStartBufferPos意思是到了最早可读的位置，可读的起始位置，此时seek到了seqNum（seqNum在bigbuf的范围内，否则前面已返回-1），但可能没有seek到subSeqNum == 1，因为subSeqNum == 1的节点可能不在bigbuf的范围内        
				//！！！注意subSeqNum，跟上层调用有关，是上层写入的，取0还是取1要与livesession一致
				if((mNodes[mReadPos].seqNum == seqNum && mNodes[mReadPos].subSeqNum == 0) || (mReadPos == mStartBufferPos))
				{
					ALOGV("++++++++++++++++++++, seekToSeqNum, break the seek process.");
					break;
				}
				else
				{
					mValidNodeCount++;
					mPassedNodeCount--;
					mReadPos--;
					if(mReadPos < 0)
						mReadPos = mMaxNodeCount - 1;

					mValidDataSize += mNodes[mReadPos].dataSize;
					mNodes[mReadPos].offset		 = 0;
					mNodes[mReadPos].hasBeenRead = 0;

				}
			}
			
 		}
		else
		{
			//* seek forward. //此时liveDataSource->mNodes[mReadPos].seqNum < seqNum
			ALOGV("++++++++++++++++++++, seekToSeqNum, seek forward.");
			while(1)
			{
				if(mValidNodeCount == 0)	 
				{
					ALOGV("!!!!!!!!!!!!!!!!!!!!, seekToSeqNum, mValidNodeCount = 0, should not happen.");
					break;
				}

				ALOGV("current seqNum = %d, subSeqNum = %d, target seqNum = %d, mReadPos = %d, mStartBufferPos = %d, mWritePos = %d",
						mNodes[mReadPos].seqNum, mNodes[mReadPos].subSeqNum, seqNum, mReadPos, mStartBufferPos, mWritePos);
				
				if(mNodes[mReadPos].seqNum == seqNum)//subSeqNum == 0应总是满足，因为我们从liveDataSource->mNodes[mReadPos].seqNum < seqNum开始循环。因为seekDir < 0等价于mNodes[liveDataSource->mReadPos].seqNum >= seqNum
				{
					mNodes[mReadPos].offset		 = 0;
					mNodes[mReadPos].hasBeenRead = 0;
					break;
				}
				else
				{
                    mValidDataSize 				-= (mNodes[mReadPos].dataSize - mNodes[mReadPos].offset);
					mNodes[mReadPos].offset		 = 0;
					mNodes[mReadPos].hasBeenRead = 1;
					mValidNodeCount--;
					mPassedNodeCount++;
					mReadPos++;
					if(mReadPos >= mMaxNodeCount)
						mReadPos = 0;

				}
			}
			
 		}
		return 0;
	}

	int	LiveDataSource::getValidDataSize(void)
	{
//		ALOGV("++++++++++++++++++++: getValidDataSize, mValidDataSize = %d.", mValidDataSize);
		return mValidDataSize;
	}

	bool LiveDataSource::eof(void)
	{
		if(mFinalResult == OK)
			return false;
		else
			return true;
	}

	int LiveDataSource::getBandwidth(void)
	{
		return mBandwidth;
	}

	
	void LiveDataSource::updataBWEstimator(int downloadSize, int64_t timeCost)
	{
		int64_t totalTimeCost;
		int64_t totalSize;
		int     i;

		mBWArray[mBWWritePos].downloadSize     = downloadSize;
		mBWArray[mBWWritePos].downloadTimeCost = timeCost;
		mBWWritePos++;
		if(mBWWritePos >= BANDWIDTH_ARRAY_SIZE)
			mBWWritePos = 0;

		if(mBWCount < BANDWIDTH_ARRAY_SIZE)
			mBWCount++;

		totalTimeCost = 0;
		totalSize = 0;
		for(i=0; i<mBWCount; i++)
		{
			totalTimeCost += mBWArray[i].downloadTimeCost;
			totalSize     += mBWArray[i].downloadSize;
		}

		if(totalTimeCost > 0)
		{
			mBandwidth = totalSize*8*1000/totalTimeCost;
		}
	}

	status_t LiveDataSource::getNetWorkStatus(int32_t *networkstatus)	
	{		
		*networkstatus = (int32_t)mFinalResult; 	
		return OK;	
	}


}  // namespace android




