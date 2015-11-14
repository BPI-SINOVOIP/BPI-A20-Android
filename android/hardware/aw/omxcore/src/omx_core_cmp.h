
/*============================================================================
                            O p e n M A X   w r a p p e r s
                             O p e n  M A X   C o r e

 OpenMAX Core Macros interface.

============================================================================*/

//////////////////////////////////////////////////////////////////////////////
//                             Include Files
//////////////////////////////////////////////////////////////////////////////
#ifndef OMX_CORE_CMP_H
#define OMX_CORE_CMP_H

#include "OMX_Core.h"

#define printf ALOGV
#define DEBUG_PRINT_ERROR printf
#define DEBUG_PRINT       printf
#define DEBUG_DETAIL      printf

#ifdef __cplusplus
extern "C" {
#endif


void* aw_omx_create_component_wrapper(OMX_PTR obj_ptr);


OMX_ERRORTYPE aw_omx_component_init(OMX_IN OMX_HANDLETYPE hComp, OMX_IN OMX_STRING componentName);


OMX_ERRORTYPE aw_omx_component_get_version(OMX_IN OMX_HANDLETYPE               hComp,
                                           OMX_OUT OMX_STRING          componentName,
                                           OMX_OUT OMX_VERSIONTYPE* componentVersion,
                                           OMX_OUT OMX_VERSIONTYPE*      specVersion,
                                           OMX_OUT OMX_UUIDTYPE*       componentUUID);

OMX_ERRORTYPE aw_omx_component_send_command(OMX_IN OMX_HANDLETYPE hComp,
                                            OMX_IN OMX_COMMANDTYPE  cmd,
                                            OMX_IN OMX_U32       param1,
                                            OMX_IN OMX_PTR      cmdData);

OMX_ERRORTYPE aw_omx_component_get_parameter(OMX_IN OMX_HANDLETYPE     hComp,
                                             OMX_IN OMX_INDEXTYPE paramIndex,
                                             OMX_INOUT OMX_PTR     paramData);

OMX_ERRORTYPE aw_omx_component_set_parameter(OMX_IN OMX_HANDLETYPE     hComp,
                                             OMX_IN OMX_INDEXTYPE paramIndex,
                                             OMX_IN OMX_PTR        paramData);

OMX_ERRORTYPE aw_omx_component_get_config(OMX_IN OMX_HANDLETYPE      hComp,
                                          OMX_IN OMX_INDEXTYPE configIndex,
                                          OMX_INOUT OMX_PTR     configData);

OMX_ERRORTYPE aw_omx_component_set_config(OMX_IN OMX_HANDLETYPE      hComp,
                                          OMX_IN OMX_INDEXTYPE configIndex,
                                          OMX_IN OMX_PTR        configData);

OMX_ERRORTYPE aw_omx_component_get_extension_index(OMX_IN OMX_HANDLETYPE      hComp,
                                                   OMX_IN OMX_STRING      paramName,
                                                   OMX_OUT OMX_INDEXTYPE* indexType);

OMX_ERRORTYPE aw_omx_component_get_state(OMX_IN OMX_HANDLETYPE  hComp,
                                         OMX_OUT OMX_STATETYPE* state);

OMX_ERRORTYPE aw_omx_component_tunnel_request(OMX_IN OMX_HANDLETYPE                hComp,
                                              OMX_IN OMX_U32                        port,
                                              OMX_IN OMX_HANDLETYPE        peerComponent,
                                              OMX_IN OMX_U32                    peerPort,
                                              OMX_INOUT OMX_TUNNELSETUPTYPE* tunnelSetup);

OMX_ERRORTYPE aw_omx_component_use_buffer(OMX_IN OMX_HANDLETYPE                hComp,
                                          OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                          OMX_IN OMX_U32                        port,
                                          OMX_IN OMX_PTR                     appData,
                                          OMX_IN OMX_U32                       bytes,
                                          OMX_IN OMX_U8*                      buffer);


// aw_omx_component_allocate_buffer  -- API Call
OMX_ERRORTYPE aw_omx_component_allocate_buffer(OMX_IN OMX_HANDLETYPE                hComp,
                                               OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                               OMX_IN OMX_U32                        port,
                                               OMX_IN OMX_PTR                     appData,
                                               OMX_IN OMX_U32                       bytes);

OMX_ERRORTYPE aw_omx_component_free_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                           OMX_IN OMX_U32                 port,
                                           OMX_IN OMX_BUFFERHEADERTYPE* buffer);

OMX_ERRORTYPE aw_omx_component_empty_this_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                                 OMX_IN OMX_BUFFERHEADERTYPE* buffer);

OMX_ERRORTYPE aw_omx_component_fill_this_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                                OMX_IN OMX_BUFFERHEADERTYPE* buffer);

OMX_ERRORTYPE aw_omx_component_set_callbacks(OMX_IN OMX_HANDLETYPE        hComp,
                                             OMX_IN OMX_CALLBACKTYPE* callbacks,
                                             OMX_IN OMX_PTR             appData);

OMX_ERRORTYPE aw_omx_component_deinit(OMX_IN OMX_HANDLETYPE hComp);

OMX_ERRORTYPE aw_omx_component_use_EGL_image(OMX_IN OMX_HANDLETYPE                hComp,
                                             OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                             OMX_IN OMX_U32                        port,
                                             OMX_IN OMX_PTR                     appData,
                                             OMX_IN void*                      eglImage);

OMX_ERRORTYPE aw_omx_component_role_enum(OMX_IN OMX_HANDLETYPE hComp,
                                         OMX_OUT OMX_U8*        role,
                                         OMX_IN OMX_U32        index);

#ifdef __cplusplus
}
#endif

#endif

