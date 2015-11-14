
/*============================================================================
                            O p e n M A X   w r a p p e r s
                             O p e n  M A X   C o r e

 This module contains the implementation of the OpenMAX core Macros which
 operate directly on the component.

*//*========================================================================*/

//////////////////////////////////////////////////////////////////////////////
//                             Include Files
//////////////////////////////////////////////////////////////////////////////
//#define LOG_NDEBUG 0
#define LOG_TAG "aw_omx_core_cmp"
#include <utils/Log.h>
#include "AWOmxDebug.h"


#include "aw_omx_common.h"
#include "omx_core_cmp.h"
#include "aw_omx_component.h"
#include <string.h>


void* aw_omx_create_component_wrapper(OMX_PTR obj_ptr)
{
    aw_omx_component *pThis        = (aw_omx_component *)obj_ptr;
    OMX_COMPONENTTYPE* component   = &(pThis->m_cmp);
    memset(&pThis->m_cmp,0,sizeof(OMX_COMPONENTTYPE));

    component->nSize               = sizeof(OMX_COMPONENTTYPE);
    component->nVersion.nVersion   = OMX_SPEC_VERSION;
    component->pApplicationPrivate = 0;
    component->pComponentPrivate   = obj_ptr;

    component->AllocateBuffer      = &aw_omx_component_allocate_buffer;
    component->FreeBuffer          = &aw_omx_component_free_buffer;
    component->GetParameter        = &aw_omx_component_get_parameter;
    component->SetParameter        = &aw_omx_component_set_parameter;
    component->SendCommand         = &aw_omx_component_send_command;
    component->FillThisBuffer      = &aw_omx_component_fill_this_buffer;
    component->EmptyThisBuffer     = &aw_omx_component_empty_this_buffer;
    component->GetState            = &aw_omx_component_get_state;
    component->GetComponentVersion = &aw_omx_component_get_version;
    component->GetConfig           = &aw_omx_component_get_config;
    component->SetConfig           = &aw_omx_component_set_config;
    component->GetExtensionIndex   = &aw_omx_component_get_extension_index;
    component->ComponentTunnelRequest = &aw_omx_component_tunnel_request;
    component->UseBuffer           = &aw_omx_component_use_buffer;
    component->SetCallbacks        = &aw_omx_component_set_callbacks;
    component->UseEGLImage         = &aw_omx_component_use_EGL_image;
    component->ComponentRoleEnum   = &aw_omx_component_role_enum;
    component->ComponentDeInit     = &aw_omx_component_deinit;
    return (void *)component;
}



/************************************************************************/
/*               COMPONENT INTERFACE                                    */
/************************************************************************/

OMX_ERRORTYPE aw_omx_component_init(OMX_IN OMX_HANDLETYPE hComp, OMX_IN OMX_STRING componentName)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_init %x\n",(unsigned)hComp);

	if(pThis)
	{
		// call the init function
		eRet = pThis->component_init(componentName);

		if(eRet != OMX_ErrorNone)
		{
			//  in case of error, please destruct the component created
			delete pThis;
		}
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_get_version(OMX_IN OMX_HANDLETYPE               hComp,
                                           OMX_OUT OMX_STRING          componentName,
                                           OMX_OUT OMX_VERSIONTYPE* componentVersion,
                                           OMX_OUT OMX_VERSIONTYPE*      specVersion,
                                           OMX_OUT OMX_UUIDTYPE*       componentUUID)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_get_version %x, %s , %x\n",(unsigned)hComp,componentName,(unsigned)componentVersion);
	if(pThis)
	{
		eRet = pThis->get_component_version(hComp,componentName,componentVersion,specVersion,componentUUID);
	}

	return eRet;
}

