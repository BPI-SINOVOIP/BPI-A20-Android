#ifndef CDX_SOCKET_UTIL_H
#define CDX_SOCKET_UTIL_H
#include <CdxTypes.h>

#include <sys/types.h>          /* See NOTES */
#include <sys/socket.h>
#include <arpa/inet.h>

#define CDX_SELECT_TIMEOUT 100000L

#ifdef __cplusplus
extern "C"
{
#endif
cdx_int32 CdxSockSetBlocking(cdx_int32 sockfd, cdx_int32 blocking);

cdx_err CdxMakeSocketBlocking(cdx_int32 s, cdx_bool blocking);

cdx_void CdxSocketMakePortPair(cdx_int32 *rtpSocket, cdx_int32 *rtcpSocket,
                                cdx_uint32 *rtpPort);
                                
cdx_int32 CdxSockAddrConstruct(struct sockaddr_in *dest, cdx_int8 *ip, cdx_int32 port);

cdx_ssize CdxSockNoblockRecv(cdx_int32 sockfd, void *buf, cdx_size len);

cdx_ssize CdxSockAsynRecv(cdx_int32 sockfd, void *buf, cdx_size len, 
                        cdx_long timeoutUs, cdx_int32 *pForceStop);
                        
cdx_ssize CdxSockAsynSend(cdx_int32 sockfd, const void *buf, cdx_size len, 
                          cdx_long timeoutUs, cdx_int32 *pForceStop);
                          
cdx_int32 CdxAsynSocket(cdx_int32 nRecvBufLen, cdx_int32 *nRecvBufLenRet);

cdx_int32  CdxSockAsynConnect(cdx_int32 sockfd, const struct sockaddr *addr, 
            socklen_t addrlen, cdx_long timeoutUs, cdx_int32 *pForceStop);
            
#ifdef __cplusplus
}
#endif

#endif
