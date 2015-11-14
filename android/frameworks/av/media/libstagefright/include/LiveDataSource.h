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
	    char	mBufferFlag;    //标记bigbuffer中每个节点的可写状态，0表示是空的，可以写
		int		hasBeenRead;	//* whether data of this node has been read.//其实可以没有，因为我们用mStartBufferPos，mReadPos已经是充分的了，整个程序中只有对它的赋值，并没有对它的使用
		int		seqNum;			//* sequence number of the ts slice which the data of this node belong to.
		int		subSeqNum;		//* for example, subSeqNum = 3 means this is the third data node of the ts data slice.   //??
		int		bufSize;		//* how big is ther buffer used by this node.
		int		dataSize;		//* how much valid data in this node. //dataSize是节点中写入数据的数量（因为bufSize可能没写满），offset是节点中读到的位置，也就是已读的数量. dataSize - offset是节点中未读的数据量
		int		offset;			//* how much data of this node has been read.
		void*	data;			//* start address of the data(buffer).
	}DataNode;

	struct LiveDataSource : public DataSource
	{
		LiveDataSource();

		virtual status_t	initCheck() const;												
		virtual ssize_t		readAt(off64_t offset, void* data, size_t size);				//* offset is without any usage here. 这个也是非阻塞的，有多少读多少，通常用这个

		ssize_t				readAtNonBlocking(off64_t offset, void* data, size_t size);		//* offset is without any usage here. 不够size就不读直接返回
		ssize_t 			readAtBlocking(off64_t offset, void *data, size_t size);        //阻塞方式，等够size才读

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
		
		int 		mMaxNodeCount;							//若mBigBuffer非空，则表示实际申请到的节点数，若mBigBuffer为空，则表示>=mMaxNodeCount是申请不到的
		void*		mBigBuffer;

		DataNode	mNodes[MAX_NODE_COUNT];
		int			mStartBufferPos;  //可读的起始位置，可能被已读，但并未重新写入
		int			mReadPos;//当前读到的位置
		int			mWritePos;//可写的位置,当前要在此处写入
		int			mValidNodeCount;
		int			mPassedNodeCount;//bigbuf中已读的节点计数
		size_t			mValidDataSize;//bigbuf 中纯的未读的数据量，参见函数readAtNonBlocking()

		int			mFirstSeqNumInBuffer;
		int			mLastSeqNumInBuffer;

		Mutex		mLock;
		Condition   mCondition;

		status_t	mFinalResult;


		int         mBandwidth;	//* in unit of kbps.当前带宽

		bw_est_node_t mBWArray[BANDWIDTH_ARRAY_SIZE];
		int           mBWWritePos;
		int           mBWCount;

		ssize_t 	readAtNonBlocking2(off64_t offset, void* data, size_t size);//不能被外层调用
		DISALLOW_EVIL_CONSTRUCTORS(LiveDataSource);
	};

}  // namespace android

#endif