OMX_ERRORTYPE aw_omx_component_send_command(OMX_IN OMX_HANDLETYPE hComp,
                                            OMX_IN OMX_COMMANDTYPE  cmd,
                                            OMX_IN OMX_U32       param1,
                                            OMX_IN OMX_PTR      cmdData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_send_command %x, %d , %d\n",(unsigned)hComp,(unsigned)cmd,(unsigned)param1);

	if(pThis)
	{
		eRet = pThis->send_command(hComp,cmd,param1,cmdData);
	}

	return eRet;
}

OMX_ERRORTYPE aw_omx_component_get_parameter(OMX_IN OMX_HANDLETYPE     hComp,
                                             OMX_IN OMX_INDEXTYPE paramIndex,
                                             OMX_INOUT OMX_PTR     paramData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_get_parameter %x, %x , %d\n",(unsigned)hComp,(unsigned)paramData,paramIndex);

	if(pThis)
	{
		eRet = pThis->get_parameter(hComp,paramIndex,paramData);
	}

	return eRet;
}

OMX_ERRORTYPE aw_omx_component_set_parameter(OMX_IN OMX_HANDLETYPE     hComp,
                                             OMX_IN OMX_INDEXTYPE paramIndex,
                                             OMX_IN OMX_PTR        paramData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_set_parameter %x, %x , %d\n",(unsigned)hComp,(unsigned)paramData,paramIndex);

	if(pThis)
	{
		eRet = pThis->set_parameter(hComp,paramIndex,paramData);
	}
	return eRet;
}


