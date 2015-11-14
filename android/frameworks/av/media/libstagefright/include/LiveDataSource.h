#ifndef LIVE_DATA_SOURCE_H
#define LIVE_DATA_SOURCE_H

#include <media/stagefright/foundation/ABase.h>
#include <media/stagefright/DataSource.h>
#include <utils/threads.h>
#include <utils/List.h>

namespace android
{

	#define BANDWIDTH_ARRAY_SIZE 100
	typedef struct BR_ESTIMATER_NODE
	{
		int64_t downloadTimeCost;
		int     downloadSize;
	}bw_est_node_t;
	
	typedef struct DATANODE
	{
	    char	mBufferFlag;    //���bigbuffer��ÿ���ڵ�Ŀ�д״̬��0��ʾ�ǿյģ�����д
		int		hasBeenRead;	//* whether data of this node has been read.//��ʵ����û�У���Ϊ������mStartBufferPos��mReadPos�Ѿ��ǳ�ֵ��ˣ�����������ֻ�ж����ĸ�ֵ����û�ж�����ʹ��
		int		seqNum;			//* sequence number of the ts slice which the data of this node belong to.
		int		subSeqNum;		//* for example, subSeqNum = 3 means this is the third data node of the ts data slice.   //??
		int		bufSize;		//* how big is ther buffer used by this node.
		int		dataSize;		//* how much valid data in this node. //dataSize�ǽڵ���д�����ݵ���������ΪbufSize����ûд������offset�ǽڵ��ж�����λ�ã�Ҳ�����Ѷ�������. dataSize - offset�ǽڵ���δ����������
		int		offset;			//* how much data of this node has been read.
		void*	data;			//* start address of the data(buffer).
	}DataNode;

	struct LiveDataSource : public DataSource
	{
		LiveDataSource();

		virtual status_t	initCheck() const;												
		virtual ssize_t		readAt(off64_t offset, void* data, size_t size);				//* offset is without any usage here. ���Ҳ�Ƿ������ģ��ж��ٶ����٣�ͨ�������

		ssize_t				readAtNonBlocking(off64_t offset, void* data, size_t size);		//* offset is without any usage here. ����size�Ͳ���ֱ�ӷ���
		ssize_t 			readAtBlocking(off64_t offset, void *data, size_t size);        //������ʽ���ȹ�size�Ŷ�

		void				queueBuffer(DataNode* node);									//* add a new data node to LiveDataSource;

		DataNode*			getBuffer(void);												//* get an empty node to receive data.

		void				queueEOS(status_t finalResult);									//* tell end of stream.

		int 				seekToSeqNum(int seqNum);										//* seek to the start of a certain ts slice.

		int					getFirstSeqNumInBuffer(void);									//* get the sequence number of the first ts slice still in buffer.

		int					getLastSeqNumInBuffer(void);									//* get the sequence number of the last ts slice in buffer.

		bool				needBufferNewData(void);										//* whether has empty space to buffer new data.

		void				reset();														//* reset the LiveDataSource, all data in buffer will be discard.

		int					getValidDataSize(void);											//* how much valid data size in buffer.

		bool				eof(void);														//* Whether all data been download.

		int                 getBandwidth(void);

		void                updataBWEstimator(int downloadSize, int64_t timeCost);
		status_t 			getNetWorkStatus(int32_t *networkstatus);

	protected:
		virtual ~LiveDataSource();

	private:

		static const int MAX_NODE_COUNT		= 1024;
		static const int NODE_BUFFER_SIZE	= 32*1024;
		static const int BIG_BUFFER_SIZE    = MAX_NODE_COUNT*NODE_BUFFER_SIZE;
		
		int 		mMaxNodeCount;							//��mBigBuffer�ǿգ����ʾʵ�����뵽�Ľڵ�������mBigBufferΪ�գ����ʾ>=mMaxNodeCount�����벻����
		void*		mBigBuffer;

		DataNode	mNodes[MAX_NODE_COUNT];
		int			mStartBufferPos;  //�ɶ�����ʼλ�ã����ܱ��Ѷ�������δ����д��
		int			mReadPos;//��ǰ������λ��
		int			mWritePos;//��д��λ��,��ǰҪ�ڴ˴�д��
		int			mValidNodeCount;
		int			mPassedNodeCount;//bigbuf���Ѷ��Ľڵ����
		size_t			mValidDataSize;//bigbuf �д���δ�������������μ�����readAtNonBlocking()

		int			mFirstSeqNumInBuffer;
		int			mLastSeqNumInBuffer;

		Mutex		mLock;
		Condition   mCondition;

		status_t	mFinalResult;


		int         mBandwidth;	//* in unit of kbps.��ǰ����

		bw_est_node_t mBWArray[BANDWIDTH_ARRAY_SIZE];
		int           mBWWritePos;
		int           mBWCount;

		ssize_t 	readAtNonBlocking2(off64_t offset, void* data, size_t size);//���ܱ�������
		DISALLOW_EVIL_CONSTRUCTORS(LiveDataSource);
	};

}  // namespace android

#endif


