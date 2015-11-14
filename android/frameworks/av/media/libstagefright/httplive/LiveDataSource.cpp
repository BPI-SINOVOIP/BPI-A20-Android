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

			if(mBigBuffer != NULL || (mMaxNodeCount<=128))                   //ԭΪ128
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
	
	//�����ͷ�0���ڵ���߶���ڵ㣬�����շ��ص�ֻ��liveDataSource->mNodes[liveDataSource->mWritePos]������ǰдλ�õĽڵ㣩����NULL. 
	//mWritePos�ı仯���ڴ˺����ڷ���,������д��ڵ�buf�������ڵ�node����ɹ����ŵĶ��к�mWritePos++��������queueBuffer()��
	DataNode* LiveDataSource::getBuffer(void)
	{
		DataNode *node, *node1, *node2;
		Mutex::Autolock autoLock(mLock);
        node = node1 = &mNodes[mWritePos];
        node2 = &mNodes[mReadPos];

		ALOGV("xxx before getBuffer: mValidNodeCount = %d, mPassedNodeCount = %d, mValidDataSize = %d, mWritePos = %d",
				mValidNodeCount, mPassedNodeCount, mValidDataSize, mWritePos);

		if (mNodes[mWritePos].mBufferFlag != 0)//��ʱmWritePos����mStartBufferPos���ɶ�����ʼλ�ã�
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
				//* ��������ȼ��ts��Ƭ�Ĵ�С

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
		//memset(node->data, 0x00, NODE_BUFFER_SIZE);//���ܲ���Ҫ����Ϊ��node->dataSize�����ƽ��ܡ��ϲ��ȡ�ȡ�Ϊ����һ�����
 		return node;

	}

	void LiveDataSource::queueBuffer(DataNode* node)//���ڵ�node����ɹ����ŵĶ���
	{
		Mutex::Autolock autoLock(mLock);	 

		node->mBufferFlag	= 1;     //�Ѿ�д������
		
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

	bool LiveDataSource::needBufferNewData(void) //�Ƿ���Ҫ����������,1--��Ҫ��0--����Ҫ
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

	void LiveDataSource::reset(void)//���õ��ǽڵ��״̬
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

	ssize_t LiveDataSource::readAtNonBlocking(off64_t offset, void *data, size_t size) //�������size�Ͳ�����ֱ�ӷ���
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
		{//��ζ��mFinalResult != OK,��mValidDataSize < size&&mFinalResult != OK
			return mFinalResult;
		}
		//mValidDataSize >= size
		return readAtNonBlocking2(offset, data, size);
	}


	ssize_t LiveDataSource::readAt(off64_t offset, void *data, size_t size)//Ҳ��NonBlocking��
	{
		Mutex::Autolock autoLock(mLock);
		return readAtNonBlocking2(offset, data, size);
	}
	
	//��liveDataSource�ж�size ���ݵ� data�й�����
	//������size,���ж��ٶ�����
	//�������������û�м���������������
	ssize_t LiveDataSource::readAtNonBlocking2(off64_t offset, void* data, size_t size) 
	{
		int			sizeToRead		= size; //Ҫ�������ݴ�С
		int			readBytes		= 0;    //�Ѷ������ݴ�С
		int			nodeDataSize	= 0;   //�ڵ���δ��������
		DataNode*	node			= NULL;
		char*		ptr				= (char*) data;

		ALOGV("xxx before readAtNonBlocking: mValidNodeCount = %d, mPassedNodeCount = %d, mValidDataSize = %d, mReadPos = %d",
				mValidNodeCount, mPassedNodeCount, mValidDataSize, mReadPos);
		while(sizeToRead > 0 && mValidNodeCount > 0)
		{
			node		 = &mNodes[mReadPos];
			nodeDataSize = node->dataSize - node->offset;//dataSize�ǽڵ���д�����ݵ�������offset�ǽڵ��ж�����λ�ã�Ҳ�����Ѷ�������

			if(sizeToRead >= nodeDataSize)
			{
				memcpy(ptr, (char*)node->data + node->offset, nodeDataSize);

				sizeToRead	-= nodeDataSize;
				readBytes	+= nodeDataSize;
				ptr			+= nodeDataSize;

                node->offset	  = 0;  //�ýڵ㱻���꣬offset���㡣offset����ʱ��ע�����λ��mReadPos���ͽڵ�hasBeenRead�ı仯                
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
	//return -1��ζ��seqNum���ڵ�ǰbigbuf��Ƭ�η�Χ֮�ڡ�seekDir < 0�ȼ���mNodes[liveDataSource->mReadPos].seqNum >= seqNum��
	//����seqNum�ڵ�ǰbigbuf��Ƭ�η�Χ֮�ڣ���seekDir < 0,seek�Ľ���ҵ���seqNum����subSeqNum���ܲ�����1����ΪsubSeqNum == 1�Ľڵ���ܲ���bigbuf�ķ�Χ��
	//����seqNum�ڵ�ǰbigbuf��Ƭ�η�Χ֮�ڣ���seekDir >= 0��mNodes[liveDataSource->mReadPos].seqNum < seqNum��,seek�Ľ���ҵ���seqNum���� subSeqNum == 1
 	int LiveDataSource::seekToSeqNum(int seqNum) //��Ƭ�κ�seek,�ı�liveDataSource�ж���λ�� mReadPos
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
				if(mPassedNodeCount == 0)	//û���Ѿ������Ľڵ��������Բ���mReadPos--��
				{
					ALOGV("!!!!!!!!!!!!!!!!!!!!, seekToSeqNum, mPassedNodeCount = 0, should not happen.");
					break;
				}
				
                //subSeqNum ȡֵ��1��ʼ//mReadPos == mStartBufferPos��˼�ǵ�������ɶ���λ�ã��ɶ�����ʼλ�ã���ʱseek����seqNum��seqNum��bigbuf�ķ�Χ�ڣ�����ǰ���ѷ���-1����������û��seek��subSeqNum == 1����ΪsubSeqNum == 1�Ľڵ���ܲ���bigbuf�ķ�Χ��        
				//������ע��subSeqNum�����ϲ�����йأ����ϲ�д��ģ�ȡ0����ȡ1Ҫ��livesessionһ��
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
			//* seek forward. //��ʱliveDataSource->mNodes[mReadPos].seqNum < seqNum
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
				
				if(mNodes[mReadPos].seqNum == seqNum)//subSeqNum == 0Ӧ�������㣬��Ϊ���Ǵ�liveDataSource->mNodes[mReadPos].seqNum < seqNum��ʼѭ������ΪseekDir < 0�ȼ���mNodes[liveDataSource->mReadPos].seqNum >= seqNum
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