OMX_ERRORTYPE aw_omx_component_get_config(OMX_IN OMX_HANDLETYPE      hComp,
                                          OMX_IN OMX_INDEXTYPE configIndex,
                                          OMX_INOUT OMX_PTR     configData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_get_config %x\n",(unsigned)hComp);

	if(pThis)
	{
		eRet = pThis->get_config(hComp, configIndex, configData);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_set_config(OMX_IN OMX_HANDLETYPE      hComp,
                                          OMX_IN OMX_INDEXTYPE configIndex,
                                          OMX_IN OMX_PTR        configData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_set_config %x\n",(unsigned)hComp);

	if(pThis)
	{
		eRet = pThis->set_config(hComp, configIndex, configData);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_get_extension_index(OMX_IN OMX_HANDLETYPE      hComp,
                                                   OMX_IN OMX_STRING      paramName,
                                                   OMX_OUT OMX_INDEXTYPE* indexType)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	if(pThis)
	{
		eRet = pThis->get_extension_index(hComp,paramName,indexType);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_get_state(OMX_IN OMX_HANDLETYPE  hComp,
                                         OMX_OUT OMX_STATETYPE* state)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_get_state %x\n",(unsigned)hComp);

	if(pThis)
	{
		eRet = pThis->get_state(hComp,state);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_tunnel_request(OMX_IN OMX_HANDLETYPE                hComp,
                                              OMX_IN OMX_U32                        port,
                                              OMX_IN OMX_HANDLETYPE        peerComponent,
                                              OMX_IN OMX_U32                    peerPort,
                                              OMX_INOUT OMX_TUNNELSETUPTYPE* tunnelSetup)
{
	DEBUG_PRINT("Error: aw_omx_component_tunnel_request Not Implemented\n");
	return OMX_ErrorNotImplemented;
}


OMX_ERRORTYPE aw_omx_component_use_buffer(OMX_IN OMX_HANDLETYPE                hComp,
                                          OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                          OMX_IN OMX_U32                        port,
                                          OMX_IN OMX_PTR                     appData,
                                          OMX_IN OMX_U32                       bytes,
                                          OMX_IN OMX_U8*                      buffer)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_use_buffer %x\n",(unsigned)hComp);

	if(pThis)
	{
		eRet = pThis->use_buffer(hComp, bufferHdr, port, appData, bytes, buffer);
	}

	return eRet;
}


// aw_omx_component_allocate_buffer  -- API Call
OMX_ERRORTYPE aw_omx_component_allocate_buffer(OMX_IN OMX_HANDLETYPE                hComp,
                                               OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                               OMX_IN OMX_U32                        port,
                                               OMX_IN OMX_PTR                     appData,
                                               OMX_IN OMX_U32                       bytes)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_allocate_buffer %x, %x , %d\n",(unsigned)hComp,(unsigned)bufferHdr,(unsigned)port);

	if(pThis)
	{
		eRet = pThis->allocate_buffer(hComp,bufferHdr,port,appData,bytes);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_free_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                           OMX_IN OMX_U32                 port,
                                           OMX_IN OMX_BUFFERHEADERTYPE* buffer)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_free_buffer[%d] %x, %x\n", (unsigned)port, (unsigned)hComp, (unsigned)buffer);

	if(pThis)
	{
		eRet = pThis->free_buffer(hComp,port,buffer);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_empty_this_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                                 OMX_IN OMX_BUFFERHEADERTYPE* buffer)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	//DEBUG_PRINT("OMXCORE: aw_omx_component_empty_this_buffer %x, %x\n",(unsigned)hComp,(unsigned)buffer);

	if(pThis)
	{
		eRet = pThis->empty_this_buffer(hComp,buffer);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_fill_this_buffer(OMX_IN OMX_HANDLETYPE         hComp,
                                                OMX_IN OMX_BUFFERHEADERTYPE* buffer)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	//DEBUG_PRINT("OMXCORE: aw_omx_component_fill_this_buffer %x, %x\n",(unsigned)hComp,(unsigned)buffer);

	if(pThis)
	{
		eRet = pThis->fill_this_buffer(hComp,buffer);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_set_callbacks(OMX_IN OMX_HANDLETYPE        hComp,
                                             OMX_IN OMX_CALLBACKTYPE* callbacks,
                                             OMX_IN OMX_PTR             appData)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_set_callbacks %x, %x , %x\n",(unsigned)hComp,(unsigned)callbacks,(unsigned)appData);

	if(pThis)
	{
		eRet = pThis->set_callbacks(hComp,callbacks,appData);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_deinit(OMX_IN OMX_HANDLETYPE hComp)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_deinit %x\n",(unsigned)hComp);

	if(pThis)
	{
		// call the deinit fuction first
		OMX_STATETYPE state;
		pThis->get_state(hComp,&state);
		DEBUG_PRINT("Calling FreeHandle in state %d \n", state);
		eRet = pThis->component_deinit(hComp);
		// destroy the component.
		delete pThis;
		((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate = NULL;
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_use_EGL_image(OMX_IN OMX_HANDLETYPE                hComp,
                                             OMX_INOUT OMX_BUFFERHEADERTYPE** bufferHdr,
                                             OMX_IN OMX_U32                        port,
                                             OMX_IN OMX_PTR                     appData,
                                             OMX_IN void*                      eglImage)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_use_EGL_image %x, %x , %d\n",(unsigned)hComp,(unsigned)bufferHdr,(unsigned)port);
	if(pThis)
	{
		eRet = pThis->use_EGL_image(hComp,bufferHdr,port,appData,eglImage);
	}

	return eRet;
}


OMX_ERRORTYPE aw_omx_component_role_enum(OMX_IN OMX_HANDLETYPE hComp,
                                         OMX_OUT OMX_U8*        role,
                                         OMX_IN OMX_U32        index)
{
	OMX_ERRORTYPE eRet = OMX_ErrorBadParameter;
	aw_omx_component *pThis = (hComp)? (aw_omx_component *)(((OMX_COMPONENTTYPE *)hComp)->pComponentPrivate):NULL;
	DEBUG_PRINT("OMXCORE: aw_omx_component_role_enum %x, %x , %d\n",(unsigned)hComp,(unsigned)role,(unsigned)index);

	if(pThis)
  	{
		eRet = pThis->component_role_enum(hComp,role,index);
  	}

	return eRet;
}
