/******************************************************************************
 *
 *  Copyright (C) 2009-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

/******************************************************************************
 *
 *  this file contains the NCI transport internal definitions and functions.
 *
 ******************************************************************************/

#ifndef NFC_HAL_INT_H
#define NFC_HAL_INT_H

#include "nfc_hal_target.h"
#include "gki.h"
#include "nci_defs.h"
#include "nfc_brcm_defs.h"
#include "nfc_hal_api.h"
#include "nfc_hal_int_api.h"

#ifdef __cplusplus
extern "C" {
#endif

/****************************************************************************
** NFC HAL TASK transport definitions
****************************************************************************/
/* NFC HAL Task event masks */
#define NFC_HAL_TASK_EVT_DATA_RDY               EVENT_MASK (APPL_EVT_0)
#define NFC_HAL_TASK_EVT_INITIALIZE             EVENT_MASK (APPL_EVT_5)
#define NFC_HAL_TASK_EVT_TERMINATE              EVENT_MASK (APPL_EVT_6)
#define NFC_HAL_TASK_EVT_POWER_CYCLE            EVENT_MASK (APPL_EVT_7)

#define NFC_HAL_TASK_EVT_MBOX                   (TASK_MBOX_0_EVT_MASK)

/* NFC HAL Task mailbox definitions */
#define NFC_HAL_TASK_MBOX                       (TASK_MBOX_0)

/* NFC HAL Task Timer events */
#ifndef NFC_HAL_QUICK_TIMER_EVT_MASK
#define NFC_HAL_QUICK_TIMER_EVT_MASK            (TIMER_0_EVT_MASK)
#endif

#ifndef NFC_HAL_QUICK_TIMER_ID
#define NFC_HAL_QUICK_TIMER_ID                  (TIMER_0)
#endif

/* NFC HAL Task Timer types */
#define NFC_HAL_TTYPE_NCI_WAIT_RSP              0
#define NFC_HAL_TTYPE_POWER_CYCLE               1

/* NFC HAL Task Wait Response flag */
#define NFC_HAL_WAIT_RSP_CMD                    0x10    /* wait response on an NCI command                  */
#define NFC_HAL_WAIT_RSP_VSC                    0x20    /* wait response on an NCI vendor specific command  */
#define NFC_HAL_WAIT_RSP_PROP                   0x40    /* wait response on a proprietary command           */
#define NFC_HAL_WAIT_RSP_NONE                   0x00    /* not waiting for anything                         */

typedef UINT8 tNFC_HAL_WAIT_RSP;

typedef UINT16 tNFC_HAL_HCI_EVT;


#define NFC_HAL_HCI_DH_TARGET_HANDLE            0xF2
#define NFC_HAL_HCI_UICC0_TARGET_HANDLE         0xF3
#define NFC_HAL_HCI_UICC1_TARGET_HANDLE         0xF4

#define NFC_HAL_HCI_SESSION_ID_LEN              0x08
#define NFC_HAL_HCI_NETWK_INFO_SIZE             184
#define NFC_HAL_HCI_DH_NETWK_INFO_SIZE          111

#define NFC_HAL_HCI_ADM_NOTIFY_ALL_PIPE_CLEARED 0x15
#define NFC_HAL_HCI_ADMIN_PIPE                  0x01
#define NFC_HAL_HCI_HOST_ID_UICC0               0x02        /* Host ID for UICC 0 */
#define NFC_HAL_HCI_HOST_ID_UICC1               0x03        /* Host ID for UICC 1 */
#define NFC_HAL_HCI_COMMAND_TYPE                0x00

/* NFC HAL transport configuration */
typedef struct
{
    BOOLEAN         shared_transport;           /* TRUE if using shared HCI/NCI transport */
    UINT8           userial_baud;
    UINT8           userial_fc;
} tNFC_HAL_TRANS_CFG;

#ifdef TESTER
#define NFC_HAL_TRANS_CFG_QUALIFIER               /* For Insight, ncit_cfg is runtime-configurable */
#else
#define NFC_HAL_TRANS_CFG_QUALIFIER   const       /* For all other platforms, ncit_cfg is constant */
#endif
extern NFC_HAL_TRANS_CFG_QUALIFIER tNFC_HAL_TRANS_CFG nfc_hal_trans_cfg;

/*****************************************************************************
* BT HCI definitions
*****************************************************************************/
#define BT_HDR      NFC_HDR

/* Tranport message type */
#define HCIT_TYPE_COMMAND   0x01
#define HCIT_TYPE_EVENT     0x04
#define HCIT_TYPE_NFC       0x10

/* Vendor-Specific BT HCI definitions */
#define HCI_SUCCESS                         0x00
#define HCI_GRP_VENDOR_SPECIFIC             (0x3F << 10)            /* 0xFC00 */
#define HCI_BRCM_WRITE_SLEEP_MODE           (0x0027 | HCI_GRP_VENDOR_SPECIFIC)
#define HCI_GRP_HOST_CONT_BASEBAND_CMDS     (0x03 << 10)            /* 0x0C00 */
#define HCI_RESET                           (0x0003 | HCI_GRP_HOST_CONT_BASEBAND_CMDS)
#define HCI_COMMAND_COMPLETE_EVT            0x0E
#define HCI_BRCM_WRITE_SLEEP_MODE_LENGTH    12
#define HCI_BRCM_UPDATE_BAUD_RATE_UNENCODED_LENGTH      0x06
#define HCIE_PREAMBLE_SIZE                  2

/****************************************************************************
** Internal constants and definitions
****************************************************************************/

/* NFC HAL receiving states */
enum
{
    NFC_HAL_RCV_IDLE_ST,            /* waiting for packet type byte             */
    NFC_HAL_RCV_NCI_MSG_ST,         /* waiting for the first byte of NCI header */
    NFC_HAL_RCV_NCI_HDR_ST,         /* reading NCI header                       */
    NFC_HAL_RCV_NCI_PAYLOAD_ST,     /* reading NCI payload                      */
    NFC_HAL_RCV_BT_MSG_ST,          /* waiting for the first byte of BT header  */
    NFC_HAL_RCV_BT_HDR_ST,          /* reading BT HCI header                    */
    NFC_HAL_RCV_BT_PAYLOAD_ST       /* reading BT HCI payload                   */
};

/* errors during NCI packet reassembly process */
#define NFC_HAL_NCI_RAS_TOO_BIG             0x01
#define NFC_HAL_NCI_RAS_ERROR               0x02
typedef UINT8 tNFC_HAL_NCI_RAS;

/* NFC HAL power mode */
enum
{
    NFC_HAL_POWER_MODE_FULL,            /* NFCC is full power mode      */
    NFC_HAL_POWER_MODE_LAST
};
typedef UINT8 tNFC_HAL_POWER_MODE;


/* NFC HAL event for low power mode */
enum
{
    NFC_HAL_LP_TX_DATA_EVT,                 /* DH is sending data to NFCC   */
    NFC_HAL_LP_RX_DATA_EVT,                 /* DH received data from NFCC   */
    NFC_HAL_LP_TIMEOUT_EVT,                 /* Timeout                      */
    NFC_HAL_LP_LAST_EVT
};
typedef UINT8 tNFC_HAL_LP_EVT;

#define NFC_HAL_ASSERT_NFC_WAKE      0x00   /* assert NFC_WAKE      */
#define NFC_HAL_DEASSERT_NFC_WAKE    0x01   /* deassert NFC_WAKE    */

#define NFC_HAL_BT_HCI_CMD_HDR_SIZE     3   /* opcode (2) +  length (1)    */
#define NFC_HAL_CMD_TOUT            (2000)  /* timeout for NCI CMD (in ms) */

#define NFC_HAL_SAVED_HDR_SIZE          (2)
#define NFC_HAL_SAVED_CMD_SIZE          (2)

#ifndef NFC_HAL_DEBUG
#define NFC_HAL_DEBUG  TRUE
#endif

#if (NFC_HAL_DEBUG == TRUE)
extern const char * const nfc_hal_init_state_str[];
#define NFC_HAL_SET_INIT_STATE(state)  NCI_TRACE_DEBUG3 ("init state: %d->%d(%s)", nfc_hal_cb.dev_cb.initializing_state, state, nfc_hal_init_state_str[state]); nfc_hal_cb.dev_cb.initializing_state = state;
#else
#define NFC_HAL_SET_INIT_STATE(state)  nfc_hal_cb.dev_cb.initializing_state = state;
#endif


/* NFC HAL - NFCC initializing state */
enum
{
    NFC_HAL_INIT_STATE_IDLE,               /* Initialization is done                */
    NFC_HAL_INIT_STATE_W4_XTAL_SET,        /* Waiting for crystal setting rsp       */
    NFC_HAL_INIT_STATE_W4_RESET,           /* Waiting for reset rsp                 */
    NFC_HAL_INIT_STATE_W4_BUILD_INFO,      /* Waiting for build info rsp            */
    NFC_HAL_INIT_STATE_W4_PATCH_INFO,      /* Waiting for patch info rsp            */
    NFC_HAL_INIT_STATE_W4_APP_COMPLETE,    /* Waiting for complete from application */
    NFC_HAL_INIT_STATE_W4_POST_INIT_DONE,  /* Waiting for complete of post init     */
    NFC_HAL_INIT_STATE_W4_CONTROL_DONE,    /* Waiting for control release           */
    NFC_HAL_INIT_STATE_W4_PREDISCOVER_DONE,/* Waiting for complete of prediscover   */
    NFC_HAL_INIT_STATE_W4_RE_INIT,         /* Waiting for reset rsp on ReInit       */
    NFC_HAL_INIT_STATE_CLOSING             /* Shutting down                         */
};
typedef UINT8 tNFC_HAL_INIT_STATE;

/* NFC HAL - NFCC config items during post initialization */
enum
{
    NFC_HAL_DM_CONFIG_LPTD,
    NFC_HAL_DM_CONFIG_PLL_325,
    NFC_HAL_DM_CONFIG_START_UP,
    NFC_HAL_DM_CONFIG_I93_DATA_RATE,
    NFC_HAL_DM_CONFIG_FW_FSM,
    NFC_HAL_DM_CONFIG_START_UP_VSC,
    NFC_HAL_DM_CONFIG_NONE
};
typedef UINT8 tNFC_HAL_DM_CONFIG;

/* callback function prototype */
typedef struct
{
    UINT16  opcode;
    UINT16  param_len;
    UINT8   *p_param_buf;
} tNFC_HAL_BTVSC_CPLT;

typedef void (tNFC_HAL_BTVSC_CPLT_CBACK) (tNFC_HAL_BTVSC_CPLT *p1);


/* data type for NFC_HAL_HCI_RSP_NV_READ_EVT */
typedef struct
{
    NFC_HDR           hdr;
    UINT8             block;
    UINT16            size;
    tHAL_NFC_STATUS   status;
} tNFC_HAL_HCI_RSP_NV_READ_EVT;

/* data type for NFC_HAL_HCI_RSP_NV_WRITE_EVT */
typedef struct
{
    NFC_HDR           hdr;
    tHAL_NFC_STATUS   status;
} tNFC_HAL_HCI_RSP_NV_WRITE_EVT;


/* union of all event data types */
typedef union
{
    NFC_HDR                         hdr;
    /* Internal events */
    tNFC_HAL_HCI_RSP_NV_READ_EVT    nv_read;
    tNFC_HAL_HCI_RSP_NV_WRITE_EVT   nv_write;
} tNFC_HAL_HCI_EVENT_DATA;

/*****************************************************************************
** Control block for NFC HAL
*****************************************************************************/

/* Patch RAM Download Control block */

/* PRM states */
enum
{
    NFC_HAL_PRM_ST_IDLE,

    /* Secure patch download stated */
    NFC_HAL_PRM_ST_SPD_GET_VERSION,
    NFC_HAL_PRM_ST_SPD_COMPARE_VERSION,
    NFC_HAL_PRM_ST_SPD_GET_PATCH_HEADER,
    NFC_HAL_PRM_ST_SPD_DOWNLOADING,
    NFC_HAL_PRM_ST_SPD_AUTHENTICATING,
    NFC_HAL_PRM_ST_SPD_AUTH_DONE
};
typedef UINT8 tNFC_HAL_PRM_STATE;

/* Maximum number of patches (currently 2: LPM and FPM) */
#define NFC_HAL_PRM_MAX_PATCH_COUNT    2
#define NFC_HAL_PRM_PATCH_MASK_ALL     0xFFFFFFFF

/* Structures for PRM Control Block */
typedef struct
{
    UINT8               power_mode;
    UINT16              len;
} tNFC_HAL_PRM_PATCHDESC;

typedef struct
{
    tNFC_HAL_PRM_STATE  state;                  /* download state */
    UINT32              flags;                  /* internal flags */
    UINT16              cur_patch_len_remaining;/* bytes remaining in patchfile to process     */
    const UINT8*        p_cur_patch_data;       /* pointer to patch currently being downloaded */
    UINT16              cur_patch_offset;       /* offset of next byte to process              */
    UINT32              dest_ram;
    TIMER_LIST_ENT      timer;                  /* Timer for patch download                    */

    /* Secure Patch Download */
    UINT32              spd_patch_needed_mask;  /* Mask of patches that need to be downloaded */
    UINT32              spd_nvm_patch_mask;     /* Mask of patches currently in NVM */
    UINT16              spd_project_id;         /* Current project_id of patch in nvm */
    UINT16              spd_nvm_max_size;
    UINT16              spd_patch_max_size;
    UINT16              spd_fpm_patch_size;
    UINT16              spd_lpm_patch_size;

    UINT8               spd_patch_count;        /* Number of patches left to download */
    UINT8               spd_cur_patch_idx;      /* Current patch being downloaded */
    UINT16              spd_ver_major;          /* Current major version of patch in nvm */
    UINT16              spd_ver_minor;          /* Current minor version of patch in nvm */
    tNFC_HAL_PRM_PATCHDESC spd_patch_desc[NFC_HAL_PRM_MAX_PATCH_COUNT];

    /* I2C-patch */
    UINT8               *p_spd_patch;           /* pointer to spd patch             */
    UINT16              spd_patch_len_remaining;/* patch length                     */
    UINT16              spd_patch_offset;       /* offset of next byte to process   */

    tNFC_HAL_PRM_FORMAT format;                 /* format of patch ram              */
    tNFC_HAL_PRM_CBACK  *p_cback;               /* Callback for download status notifications */
    UINT32              patchram_delay;         /* the dealy after patch */
} tNFC_HAL_PRM_CB;

/* Patch for I2C fix */
typedef struct
{
    UINT8               *p_patch;               /* patch for i2c fix                */
    UINT32              prei2c_delay;           /* the dealy after preI2C patch */
    UINT16              len;                    /* i2c patch length                 */
} tNFC_HAL_PRM_I2C_FIX_CB;

/* Control block for NCI transport */
typedef struct
{
    UINT8               nci_ctrl_size;      /* Max size for NCI messages                              */
    UINT8               rcv_state;          /* current rx state                                       */
    UINT16              rcv_len;            /* bytes remaining to be received in current rx state     */
    NFC_HDR             *p_rcv_msg;         /* buffer to receive NCI message                          */
    NFC_HDR             *p_frag_msg;        /* fragmented NCI message; waiting for last fragment      */
    NFC_HDR             *p_pend_cmd;        /* pending NCI message; waiting for NFCC state to be free */
    tNFC_HAL_NCI_RAS    nci_ras;            /* nci reassembly error status                            */
    TIMER_LIST_ENT      nci_wait_rsp_timer; /* Timer for waiting for nci command response             */
    tNFC_HAL_WAIT_RSP   nci_wait_rsp;       /* nci wait response flag                                 */
    UINT8               last_hdr[NFC_HAL_SAVED_HDR_SIZE];/* part of last NCI command header           */
    UINT8               last_cmd[NFC_HAL_SAVED_CMD_SIZE];/* part of last NCI command payload          */
    void                *p_vsc_cback;       /* the callback function for last VSC command             */
} tNFC_HAL_NCIT_CB;

/* Control block for device initialization */
typedef struct
{
    tNFC_HAL_INIT_STATE     initializing_state;     /* state of initializing NFCC               */

    UINT32                  brcm_hw_id;             /* BRCM NFCC HW ID                          */
    tNFC_HAL_DM_CONFIG      next_dm_config;         /* next config in post initialization       */
    UINT8                   next_startup_vsc;       /* next start-up VSC offset in post init    */

    tNFC_HAL_POWER_MODE     power_mode;             /* NFCC power mode                          */
    UINT8                   snooze_mode;            /* current snooze mode                      */
    UINT8                   new_snooze_mode;        /* next snooze mode after receiving cmpl    */
    UINT8                   nfc_wake_active_mode;   /* NFC_HAL_LP_ACTIVE_LOW/HIGH               */
    TIMER_LIST_ENT          lp_timer;               /* timer for low power mode                 */


    tHAL_NFC_STATUS_CBACK   *p_prop_cback;          /* callback to notify complete of proprietary update */
} tNFC_HAL_DEV_CB;

/* data members for NFC_HAL-HCI */
typedef struct
{
    TIMER_LIST_ENT          hci_timer;                /* Timer to avoid indefinitely waiting for response */
    UINT8                   *p_hci_netwk_info_buf;    /* Buffer for reading HCI Network information */
    UINT8                   *p_hci_netwk_dh_info_buf; /* Buffer for reading HCI Network DH information */
    UINT8                   hci_netwk_config_block;   /* Rsp awaiting for hci network configuration block */
    BOOLEAN                 b_wait_hcp_conn_create_rsp; /* Waiting for hcp connection create response */
    BOOLEAN                 b_check_clear_all_pipe_cmd;
    UINT8                   hcp_conn_id;
} tNFC_HAL_HCI_CB;

typedef struct
{
    tHAL_NFC_CBACK          *p_stack_cback;     /* Callback for HAL event notification  */
    tHAL_NFC_DATA_CBACK     *p_data_cback;      /* Callback for data event notification  */

    TIMER_LIST_Q            quick_timer_queue;  /* timer list queue                 */
    TIMER_LIST_ENT          timer;              /* timer for NCI transport task     */

    tNFC_HAL_NCIT_CB        ncit_cb;            /* NCI transport */
    tNFC_HAL_DEV_CB         dev_cb;             /* device initialization */

    /* Patchram control block */
    tNFC_HAL_PRM_CB         prm;
    tNFC_HAL_PRM_I2C_FIX_CB prm_i2c;

    /* data members for NFC_HAL-HCI */
    tNFC_HAL_HCI_CB         hci_cb;


    tNFC_HAL_NCI_CBACK      *p_reinit_cback;
    UINT8                   max_rf_credits;     /* NFC Max RF data credits */
    UINT8                   trace_level;        /* NFC HAL trace level */
} tNFC_HAL_CB;

/* Global NCI data */
#if NFC_DYNAMIC_MEMORY == FALSE
extern tNFC_HAL_CB   nfc_hal_cb;
#else
#define nfc_hal_cb (*nfc_hal_cb_ptr)
extern tNFC_HAL_CB *nfc_hal_cb_ptr;
#endif

/****************************************************************************
** Internal nfc functions
****************************************************************************/

/* From nfc_hal_main.c */
UINT32 nfc_hal_main_task (UINT32 param);
void   nfc_hal_main_init (void);
void   nfc_hal_main_pre_init_done (tHAL_NFC_STATUS);
void   nfc_hal_main_start_quick_timer (TIMER_LIST_ENT *p_tle, UINT16 type, UINT32 timeout);
void   nfc_hal_main_stop_quick_timer (TIMER_LIST_ENT *p_tle);
void   nfc_hal_main_send_error (tHAL_NFC_STATUS status);

/* nfc_hal_nci.c */
BOOLEAN nfc_hal_nci_receive_msg (UINT8 byte);
BOOLEAN nfc_hal_nci_preproc_rx_nci_msg (NFC_HDR *p_msg);
void    nfc_hal_nci_assemble_nci_msg (void);
void    nfc_hal_nci_add_nfc_pkt_type (NFC_HDR *p_msg);
void    nfc_hal_nci_send_cmd (NFC_HDR *p_buf);
void    nfc_hal_nci_cmd_timeout_cback (void *p_tle);

/* nfc_hal_dm.c */
void nfc_hal_dm_init (void);
void nfc_hal_dm_set_xtal_freq_index (void);
void nfc_hal_dm_send_reset_cmd (void);
void nfc_hal_dm_proc_msg_during_init (NFC_HDR *p_msg);
void nfc_hal_dm_config_nfcc (void);
void nfc_hal_dm_send_nci_cmd (const UINT8 *p_data, UINT16 len, tNFC_HAL_NCI_CBACK *p_cback);
void nfc_hal_dm_send_bt_cmd (const UINT8 *p_data, UINT16 len, tNFC_HAL_BTVSC_CPLT_CBACK *p_cback);
void nfc_hal_dm_set_nfc_wake (UINT8 cmd);
void nfc_hal_dm_pre_init_nfcc (void);
void nfc_hal_dm_shutting_down_nfcc (void);
BOOLEAN nfc_hal_dm_power_mode_execute (tNFC_HAL_LP_EVT event);
void nfc_hal_dm_send_pend_cmd (void);

/* nfc_hal_prm.c */
void nfc_hal_prm_spd_reset_ntf (UINT8 reset_reason, UINT8 reset_type);
void nfc_hal_prm_nci_command_complete_cback (tNFC_HAL_NCI_EVT event, UINT16 data_len, UINT8 *p_data);
void nfc_hal_prm_process_timeout (void *p_tle);

/* nfc_hal_hci.c */
void nfc_hal_hci_enable (void);
void nfc_hal_hci_evt_hdlr (tNFC_HAL_HCI_EVENT_DATA *p_evt_data);
void nfc_hal_hci_handle_hci_netwk_info (UINT8 *p_data);
void nfc_hal_hci_handle_hcp_pkt (UINT8 *p_data);
void nfc_hal_hci_timeout_cback (void *p_tle);


/* Define default NCI protocol trace function (if protocol tracing is enabled) */
#if (defined(NFC_HAL_TRACE_PROTOCOL) && (NFC_HAL_TRACE_PROTOCOL == TRUE))
#if !defined (DISP_NCI)
#define DISP_NCI    (DispNci)
void DispNci (UINT8 *p, UINT16 len, BOOLEAN is_recv);
#endif  /* DISP_NCI */

/* For displaying vendor-specific HCI commands */
void DispHciCmd (BT_HDR *p_buf);
void DispHciEvt (BT_HDR *p_buf);
#endif /* NFC_HAL_TRACE_PROTOCOL */

#ifdef __cplusplus
}
#endif

#endif /* NFC_HAL_INT_H */
