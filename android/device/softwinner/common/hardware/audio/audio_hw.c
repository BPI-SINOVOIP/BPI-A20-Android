/*
 * Copyright (C) 2011 The Android Open Source Project
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

#define LOG_TAG "audio_hw_primary"
//#define LOG_NDEBUG 0

#include <errno.h>
#include <pthread.h>
#include <stdint.h>
#include <sys/time.h>
#include <stdlib.h>

#include <cutils/log.h>
#include <cutils/str_parms.h>
#include <cutils/properties.h>

#include <hardware/hardware.h>
#include <system/audio.h>
#include <hardware/audio.h>

#include <tinyalsa/asoundlib.h>
#include <audio_utils/resampler.h>
#include <audio_utils/echo_reference.h>
#include <hardware/audio_effect.h>
#include <audio_effects/effect_aec.h>
#include <fcntl.h>

#include "audio_ril.h"

#include <cutils/properties.h> // for property_get

#define F_LOG ALOGV("%s, line: %d", __FUNCTION__, __LINE__);

#define  CALL_VOLUME_MAX    59
#define  CALL_VOLUME_MIN    0
static int call_volume = CALL_VOLUME_MAX;


#define PRO_AUDIO_MULTI_OUTPUT		"ro.audio.multi.output"
#define PRO_AUDIO_OUTPUT_ACTIVE		"audio.output.active"
#define PRO_AUDIO_INPUT_ACTIVE		"audio.input.active"

/* Mixer control names */
#define MIXER_MASTER_PLAYBACK_VOLUME   		"Master Playback Volume"
#define MIXER_AUDIO_SPEAKER_OUT             "Audio speaker out"

/* ALSA cards for A1X */
#define CARD_A1X_CODEC		0
#define CARD_A1X_HDMI		1
#define CARD_A1X_SPDIF		2
#define CARD_A1X_DEFAULT	CARD_A1X_CODEC

/* ALSA ports for A10 */
#define PORT_CODEC			0
#define PORT_HDMI			0
#define PORT_SPDIF			0
#define PORT_MODEM			0

#define SAMPLING_RATE_8K	8000
#define SAMPLING_RATE_11K	11025
#define SAMPLING_RATE_44K	44100
#define SAMPLING_RATE_48K	48000

/* constraint imposed by ABE: all period sizes must be multiples of 24 */
#define ABE_BASE_FRAME_COUNT 24
/* number of base blocks in a short period (low latency) */
//#define SHORT_PERIOD_MULTIPLIER 44  /* 22 ms */
#define SHORT_PERIOD_MULTIPLIER 55 /* 40 ms */
/* number of frames per short period (low latency) */
//#define SHORT_PERIOD_SIZE (ABE_BASE_FRAME_COUNT * SHORT_PERIOD_MULTIPLIER)
#define SHORT_PERIOD_SIZE 1360
/* number of short periods in a long period (low power) */
//#define LONG_PERIOD_MULTIPLIER 14  /* 308 ms */
#define LONG_PERIOD_MULTIPLIER 6  /* 240 ms */
/* number of frames per long period (low power) */
#define LONG_PERIOD_SIZE (SHORT_PERIOD_SIZE * LONG_PERIOD_MULTIPLIER)
/* number of pseudo periods for playback */
#define PLAYBACK_PERIOD_COUNT 4
/* number of periods for capture */
#define CAPTURE_PERIOD_COUNT 4
/* minimum sleep time in out_write() when write threshold is not reached */
#define MIN_WRITE_SLEEP_US 5000

#define RESAMPLER_BUFFER_FRAMES (SHORT_PERIOD_SIZE * 2)
#define RESAMPLER_BUFFER_SIZE (4 * RESAMPLER_BUFFER_FRAMES)

/* android default out sampling rate*/
#define DEFAULT_OUT_SAMPLING_RATE SAMPLING_RATE_44K

/* audio codec default sampling rate*/
#define MM_SAMPLING_RATE SAMPLING_RATE_44K

/*wifi display buffer size*/
#define AF_BUFFER_SIZE 1024 * 80

#define DISP_CMD_HDMI_GET_HPD_STATUS 0x1c5

enum tty_modes {
    TTY_MODE_OFF,
    TTY_MODE_VCO,
    TTY_MODE_HCO,
    TTY_MODE_FULL
};

struct pcm_config pcm_config_mm_out = {
    .channels = 2,
    .rate = MM_SAMPLING_RATE,
    .period_size = SHORT_PERIOD_SIZE,
    .period_count = PLAYBACK_PERIOD_COUNT,
    .format = PCM_FORMAT_S16_LE,
};

struct pcm_config pcm_config_mm_in = {
    .channels = 2,
    .rate = MM_SAMPLING_RATE,
    .period_size = 1024,
    .period_count = CAPTURE_PERIOD_COUNT,
    .format = PCM_FORMAT_S16_LE,
};

struct route_setting
{
    char *ctl_name;
    int intval;
    char *strval;
};

struct mixer_ctls
{
	struct mixer_ctl *master_playback_volume;
    struct mixer_ctl *audio_speaker_out;
};

struct pcm_buf_manager
{
	pthread_mutex_t lock;       		/* see note below on mutex acquisition order */
	bool 		    BufExist;
    unsigned char   *BufStart;        
    int             BufTotalLen;      
    unsigned char   *BufReadPtr;      
    int             DataLen;          
    unsigned char   *BufWritPtr;      
    int            	BufValideLen;       
    int				SampleRate;
    int				Channel;
};

#define MAX_AUDIO_DEVICES	16

typedef enum e_AUDIO_DEVICE_MANAGEMENT
{
	AUDIO_IN		= 0x01,
	AUDIO_OUT		= 0x02,
}e_AUDIO_DEVICE_MANAGEMENT;

typedef struct sunxi_audio_device_manager {
	char		name[32];
	int			card;
	int			device;
	int			flag_in;			//
	int			flag_in_active;		// 0: do not use, 1: used to caputre or playback
	int			flag_out;
	int			flag_out_active;	// 0: do not use, 1: used to caputre or playback
	bool		flag_exist;			// for hot-plugging
}sunxi_audio_device_manager;

struct sunxi_audio_device {
    struct audio_hw_device hw_device;

    pthread_mutex_t lock;       /* see note below on mutex acquisition order */
    struct mixer *mixer;
    struct mixer_ctls mixer_ctls;
    int mode;
    int devices;
    struct pcm *pcm_modem_dl;
    struct pcm *pcm_modem_ul;
    int in_call;
    float voice_volume;
    struct sunxi_stream_in *active_input;
    struct sunxi_stream_out *active_output;
    bool mic_mute;
    int tty_mode;
    struct echo_reference_itfe *echo_reference;
    bool bluetooth_nrec;
    int wb_amr;
	bool raw_flag;		// flag for raw data

	// add for audio device management
	struct sunxi_audio_device_manager dev_manager[MAX_AUDIO_DEVICES];
	int usb_audio_cnt;
	char in_devices[128], out_devices[128];
	char out_device_active_req[128];
	bool support_multi_ouput;
	bool first_set_audio_routing;
	bool af_capture_flag;
	struct pcm_buf_manager PcmManager;
};

struct sunxi_stream_out {
    struct audio_stream_out stream;

    pthread_mutex_t lock;       /* see note below on mutex acquisition order */
	struct pcm_config config;
	struct pcm_config multi_config[16];
	struct pcm *pcm;
	struct pcm *multi_pcm[16];
	struct resampler_itfe *resampler;
	struct resampler_itfe *multi_resampler[16];
	char *buffer;
    int standby;
    struct echo_reference_itfe *echo_reference;
    struct sunxi_audio_device *dev;
    int write_threshold;
};

#define MAX_PREPROCESSORS 3 /* maximum one AGC + one NS + one AEC per input stream */

struct sunxi_stream_in {
    struct audio_stream_in stream;

    pthread_mutex_t lock;       /* see note below on mutex acquisition order */
    struct pcm_config config;
    struct pcm *pcm;
    int device;
    struct resampler_itfe *resampler;
    struct resampler_buffer_provider buf_provider;
    int16_t *buffer;
    size_t frames_in;
    unsigned int requested_rate;
    int standby;
    int source;
    struct echo_reference_itfe *echo_reference;
    bool need_echo_reference;
    effect_handle_t preprocessors[MAX_PREPROCESSORS];
    int num_preprocessors;
    int16_t *proc_buf;
    size_t proc_buf_size;
    size_t proc_frames_in;
    int16_t *ref_buf;
    size_t ref_buf_size;
    size_t ref_frames_in;
    int read_status;

    struct sunxi_audio_device *dev;
};

#if !LOG_NDEBUG
// for test
static void tinymix_print_enum(struct mixer_ctl *ctl, int print_all)
{
    unsigned int num_enums;
    unsigned int i;
    const char *string;

    num_enums = mixer_ctl_get_num_enums(ctl);

    for (i = 0; i < num_enums; i++) {
        string = mixer_ctl_get_enum_string(ctl, i);
        if (print_all)
            printf("\t%s%s", mixer_ctl_get_value(ctl, 0) == (int)i ? ">" : "",
                   string);
        else if (mixer_ctl_get_value(ctl, 0) == (int)i)
            printf(" %-s", string);
    }
}

static void tinymix_detail_control(struct mixer *mixer, unsigned int id,
                                   int print_all)
{
    struct mixer_ctl *ctl;
    enum mixer_ctl_type type;
    unsigned int num_values;
    unsigned int i;
    int min, max;

    if (id >= mixer_get_num_ctls(mixer)) {
        fprintf(stderr, "Invalid mixer control\n");
        return;
    }

    ctl = mixer_get_ctl(mixer, id);

    type = mixer_ctl_get_type(ctl);
    num_values = mixer_ctl_get_num_values(ctl);

    if (print_all)
        printf("%s:", mixer_ctl_get_name(ctl));

    for (i = 0; i < num_values; i++) {
        switch (type)
        {
        case MIXER_CTL_TYPE_INT:
            printf(" %d", mixer_ctl_get_value(ctl, i));
            break;
        case MIXER_CTL_TYPE_BOOL:
            printf(" %s", mixer_ctl_get_value(ctl, i) ? "On" : "Off");
            break;
        case MIXER_CTL_TYPE_ENUM:
            tinymix_print_enum(ctl, print_all);
            break;
         case MIXER_CTL_TYPE_BYTE:
            printf(" 0x%02x", mixer_ctl_get_value(ctl, i));
            break;
        default:
            printf(" unknown");
            break;
        };
    }

    if (print_all) {
        if (type == MIXER_CTL_TYPE_INT) {
            min = mixer_ctl_get_range_min(ctl);
            max = mixer_ctl_get_range_max(ctl);
            printf(" (range %d->%d)", min, max);
        }
    }
    printf("\n");
}

static void tinymix_list_controls(struct mixer *mixer)
{
    struct mixer_ctl *ctl;
    const char *name, *type;
    unsigned int num_ctls, num_values;
    unsigned int i;

    num_ctls = mixer_get_num_ctls(mixer);

    printf("Number of controls: %d\n", num_ctls);

    printf("ctl\ttype\tnum\t%-40s value\n", "name");
    for (i = 0; i < num_ctls; i++) {
        ctl = mixer_get_ctl(mixer, i);

        name = mixer_ctl_get_name(ctl);
        type = mixer_ctl_get_type_string(ctl);
        num_values = mixer_ctl_get_num_values(ctl);
        printf("%d\t%s\t%d\t%-40s", i, type, num_values, name);
        tinymix_detail_control(mixer, i, 0);
    }
}
#endif

/*wifi display buffer manager*/
static int WritePcmData(void * pInbuf, int inSize, struct pcm_buf_manager *PcmManager)
{
	ALOGV("RequestWriteBuf ++: size: %d", inSize);

	if (PcmManager->BufValideLen< inSize)
	{
		ALOGE("not enough buffer to write");
		return -1;
	}

	pthread_mutex_lock(&PcmManager->lock);

	if ((PcmManager->BufWritPtr + inSize)
		> (PcmManager->BufStart + PcmManager->BufTotalLen))
	{
		int endSize = PcmManager->BufStart + PcmManager->BufTotalLen
			- PcmManager->BufWritPtr;
		memcpy(PcmManager->BufWritPtr, pInbuf, endSize);
		memcpy(PcmManager->BufStart, (void *)((char *)pInbuf + endSize), inSize - endSize);

		PcmManager->BufWritPtr = PcmManager->BufWritPtr
			+ inSize - PcmManager->BufTotalLen;
	}
	else
	{
		memcpy(PcmManager->BufWritPtr, pInbuf, inSize);
		PcmManager->BufWritPtr += inSize;
	}

	PcmManager->BufValideLen -= inSize;
	PcmManager->DataLen += inSize;

	ALOGV("after wr: BufTotalLen: %d, DataLen: %d, BufValideLen: %d, BufReadPtr: %p, BufWritPtr: %p",
		PcmManager->BufTotalLen, PcmManager->DataLen, PcmManager->BufValideLen,
		PcmManager->BufReadPtr, PcmManager->BufWritPtr);

	pthread_mutex_unlock(&PcmManager->lock);
	ALOGV("RequestWriteBuf --");
	return 0;
}

static int ReadPcmData(void *pBuf, int uGetLen, struct pcm_buf_manager *PcmManager)
{
	int underflow = 0, fill_dc_size;
	int size_read = uGetLen;
	int timeout = 0, max_wait_count;

	max_wait_count = uGetLen * 100 / (PcmManager->SampleRate*PcmManager->Channel * 2) + 1; //normal
	max_wait_count *= 2;//twice

	ALOGV("ReadPcmDataForEnc ++, getLen: %d max_wait_count=%d", uGetLen,max_wait_count);
    while(PcmManager->DataLen < uGetLen)
    {
        ALOGV("pcm is not enough for audio encoder! uGetLen: %d, uDataLen: %d\n",
			uGetLen, PcmManager->DataLen);
        usleep(10*1000);
        timeout++;
        if(timeout > max_wait_count) {
        	if (PcmManager->DataLen < uGetLen) {
        		underflow = 1;
        		size_read = PcmManager->DataLen;
        		fill_dc_size = uGetLen - PcmManager->DataLen;
        		ALOGV("fill with dc size:%d",uGetLen - PcmManager->DataLen);
        	}
        	break;
        }
    }

    if((PcmManager->BufReadPtr + size_read)
		> (PcmManager->BufStart + PcmManager->BufTotalLen))
    {
        int len1 = PcmManager->BufStart
			+ PcmManager->BufTotalLen - PcmManager->BufReadPtr;
        memcpy((void *)pBuf, (void *)PcmManager->BufReadPtr, len1);
        memcpy((void *)((char *)pBuf + len1), (void *)PcmManager->BufStart, size_read - len1);
    }
    else
    {
        memcpy(pBuf, PcmManager->BufReadPtr, size_read);
    }

	pthread_mutex_lock(&PcmManager->lock);

    PcmManager->BufReadPtr += size_read;

    if(PcmManager->BufReadPtr
		>= PcmManager->BufStart + PcmManager->BufTotalLen)
    {
        PcmManager->BufReadPtr -= PcmManager->BufTotalLen;
    }
    PcmManager->DataLen -= size_read;
    PcmManager->BufValideLen += size_read;

	ALOGV("after rd: BufTotalLen: %d, DataLen: %d, BufValideLen: %d, pBufReadPtr: %p, pBufWritPtr: %p",
		PcmManager->BufTotalLen, PcmManager->DataLen, PcmManager->BufValideLen,
		PcmManager->BufReadPtr, PcmManager->BufWritPtr);
	
	pthread_mutex_unlock(&PcmManager->lock);
	ALOGV("ReadPcmDataForEnc --");

	if (underflow) {
		char *ptr = (char*)pBuf;
		memset(ptr+size_read, ptr[size_read-1], fill_dc_size);
	}

    return uGetLen;
}

/**
 * NOTE: when multiple mutexes have to be acquired, always respect the following order:
 *        hw device > in stream > out stream
 */

static void select_output_device(struct sunxi_audio_device *adev);
static void select_input_device(struct sunxi_audio_device *adev);
static int adev_set_voice_volume(struct audio_hw_device *dev, float volume);
static int do_input_standby(struct sunxi_stream_in *in);
static int do_output_standby(struct sunxi_stream_out *out);

typedef struct name_map_t
{
	char name_linux[32];
	char name_android[32];
}name_map;

#define AUDIO_MAP_CNT	8
#define AUDIO_NAME_CODEC	"AUDIO_CODEC"
#define AUDIO_NAME_HDMI		"AUDIO_HDMI"
#define AUDIO_NAME_SPDIF	"AUDIO_SPDIF"
#define AUDIO_NAME_I2S		"AUDIO_I2S"

static name_map audio_name_map[AUDIO_MAP_CNT] =
{
	{"audiocodec",		AUDIO_NAME_CODEC},
	{"sndhdmi",			AUDIO_NAME_HDMI},
	{"sndspdif",		AUDIO_NAME_SPDIF},
	{"audiocodec_half",	AUDIO_NAME_CODEC},
	{"sndhdmiraw",		AUDIO_NAME_HDMI},
	{"sndspdifraw",		AUDIO_NAME_SPDIF},
};

static int set_audio_devices_active(struct sunxi_audio_device *adev, int in_out, char * devices);

static int find_name_map(struct sunxi_audio_device *adev, char * in, char * out)
{
	int index = 0;

	if (in == 0 || out == 0)
	{
		ALOGE("error params");
		return -1;
	}

	for (; index < AUDIO_MAP_CNT; index++)
	{
		if (strlen(audio_name_map[index].name_linux) == 0)
		{

			//sprintf(out, "AUDIO_USB%d", adev->usb_audio_cnt++);
			sprintf(out, "AUDIO_USB_%s", in);
			strcpy(audio_name_map[index].name_linux, in);
			strcpy(audio_name_map[index].name_android, out);
			ALOGD("linux name = %s, android name = %s",
				audio_name_map[index].name_linux,
				audio_name_map[index].name_android);
			return 0;
		}

		if (!strcmp(in, audio_name_map[index].name_linux))
		{
			strcpy(out, audio_name_map[index].name_android);
			ALOGD("linux name = %s, android name = %s",
				audio_name_map[index].name_linux,
				audio_name_map[index].name_android);
			return 0;
		}
	}

	return 0;
}

static int do_init_audio_card(struct sunxi_audio_device *adev, int card)
{
	int ret = -1;
	int fd = 0;
	char * snd_path = "/sys/class/sound";
	char snd_card[128], snd_node[128];
	char snd_id[32], snd_name[32];

	memset(snd_card, 0, sizeof(snd_card));
	memset(snd_node, 0, sizeof(snd_node));
	memset(snd_id, 0, sizeof(snd_id));
	memset(snd_name, 0, sizeof(snd_name));

	sprintf(snd_card, "%s/card%d", snd_path, card);
	ret = access(snd_card, F_OK);
	if(ret == 0)
	{
		// id / name
		sprintf(snd_node, "%s/card%d/id", snd_path, card);
		ALOGD("read card %s/card%d/id",snd_path, card);
		fd = open(snd_node, O_RDONLY);
		if (fd > 0)
		{
			ret = read(fd, snd_id, sizeof(snd_id));
			if (ret > 0)
			{
				snd_id[ret - 1] = 0;
				ALOGD("%s, %s, len: %d", snd_node, snd_id, ret);
			}
			close(fd);
		}
		else
		{
			return -1;
		}
		ALOGD("find name map");
		find_name_map(adev, snd_id, snd_name);
		strcpy(adev->dev_manager[card].name, snd_name);

		adev->dev_manager[card].card = card;
		adev->dev_manager[card].device = 0;
		adev->dev_manager[card].flag_exist = true;

		// playback device
		sprintf(snd_node, "%s/card%d/pcmC%dD0p", snd_path, card, card);
		ret = access(snd_node, F_OK);
		if(ret == 0)
		{
			// there is a playback device
			adev->dev_manager[card].flag_out = AUDIO_OUT;
			adev->dev_manager[card].flag_out_active = 0;
		}

		// capture device
		sprintf(snd_node, "%s/card%d/pcmC%dD0c", snd_path, card, card);
		ret = access(snd_node, F_OK);
		if(ret == 0)
		{
			// there is a capture device
			adev->dev_manager[card].flag_in = AUDIO_IN;
			adev->dev_manager[card].flag_in_active = 0;
		}
	}
	else
	{
		return -1;
	}

	return 0;
}

static void init_audio_devices(struct sunxi_audio_device *adev)
{
	int card = 0;

	F_LOG;

	memset(adev->dev_manager, 0, sizeof(adev->dev_manager));

	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (do_init_audio_card(adev, card) == 0)
		{
			// break;
			ALOGV("card: %d, name: %s, capture: %d, playback: %d",
				card, adev->dev_manager[card].name,
				adev->dev_manager[card].flag_in == AUDIO_IN,
				adev->dev_manager[card].flag_out == AUDIO_OUT);
		}
	}
}

static void init_audio_devices_active(struct sunxi_audio_device *adev)
{
	int card = 0;
	int flag_active = 0;
	char * active_name_in;
	char * active_name_out;

	F_LOG;

	// high priority, due to the proprety
	char prop_value_in[128];
	int ret = property_get(PRO_AUDIO_INPUT_ACTIVE, prop_value_in, "");
	if (ret > 0)
	{
		ALOGV("init_audio_devices_active: get property %s: %s", PRO_AUDIO_INPUT_ACTIVE, prop_value_in);
		if (set_audio_devices_active(adev, AUDIO_IN, prop_value_in) == 0)
		{
			active_name_in = prop_value_in;
			flag_active |= AUDIO_IN;
		}
	}
	else
	{
		ALOGV("init_audio_devices_active: get property %s failed, %s", PRO_AUDIO_INPUT_ACTIVE, strerror(errno));
	}

	char prop_value_out[128];
	ret = property_get(PRO_AUDIO_OUTPUT_ACTIVE, prop_value_out, "");
	if (ret > 0)
	{
		ALOGV("init_audio_devices_active: get property %s: %s", PRO_AUDIO_OUTPUT_ACTIVE, prop_value_out);
		if (set_audio_devices_active(adev, AUDIO_OUT, prop_value_out) == 0)
		{
			active_name_out = prop_value_out;
			flag_active |= AUDIO_OUT;
		}
	}
	else
	{
		ALOGV("init_audio_devices_active: get property %s failed, %s", PRO_AUDIO_OUTPUT_ACTIVE, strerror(errno));
	}

	if ((flag_active & AUDIO_IN)
		&& (flag_active & AUDIO_OUT))
	{
		goto INIT_END;
	}

	// midle priority, use codec
	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		// default use auido codec in/out
		if (adev->dev_manager[card].flag_exist
			&& (!strcmp(adev->dev_manager[card].name, AUDIO_NAME_CODEC)))
		{
			if (!(flag_active & AUDIO_IN)
				&& (adev->dev_manager[card].flag_in == AUDIO_IN))
			{
				ALOGV("OK, default use %s capture", adev->dev_manager[card].name);
				active_name_in = adev->dev_manager[card].name;
				adev->dev_manager[card].flag_in_active = 1;
				flag_active |= AUDIO_IN;
			}
			if (!(flag_active & AUDIO_OUT)
				&& (adev->dev_manager[card].flag_out == AUDIO_OUT))
			{
				ALOGV("OK, default use %s playback", adev->dev_manager[card].name);
				active_name_out = adev->dev_manager[card].name;
				adev->dev_manager[card].flag_out_active = 1;
				flag_active |= AUDIO_OUT;
			}

			break;
		}
	}

	if ((flag_active & AUDIO_IN)
		&& (flag_active & AUDIO_OUT))
	{
		goto INIT_END;
	}

	// low priority, chose any device
	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (!adev->dev_manager[card].flag_exist)
		{
			break;
		}

		// there is no auido codec in
		if (!(flag_active & AUDIO_IN))
		{
			if (adev->dev_manager[card].flag_in == AUDIO_IN)
			{
				ALOGV("OK, default use %s capture", adev->dev_manager[card].name);
				active_name_in = adev->dev_manager[card].name;
				adev->dev_manager[card].flag_in_active = 1;
				flag_active |= AUDIO_IN;
			}
		}

		// there is no auido codec out
		if (!(flag_active & AUDIO_OUT))
		{
			if (adev->dev_manager[card].flag_out == AUDIO_OUT)
			{
				ALOGV("OK, default use %s playback", adev->dev_manager[card].name);
				active_name_out = adev->dev_manager[card].name;
				adev->dev_manager[card].flag_out_active = 1;
				flag_active |= AUDIO_OUT;
			}
		}
	}

INIT_END:

	if (flag_active & AUDIO_IN)
	{
		if (active_name_in)
		{
			adev->devices |= AUDIO_DEVICE_IN_BUILTIN_MIC;
		}
	}
	else
	{
		ALOGW("there is not a audio capture devices");
	}

	if (flag_active & AUDIO_OUT)
	{
		if (active_name_out)
		{
			if (strstr(active_name_out, AUDIO_NAME_CODEC))
			{
				adev->devices |= AUDIO_DEVICE_OUT_SPEAKER;
			}
			if (strstr(active_name_out, AUDIO_NAME_SPDIF))
			{
				adev->devices |= AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET;
			}
			if (strstr(active_name_out, AUDIO_NAME_HDMI))
			{
				adev->devices |= AUDIO_DEVICE_OUT_AUX_DIGITAL;
			}
		}
	}
	else
	{
		ALOGW("there is not a audio playback devices");
	}

	ALOGV("OK, default adev->devices: %08x", adev->devices);
}

static int updata_audio_devices(struct sunxi_audio_device *adev)
{
	int card = 0;
	int ret = -1;
	int fd = 0;
	char * snd_path = "/sys/class/sound";
	char snd_card[128];

	memset(snd_card, 0, sizeof(snd_card));

	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		sprintf(snd_card, "%s/card%d", snd_path, card);
		ret = access(snd_card, F_OK);
		if(ret == 0)
		{
			if (adev->dev_manager[card].flag_exist == true)
			{
				continue;		// no changes
			}
			else				// plug-in
			{
				ALOGD("do init audio card");
				do_init_audio_card(adev, card);
			}
		}
		else
		{
			if (adev->dev_manager[card].flag_exist == false)
			{
				continue;		// no changes
			}
			else				// plug-out
			{
				adev->dev_manager[card].flag_exist = false;
				adev->dev_manager[card].flag_in = 0;
				adev->dev_manager[card].flag_out = 0;
			}
		}
	}

	return 0;
}

static char * get_audio_devices(struct sunxi_audio_device *adev, int in_out)
{
	char * in_devices = adev->in_devices;
	char * out_devices = adev->out_devices;

	updata_audio_devices(adev);

	memset(in_devices, 0, sizeof(adev->in_devices));
	memset(out_devices, 0, sizeof(adev->out_devices));

	ALOGD("getAudioDevices()");
	int card = 0;
	for(card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (adev->dev_manager[card].flag_exist == true)
		{
			if (adev->dev_manager[card].flag_in == AUDIO_IN)
			{
				strcat(in_devices, adev->dev_manager[card].name);
				strcat(in_devices, ",");
				ALOGD("in dev:%s",adev->dev_manager[card].name);
			}

			if (adev->dev_manager[card].flag_out == AUDIO_OUT)
			{
				strcat(out_devices, adev->dev_manager[card].name);
				strcat(out_devices, ",");
				ALOGD("out dev:%s",adev->dev_manager[card].name);
			}
		}
	}

	in_devices[strlen(in_devices) - 1] = 0;
	out_devices[strlen(out_devices) - 1] = 0;

	//
	if (in_out & AUDIO_IN)
	{
		ALOGD("in capture: %s",in_devices);
		return in_devices;
	}
	else if(in_out & AUDIO_OUT)
	{
		ALOGD("out playback: %s",out_devices);
		return out_devices;
	}
	else
	{
		ALOGE("unknown in/out flag");
		return 0;
	}
}

static int set_audio_devices_active_internal(struct sunxi_stream_out *stream, int in_out, int value)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;
    struct sunxi_audio_device *adev = out->dev;
	int card = 0;
	char devices[128];
	int ret = -1;

	F_LOG;

	switch(value & AUDIO_DEVICE_OUT_ALL) {
        case AUDIO_DEVICE_OUT_EARPIECE:
        case AUDIO_DEVICE_OUT_SPEAKER:
		case AUDIO_DEVICE_OUT_WIRED_HEADSET:
        case AUDIO_DEVICE_OUT_WIRED_HEADPHONE:
			// codec
			strcpy(devices, AUDIO_NAME_CODEC);
            break;
        case AUDIO_DEVICE_OUT_AUX_DIGITAL:
			// hdmi
			strcpy(devices, AUDIO_NAME_HDMI);
            break;
        case AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET:
			// spdif
			strcpy(devices, AUDIO_NAME_SPDIF);
            break;
        default:
			//do nothing
            // codec
			//strcpy(devices, AUDIO_NAME_CODEC);
			return 0;
	}

	if (in_out & AUDIO_OUT)
	{
		char prop_value_out[128];
		ret = property_get(PRO_AUDIO_OUTPUT_ACTIVE, prop_value_out, "");
		int odev = value & AUDIO_DEVICE_OUT_ALL;
		if ((ret > 0)
			&& adev->support_multi_ouput
			/*&& (strlen(adev->out_device_active_req) > 0)*/)
		{
			// support multi-audio devices output at the same time
			ALOGV("get property %s: %s", PRO_AUDIO_OUTPUT_ACTIVE, prop_value_out);
			if(odev == AUDIO_DEVICE_OUT_WIRED_HEADPHONE
				|| odev == AUDIO_DEVICE_OUT_AUX_DIGITAL)
			{
				//deal with:headphone plugin or HDMI plugin
				ALOGV("just change to this audio device:%d", odev);
			}
			else if (!strstr(prop_value_out, devices))
			{

				strcat(devices, ",");
				strcat(devices, prop_value_out);

			}
			else
			{
				strcpy(devices, prop_value_out);
			}
		}
		if (strstr(prop_value_out, AUDIO_NAME_HDMI))
		{
			do_output_standby(out);
		}
	}
	set_audio_devices_active(adev, in_out, devices);

	return 0;
}

static int set_audio_devices_active(struct sunxi_audio_device *adev, int in_out, char * devices)
{
	int card = 0, i = 0;
	char name[8][32];
	int cnt = 0;
	char str[128];
	int ret = -1;

	strcpy(str, devices);
	char *pval = str;

	if (pval == NULL)
	{
		return -1;
	}

	if (in_out & AUDIO_IN)
	{
		ret = property_set(PRO_AUDIO_INPUT_ACTIVE, devices);
		if (ret < 0)
		{
			ALOGE("set property %s: %s failed", PRO_AUDIO_INPUT_ACTIVE, devices);
		}
		else
		{
			ALOGV("set property %s: %s ok", PRO_AUDIO_INPUT_ACTIVE, devices);
		}
	}

	if (in_out & AUDIO_OUT)
	{
		ret = property_set(PRO_AUDIO_OUTPUT_ACTIVE, devices);
		if (ret < 0)
		{
			ALOGE("set property %s: %s failed", PRO_AUDIO_OUTPUT_ACTIVE, devices);
		}
		else
		{
			ALOGV("set property %s: %s ok", PRO_AUDIO_OUTPUT_ACTIVE, devices);
		}
	}

	char *seps = " ,";
	pval = strtok(pval, seps);
	while (pval != NULL)
	{
		strcpy(name[cnt++], pval);
		pval = strtok(NULL, seps);
	}

	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (in_out & AUDIO_IN)
		{
			adev->dev_manager[card].flag_in_active = 0;
		}
		else
		{
			adev->dev_manager[card].flag_out_active = 0;
		}
	}

	for (i = 0; i < cnt; i++)
	{
		for (card = 0; card < MAX_AUDIO_DEVICES; card++)
		{
			if (in_out & AUDIO_IN)
			{
				if ((adev->dev_manager[card].flag_in == in_out)
					&& (strcmp(adev->dev_manager[card].name, name[i]) == 0))
				{
					ALOGV("%s %s device will be active", name[i], "input");
					adev->dev_manager[card].flag_in_active = 1;
					// only one capture device can be active
					return 0;
				}
			}
			else
			{
				if ((adev->dev_manager[card].flag_out == in_out)
					&& (strcmp(adev->dev_manager[card].name, name[i]) == 0))
				{
					ALOGV("%s %s device will be active", name[i], "output");
					adev->dev_manager[card].flag_out_active = 1;
					break;
				}
			}
		}

		if (card == MAX_AUDIO_DEVICES)
		{
			if (in_out & AUDIO_IN)
			{
			    ALOGE("can not set %s %s active", name[i], (in_out & AUDIO_IN) ? "input" : "ouput");
			    adev->dev_manager[0].flag_in_active = 1;
			    ALOGE("but device %s %s will be active", adev->dev_manager[0].name, (in_out & AUDIO_IN) ? "input" : "ouput");
				return 0;
			}
			else
			{
			    ALOGE("can not set %s %s active", name[i], (in_out & AUDIO_IN) ? "input" : "ouput");
				return -1;
			}
			return -1;
		}
	}

	return 0;
}

static int get_audio_devices_active(struct sunxi_audio_device *adev, int in_out, char * devices)
{
	int card = 0, i = 0;
	int flag_in_out = -1;

	if (devices == 0)
	{
		return -1;
	}

	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (in_out & AUDIO_IN)
		{
			if ((adev->dev_manager[card].flag_in == in_out)
				&& (adev->dev_manager[card].flag_in_active == 1))
			{
				strcat(devices, adev->dev_manager[card].name);
				strcat(devices, ",");
			}
		}
		else
		{
			if ((adev->dev_manager[card].flag_out == in_out)
				&& (adev->dev_manager[card].flag_out_active == 1))
			{
				strcat(devices, adev->dev_manager[card].name);
				strcat(devices, ",");
			}
		}
	}

	devices[strlen(devices) - 1] = 0;

	ALOGD("get_audio_devices_active: %s", devices);

	return 0;
}
/* The enable flag when 0 makes the assumption that enums are disabled by
 * "Off" and integers/booleans by 0 */
static int set_route_by_array(struct mixer *mixer, struct route_setting *route,
                              int enable)
{
    struct mixer_ctl *ctl;
    unsigned int i, j;

    /* Go through the route array and set each value */
    i = 0;
    while (route[i].ctl_name) {
        ctl = mixer_get_ctl_by_name(mixer, route[i].ctl_name);
        if (!ctl)
            return -EINVAL;

        if (route[i].strval) {
            if (enable)
                mixer_ctl_set_enum_by_string(ctl, route[i].strval);
            else
                mixer_ctl_set_enum_by_string(ctl, "Off");
        } else {
            /* This ensures multiple (i.e. stereo) values are set jointly */
            for (j = 0; j < mixer_ctl_get_num_values(ctl); j++) {
                if (enable)
                    mixer_ctl_set_value(ctl, j, route[i].intval);
                else
                    mixer_ctl_set_value(ctl, j, 0);
            }
        }
        i++;
    }

    return 0;
}

static int start_call(struct sunxi_audio_device *adev)
{
    F_LOG;

//	set_route_by_array(adev->mixer, mic1_up_routing, 1);
//	set_route_by_array(adev->mixer, line_in_routing, 1);
//	mixer_ctl_set_value(adev->mixer_ctls.playback_pamute_switch, 0, 1);		// in call mode must switch pa unmute

	ril_set_call_volume(0, 1);

//	mixer_ctl_set_value(adev->mixer_ctls.master_playback_volume, 0, (call_volume ? call_volume : CALL_VOLUME_MIN));

	return 0;
}

static void end_call(struct sunxi_audio_device *adev)
{
    F_LOG;
	
//	mixer_ctl_set_value(adev->mixer_ctls.playback_pamute_switch, 0, 0);
    mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 0);
	usleep(5000);
//	set_route_by_array(adev->mixer, mic1_up_routing, 0);
//	set_route_by_array(adev->mixer, line_in_routing, 0);
	
	ril_set_call_audio_path(SOUND_AUDIO_PATH_SPEAKER);

	mixer_ctl_set_value(adev->mixer_ctls.master_playback_volume, 0, CALL_VOLUME_MAX);
}

static void set_incall_device(struct sunxi_audio_device *adev)
{
    int device_type;

	F_LOG;

    switch(adev->devices & AUDIO_DEVICE_OUT_ALL) {
        case AUDIO_DEVICE_OUT_EARPIECE:
            device_type = SOUND_AUDIO_PATH_HANDSET;
            break;
        case AUDIO_DEVICE_OUT_SPEAKER:
        case AUDIO_DEVICE_OUT_AUX_DIGITAL:
            device_type = SOUND_AUDIO_PATH_SPEAKER;
            break;
        case AUDIO_DEVICE_OUT_WIRED_HEADSET:
            device_type = SOUND_AUDIO_PATH_HEADSET;
            break;
        case AUDIO_DEVICE_OUT_WIRED_HEADPHONE:
            device_type = SOUND_AUDIO_PATH_HEADPHONE;
            break;
        case AUDIO_DEVICE_OUT_BLUETOOTH_SCO:
        case AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET:
        case AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT:
            if (adev->bluetooth_nrec)
                device_type = SOUND_AUDIO_PATH_BLUETOOTH;
            else
                device_type = SOUND_AUDIO_PATH_BLUETOOTH_NO_NR;
            break;
        default:
            device_type = SOUND_AUDIO_PATH_HANDSET;
            break;
    }

	ril_set_call_audio_path(device_type);
}

static void force_all_standby(struct sunxi_audio_device *adev)
{
    struct sunxi_stream_in *in;
    struct sunxi_stream_out *out;

    if (adev->active_output) {
        out = adev->active_output;
        pthread_mutex_lock(&out->lock);
        do_output_standby(out);
        pthread_mutex_unlock(&out->lock);
    }

    if (adev->active_input) {
        in = adev->active_input;
        pthread_mutex_lock(&in->lock);
        do_input_standby(in);
        pthread_mutex_unlock(&in->lock);
    }
}

static void select_mode(struct sunxi_audio_device *adev)
{
    if (adev->mode == AUDIO_MODE_IN_CALL) {
        ALOGV("Entering IN_CALL state, in_call=%d", adev->in_call);
        if (!adev->in_call) {
            force_all_standby(adev);
            /* force earpiece route for in call state if speaker is the
            only currently selected route. This prevents having to tear
            down the modem PCMs to change route from speaker to earpiece
            after the ringtone is played, but doesn't cause a route
            change if a headset or bt device is already connected. If
            speaker is not the only thing active, just remove it from
            the route. We'll assume it'll never be used initally during
            a call. This works because we're sure that the audio policy
            manager will update the output device after the audio mode
            change, even if the device selection did not change. */
            if ((adev->devices & AUDIO_DEVICE_OUT_ALL) == AUDIO_DEVICE_OUT_SPEAKER)
                adev->devices = AUDIO_DEVICE_OUT_EARPIECE |
                                AUDIO_DEVICE_IN_BUILTIN_MIC;
            else
                adev->devices &= ~AUDIO_DEVICE_OUT_SPEAKER;
            select_output_device(adev);
            start_call(adev);

            adev_set_voice_volume(&adev->hw_device, adev->voice_volume);
            adev->in_call = 1;
        }
    } else {
        ALOGV("Leaving IN_CALL state, in_call=%d, mode=%d",
             adev->in_call, adev->mode);
        if (adev->in_call) {
            adev->in_call = 0;
            end_call(adev);
            force_all_standby(adev);
            select_output_device(adev);
            select_input_device(adev);
        }
    }
}

static void select_output_device(struct sunxi_audio_device *adev)
{
	int ret = -1;
    int headset_on;
    int headphone_on;
    int speaker_on;
    int earpiece_on;
    int bt_on;

    headset_on = adev->devices & AUDIO_DEVICE_OUT_WIRED_HEADSET;
    headphone_on = adev->devices & AUDIO_DEVICE_OUT_WIRED_HEADPHONE;
    speaker_on = adev->devices & AUDIO_DEVICE_OUT_SPEAKER;
    earpiece_on = adev->devices & AUDIO_DEVICE_OUT_EARPIECE;
    bt_on = adev->devices & AUDIO_DEVICE_OUT_ALL_SCO;

	ALOGV("select_output_device, devices: %x, mode: %x", adev->devices, adev->mode);

	int pa_should_on = speaker_on;

	char prop_value[16];
	ret = property_get("audio.without.earpiece", prop_value, "");
	if (ret > 0)
	{
		ALOGD("get property audio.without.earpiece: %s", prop_value);
		if (strcmp(prop_value, "true") == 0)
		{
			pa_should_on |= earpiece_on;
		}
	}

    // mute/unmute speaker
	if (adev->mode == AUDIO_MODE_IN_CALL) 
	{		
		if(pa_should_on)
		{
			//mixer_ctl_set_value(adev->mixer_ctls.audio_earpiece_out, 0, 0);
			//mixer_ctl_set_value(adev->mixer_ctls.audio_headphone_out, 0, 0);
			mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 1);
            ALOGV("****LINE:%d,FUNC:%s",__LINE__,__FUNCTION__);
		}
		else
		{
			if(earpiece_on)
			{
                //mixer_ctl_set_value(adev->mixer_ctls.audio_earpiece_out, 0, 1);
                //mixer_ctl_set_value(adev->mixer_ctls.audio_headphone_out, 0, 0);
				mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 0);
                ALOGV("****LINE:%d,FUNC:%s",__LINE__,__FUNCTION__);
			}
			else
			{
				//mixer_ctl_set_value(adev->mixer_ctls.audio_earpiece_out, 0, 0);
				//mixer_ctl_set_value(adev->mixer_ctls.audio_headphone_out, 0, 1)
				mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 0);
				ALOGV("****LINE:%d,FUNC:%s",__LINE__,__FUNCTION__);
			}
		}
		set_incall_device(adev);
	}
	else
	{
		//mixer_ctl_set_value(adev->mixer_ctls.audio_spk_switch, 0, (pa_should_on ? 1 : 0));
		if (pa_should_on)
		{
            mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 1);
            ALOGV("****LINE:%d,FUNC:%s",__LINE__,__FUNCTION__);
        }
        else
        {
            mixer_ctl_set_value(adev->mixer_ctls.audio_speaker_out, 0, 0);
            ALOGV("****LINE:%d,FUNC:%s",__LINE__,__FUNCTION__);
        }
	}
}

static void select_input_device(struct sunxi_audio_device *adev)
{
    int headset_on = 0;
    int main_mic_on = 0;
    int sub_mic_on = 0;
    int bt_on = adev->devices & AUDIO_DEVICE_IN_ALL_SCO;

    if (!bt_on) {
        if ((adev->mode != AUDIO_MODE_IN_CALL) && (adev->active_input != 0)) {
            /* sub mic is used for camcorder or VoIP on speaker phone */
            sub_mic_on = (adev->active_input->source == AUDIO_SOURCE_CAMCORDER) ||
                         ((adev->devices & AUDIO_DEVICE_OUT_SPEAKER) &&
                          (adev->active_input->source == AUDIO_SOURCE_VOICE_COMMUNICATION));
        }
        if (!sub_mic_on) {
            headset_on = adev->devices & AUDIO_DEVICE_IN_WIRED_HEADSET;
            main_mic_on = adev->devices & AUDIO_DEVICE_IN_BUILTIN_MIC;
        }
    }
}

/* must be called with hw device and output stream mutexes locked */
static int start_output_stream(struct sunxi_stream_out *out)
{
	F_LOG;
    struct sunxi_audio_device *adev = out->dev;
    unsigned int card = CARD_A1X_DEFAULT;
    unsigned int port = PORT_CODEC;
	unsigned int index;

	if (adev->mode == AUDIO_MODE_IN_CALL)
	{
		ALOGW("mode in call, do not start stream");
		return 0;
	}

	if (adev->raw_flag)
	{
		return 0;
	}

	int device = adev->devices;
	char prop_value[512];
    int ret = property_get("audio.routing", prop_value, "");
	if (ret > 0)
	{
	    if(atoi(prop_value) == AUDIO_DEVICE_OUT_SPEAKER)
	    {
			ALOGD("start_output_stream, AUDIO_DEVICE_OUT_SPEAKER");
			device = AUDIO_DEVICE_OUT_SPEAKER;
		}
		else if(atoi(prop_value) == AUDIO_DEVICE_OUT_AUX_DIGITAL)
		{
			ALOGD("start_output_stream AUDIO_DEVICE_OUT_AUX_DIGITAL");
			device = AUDIO_DEVICE_OUT_AUX_DIGITAL;
		}
		else if(atoi(prop_value) == AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET)
		{
			ALOGD("start_output_stream AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET");
			device = AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET;
		}
		else
		{
			ALOGW("unknown audio.routing : %s", prop_value);
		}
	}
	else
	{
		// ALOGW("get audio.routing failed");
	}

	adev->devices = device;

    adev->active_output = out;

    if (adev->mode != AUDIO_MODE_IN_CALL) {
        /* FIXME: only works if only one output can be active at a time */
        select_output_device(adev);
    }
    /* S/PDIF takes priority over HDMI audio. In the case of multiple
     * devices, this will cause use of S/PDIF or HDMI only */
    out->config.rate = MM_SAMPLING_RATE;
    if (adev->devices & AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET) {
		card = CARD_A1X_SPDIF;
        port = PORT_SPDIF;
    }
    else if(adev->devices & AUDIO_DEVICE_OUT_AUX_DIGITAL) {
        card = CARD_A1X_HDMI;
        port = PORT_HDMI;
        out->config.rate = MM_SAMPLING_RATE;
    }
    /* default to low power: will be corrected in out_write if necessary before first write to
     * tinyalsa.
     */
    out->write_threshold = PLAYBACK_PERIOD_COUNT * LONG_PERIOD_SIZE;
    out->config.start_threshold = SHORT_PERIOD_SIZE * 2;
    out->config.avail_min = LONG_PERIOD_SIZE;
			
	for (index = 0; index < MAX_AUDIO_DEVICES; index++)
	{
		if (adev->dev_manager[index].flag_exist
			&& (adev->dev_manager[index].flag_out == AUDIO_OUT)
			&& adev->dev_manager[index].flag_out_active)
		{
			card = index;
			ALOGV("use %s to playback audio", adev->dev_manager[index].name);

			out->multi_config[card] = pcm_config_mm_out;
			out->multi_config[card].rate = MM_SAMPLING_RATE;
	    	out->multi_config[card].start_threshold = SHORT_PERIOD_SIZE * 2;
    		out->multi_config[card].avail_min = LONG_PERIOD_SIZE;

			if (strncmp(adev->dev_manager[index].name, "AUDIO_USB", 9) == 0){
				out->multi_pcm[card] = pcm_open_req(card, port, PCM_OUT | PCM_MMAP | PCM_NOIRQ, &out->multi_config[card], DEFAULT_OUT_SAMPLING_RATE);
			}else{
				out->multi_config[card].period_size = 4096;//256;//512;//256;
				out->multi_config[card].period_count = 1;//8;//4;//8;
				out->multi_pcm[card] = pcm_open(card, port, PCM_OUT, &out->multi_config[card]);
			}

			if (!pcm_is_ready(out->multi_pcm[card])) {
        		ALOGE("cannot open pcm driver: %s", pcm_get_error(out->multi_pcm[card]));
        		pcm_close(out->multi_pcm[card]);
				out->multi_pcm[card] = NULL;
        		adev->active_output = NULL;
        		return -ENOMEM;
    		}

		    if (adev->echo_reference != NULL)
		        out->echo_reference = adev->echo_reference;

			if (DEFAULT_OUT_SAMPLING_RATE != out->multi_config[card].rate)
			{
				int ret = create_resampler(DEFAULT_OUT_SAMPLING_RATE,
						   					out->multi_config[card].rate,
						   					2,
						   					RESAMPLER_QUALITY_DEFAULT,
						   					NULL,
						   					&out->multi_resampler[card]);
				if (ret != 0)
				{
					ALOGE("create out resampler failed, %d -> %d", DEFAULT_OUT_SAMPLING_RATE, out->multi_config[card].rate);
					return ret;
				}

				ALOGV("create out resampler OK, %d -> %d", DEFAULT_OUT_SAMPLING_RATE, out->multi_config[card].rate);
			}
			else
			{
				ALOGV("do not use out resampler");
			}

			if (out->multi_resampler[card])
			{
	    		out->multi_resampler[card]->reset(out->multi_resampler[card]);
			}

		}
	}

    return 0;
}

static int check_input_parameters(uint32_t sample_rate, int format, int channel_count)
{
    if (format != AUDIO_FORMAT_PCM_16_BIT)
        return -EINVAL;

    if ((channel_count < 1) || (channel_count > 2))
        return -EINVAL;

    switch(sample_rate) {
    case 8000:
    case 11025:
    case 16000:
    case 22050:
    case 24000:
    case 32000:
    case 44100:
    case 48000:
        break;
    default:
        return -EINVAL;
    }

    return 0;
}

static size_t get_input_buffer_size(uint32_t sample_rate, int format, int channel_count)
{
    size_t size;
    size_t device_rate;

    if (check_input_parameters(sample_rate, format, channel_count) != 0)
        return 0;

    /* take resampling into account and return the closest majoring
    multiple of 16 frames, as audioflinger expects audio buffers to
    be a multiple of 16 frames */
    size = (pcm_config_mm_in.period_size * sample_rate) / pcm_config_mm_in.rate;
    size = ((size + 15) / 16) * 16;

    return size * channel_count * sizeof(short);
}

static void add_echo_reference(struct sunxi_stream_out *out,
                               struct echo_reference_itfe *reference)
{
    pthread_mutex_lock(&out->lock);
    out->echo_reference = reference;
    pthread_mutex_unlock(&out->lock);
}

static void remove_echo_reference(struct sunxi_stream_out *out,
                                  struct echo_reference_itfe *reference)
{
    pthread_mutex_lock(&out->lock);
    if (out->echo_reference == reference) {
        /* stop writing to echo reference */
        reference->write(reference, NULL);
        out->echo_reference = NULL;
    }
    pthread_mutex_unlock(&out->lock);
}

static void put_echo_reference(struct sunxi_audio_device *adev,
                          struct echo_reference_itfe *reference)
{
    if (adev->echo_reference != NULL &&
            reference == adev->echo_reference) {
        if (adev->active_output != NULL)
            remove_echo_reference(adev->active_output, reference);
        release_echo_reference(reference);
        adev->echo_reference = NULL;
    }
}

static struct echo_reference_itfe *get_echo_reference(struct sunxi_audio_device *adev,
                                               audio_format_t format,
                                               uint32_t channel_count,
                                               uint32_t sampling_rate)
{
    put_echo_reference(adev, adev->echo_reference);
    if (adev->active_output != NULL) {
        struct audio_stream *stream = &adev->active_output->stream.common;
        uint32_t wr_channel_count = popcount(stream->get_channels(stream));
        uint32_t wr_sampling_rate = stream->get_sample_rate(stream);

        int status = create_echo_reference(AUDIO_FORMAT_PCM_16_BIT,
                                           channel_count,
                                           sampling_rate,
                                           AUDIO_FORMAT_PCM_16_BIT,
                                           wr_channel_count,
                                           wr_sampling_rate,
                                           &adev->echo_reference);
        if (status == 0)
            add_echo_reference(adev->active_output, adev->echo_reference);
    }
    return adev->echo_reference;
}

static int get_playback_delay(struct sunxi_stream_out *out,
                       size_t frames,
                       struct echo_reference_buffer *buffer)
{
	struct sunxi_audio_device *adev = out->dev;
    size_t kernel_frames;
    int status;
	int index;
	int card;

	for (index = 0; index < MAX_AUDIO_DEVICES; index++)
	{
		if (adev->dev_manager[index].flag_exist
			&& (adev->dev_manager[index].flag_out == AUDIO_OUT)
			&& adev->dev_manager[index].flag_out_active)
		{
			card = index;

			status = pcm_get_htimestamp(out->multi_pcm[card], &kernel_frames, &buffer->time_stamp);
    		if (status < 0) {
        		buffer->time_stamp.tv_sec  = 0;
        		buffer->time_stamp.tv_nsec = 0;
        		buffer->delay_ns           = 0;
        		ALOGV("get_playback_delay(): pcm_get_htimestamp error,"
               			"setting playbackTimestamp to 0");
        		return status;
    		}

	   		kernel_frames = pcm_get_buffer_size(out->multi_pcm[card]) - kernel_frames;
			break;
		}

	}

    /* adjust render time stamp with delay added by current driver buffer.
     * Add the duration of current frame as we want the render time of the last
     * sample being written. */
    buffer->delay_ns = (long)(((int64_t)(kernel_frames + frames)* 1000000000)/
                            MM_SAMPLING_RATE);

    return 0;
}

static uint32_t out_get_sample_rate(const struct audio_stream *stream)
{
    return DEFAULT_OUT_SAMPLING_RATE;
}

static int out_set_sample_rate(struct audio_stream *stream, uint32_t rate)
{
    return 0;
}

static size_t out_get_buffer_size(const struct audio_stream *stream)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;

    /* take resampling into account and return the closest majoring
    multiple of 16 frames, as audioflinger expects audio buffers to
    be a multiple of 16 frames */
    size_t size = (SHORT_PERIOD_SIZE * DEFAULT_OUT_SAMPLING_RATE) / out->config.rate;
    size = ((size + 15) / 16) * 16;
    return size * audio_stream_frame_size((struct audio_stream *)stream);
}

static uint32_t out_get_channels(const struct audio_stream *stream)
{
    return AUDIO_CHANNEL_OUT_STEREO;
}

static audio_format_t out_get_format(const struct audio_stream *stream)
{
    return AUDIO_FORMAT_PCM_16_BIT;
}

static int out_set_format(struct audio_stream *stream, audio_format_t format)
{
    return 0;
}

/* must be called with hw device and output stream mutexes locked */
static int do_output_standby(struct sunxi_stream_out *out)
{
    struct sunxi_audio_device *adev = out->dev;
	int index = 0;

    if (!out->standby) {
		if (out->pcm)
		{
			pcm_close(out->pcm);
        	out->pcm = NULL;
		}

		if (out->resampler)
		{
			release_resampler(out->resampler);
			out->resampler = NULL;
		}

        for (index = 0; index < MAX_AUDIO_DEVICES; index++)
        {
        	if (out->multi_pcm[index])
        	{
				pcm_close(out->multi_pcm[index]);
        		out->multi_pcm[index] = NULL;
			}

			if (out->multi_resampler[index])
			{
				release_resampler(out->multi_resampler[index]);
				out->multi_resampler[index] = NULL;
			}
        }

        adev->active_output = 0;

        /* stop writing to echo reference */
        if (out->echo_reference != NULL) {
            out->echo_reference->write(out->echo_reference, NULL);
            out->echo_reference = NULL;
        }

        out->standby = 1;
    }
    return 0;
}

static int out_standby(struct audio_stream *stream)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;
    int status;

    pthread_mutex_lock(&out->dev->lock);
    pthread_mutex_lock(&out->lock);
    status = do_output_standby(out);
    pthread_mutex_unlock(&out->lock);
    pthread_mutex_unlock(&out->dev->lock);
    return status;
}

static int out_dump(const struct audio_stream *stream, int fd)
{
    return 0;
}

static int out_set_parameters(struct audio_stream *stream, const char *kvpairs)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;
    struct sunxi_audio_device *adev = out->dev;
    struct sunxi_stream_in *in;
    struct str_parms *parms;
    char *str;
    char value[128];
    int ret, val = 0;
    bool force_input_standby = false;

    parms = str_parms_create_str(kvpairs);

	ALOGV("out_set_parameters: %s", kvpairs);
 
    ret = str_parms_get_str(parms, AUDIO_PARAMETER_STREAM_ROUTING, value, sizeof(value));
    if (ret >= 0) {
        val = atoi(value);
		if (adev->first_set_audio_routing)
		{
			// we do not use the android default routing
			// init audio routing by fuction init_audio_devices_active()
			adev->first_set_audio_routing= false;
			return ret;
		}
        pthread_mutex_lock(&adev->lock);
        pthread_mutex_lock(&out->lock);
        //if (((adev->devices & AUDIO_DEVICE_OUT_ALL) != val) && (val != 0)) {
        if(val != 0){
            if (out == adev->active_output) {
                /* a change in output device may change the microphone selection */
                if (adev->active_input &&
                        adev->active_input->source == AUDIO_SOURCE_VOICE_COMMUNICATION) {
                    force_input_standby = true;
                }
                /* force standby if moving to/from HDMI */
                if (((val & AUDIO_DEVICE_OUT_AUX_DIGITAL) ^
                        (adev->devices & AUDIO_DEVICE_OUT_AUX_DIGITAL)) ||
                        ((val & AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET) ^
                        (adev->devices & AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET)))
                    do_output_standby(out);
            }
			ALOGD("val: %x, adev->devices: %x", val, adev->devices);
            adev->devices &= ~AUDIO_DEVICE_OUT_ALL;
            adev->devices |= val;

			//set_audio_devices_active_internal(out, AUDIO_OUT, val);

            select_output_device(adev);
        }
        pthread_mutex_unlock(&out->lock);
        if (force_input_standby) {
            in = adev->active_input;
            pthread_mutex_lock(&in->lock);
            do_input_standby(in);
            pthread_mutex_unlock(&in->lock);
        }
        pthread_mutex_unlock(&adev->lock);
    }

	// set audio out device
	ret = str_parms_get_str(parms, AUDIO_PARAMETER_DEVICES_OUT_ACTIVE, value, sizeof(value));
	if (ret >= 0 && strcmp(value, "null") != 0)
	{
		ALOGV("out AUDIO_PARAMETER_DEVICES_OUT_ACTIVE: %s", value);

		pthread_mutex_lock(&adev->lock);
		pthread_mutex_lock(&out->lock);

		if (adev->raw_flag == true)
		{
			pthread_mutex_unlock(&out->lock);
			pthread_mutex_unlock(&adev->lock);
			ALOGW("in raw mode, should not set other audio out devices");
			return -1;
		}

		set_audio_devices_active(adev, AUDIO_OUT, value);
		//strcpy(adev->out_device_active_req, value);

		do_output_standby(out);
		select_output_device(adev);
		pthread_mutex_unlock(&out->lock);
		pthread_mutex_unlock(&adev->lock);
	}

	// for raw data output
	ret = str_parms_get_str(parms, AUDIO_PARAMETER_RAW_DATA_OUT, value, sizeof(value));
	if (ret >= 0)
	{
		bool bval = (atoi(value) == 1) ? true : false;
		ALOGD("AUDIO_PARAMETER_RAW_DATA_OUT: %d", bval);
		pthread_mutex_lock(&adev->lock);
		pthread_mutex_lock(&out->lock);

		if (adev->raw_flag != bval)
		{
			adev->raw_flag = bval;
			do_output_standby(out);
		}

		pthread_mutex_unlock(&out->lock);
		pthread_mutex_unlock(&adev->lock);
	}

    str_parms_destroy(parms);
    return ret;
}

static char * out_get_parameters(const struct audio_stream *stream, const char *keys)
{
    return strdup("");
}

static uint32_t out_get_latency(const struct audio_stream_out *stream)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;

    return (SHORT_PERIOD_SIZE * PLAYBACK_PERIOD_COUNT * 1000) / out->config.rate;
}

static int out_set_volume(struct audio_stream_out *stream, float left,
                          float right)
{
    return -ENOSYS;
}

static ssize_t out_write(struct audio_stream_out *stream, const void* buffer,
                         size_t bytes)
{
    int ret;
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;
    struct sunxi_audio_device *adev = out->dev;
    size_t frame_size = audio_stream_frame_size(&out->stream.common);
    size_t in_frames = bytes / frame_size;
    size_t out_frames = RESAMPLER_BUFFER_SIZE / frame_size;
    bool force_input_standby = false;
    struct sunxi_stream_in *in;
    int kernel_frames;
	void *buf;
	int index;
	int card;

	if (adev->mode == AUDIO_MODE_IN_CALL)
	{
		// ALOGW("mode in call, do not out_write");
		return 0;
	}

	if (adev->raw_flag)
	{
		return 0;
	}

    /* acquiring hw device mutex systematically is useful if a low priority thread is waiting
     * on the output stream mutex - e.g. executing select_mode() while holding the hw device
     * mutex
     */
    pthread_mutex_lock(&adev->lock);
    pthread_mutex_lock(&out->lock);
    if (out->standby) {
        ret = start_output_stream(out);
        if (ret != 0) {
            pthread_mutex_unlock(&adev->lock);
            goto exit;
        }
        out->standby = 0;
        /* a change in output device may change the microphone selection */
        if (adev->active_input &&
                adev->active_input->source == AUDIO_SOURCE_VOICE_COMMUNICATION)
            force_input_standby = true;
    }
    pthread_mutex_unlock(&adev->lock);

    out->write_threshold = SHORT_PERIOD_SIZE * PLAYBACK_PERIOD_COUNT;
    out->config.avail_min = SHORT_PERIOD_SIZE;

	if (adev->af_capture_flag && adev->PcmManager.BufExist) {
		WritePcmData((void *)buffer, out_frames * frame_size, &adev->PcmManager);
		memset((void *)buffer, 0, out_frames * frame_size); //mute
	}

	for (index = MAX_AUDIO_DEVICES; index >= 0; index--)
	{
		if (adev->dev_manager[index].flag_exist
			&& (adev->dev_manager[index].flag_out == AUDIO_OUT)
			&& adev->dev_manager[index].flag_out_active)
		{
			card = index;
			out->multi_config[card].avail_min = SHORT_PERIOD_SIZE;

			pcm_set_avail_min(out->multi_pcm[card], out->multi_config[card].avail_min);

			if (out->multi_resampler[card]) {
				out->multi_resampler[card]->resample_from_input(out->multi_resampler[card],
                                            					(int16_t *)buffer,
                                            					&in_frames,
                                            					(int16_t *)out->buffer,
                                            					&out_frames);
        		buf = out->buffer;
    		} else {
        		out_frames = in_frames;
        		buf = (void *)buffer;
    		}
			
    		if (out->echo_reference != NULL) {
        		struct echo_reference_buffer b;
        		b.raw = (void *)buffer;
       			b.frame_count = in_frames;

        		get_playback_delay(out, out_frames, &b);
        		out->echo_reference->write(out->echo_reference, &b);
    		}
			#if 0
		    /* do not allow more than out->write_threshold frames in kernel pcm driver buffer */
		    do {
		        struct timespec time_stamp;

		       			if (pcm_get_htimestamp(out->multi_pcm[card], (unsigned int *)&kernel_frames, &time_stamp) < 0)
		           			break;
		       			kernel_frames = pcm_get_buffer_size(out->multi_pcm[card]) - kernel_frames;

		        if (kernel_frames > out->write_threshold) {
		            unsigned long time = (unsigned long)
		                    (((int64_t)(kernel_frames - out->write_threshold) * 1000000) /
		                            MM_SAMPLING_RATE);
		            if (time < MIN_WRITE_SLEEP_US)
		                time = MIN_WRITE_SLEEP_US;
		            usleep(time);
		        }
		    } while (kernel_frames > out->write_threshold);
		#endif
			if (strncmp(adev->dev_manager[index].name, "AUDIO_USB", 9))
			{
				ret = pcm_write(out->multi_pcm[card], (void *)buf, out_frames * frame_size);
			}
			else
			{
				if (out->multi_config[index].channels == 2)
				{
					ret = pcm_mmap_write(out->multi_pcm[card], (void *)buf, out_frames * frame_size);
				}
				else
				{
					size_t i;
					char *pcm_buf = (char *)buf;
					for (i = 0; i < out_frames; i++)
					{
						pcm_buf[2 * i + 2] = pcm_buf[4 * i + 4];
						pcm_buf[2 * i + 3] = pcm_buf[4 * i + 5];
					}
					ret = pcm_mmap_write(out->multi_pcm[card], (void *)buf, out_frames * frame_size / 2);
				}
			}
		}
        if(ret!=0)
        {
            ALOGE("##############out_write()  Warning:write fail#################  card=%d", card);
            do_output_standby(out);
            break;
        }
	}

	if (ret != 0) {
		do_output_standby(out);
		ALOGW("##############out_write()  Warning:write fail#######################");
	}
exit:
    pthread_mutex_unlock(&out->lock);

    if (ret != 0) {
        usleep(bytes * 1000000 / audio_stream_frame_size(&stream->common) /
               out_get_sample_rate(&stream->common));
    }

    if (force_input_standby) {
        pthread_mutex_lock(&adev->lock);
        if (adev->active_input) {
            in = adev->active_input;
            pthread_mutex_lock(&in->lock);
            do_input_standby(in);
            pthread_mutex_unlock(&in->lock);
        }
        pthread_mutex_unlock(&adev->lock);
    }

    return bytes;
}

static int out_get_render_position(const struct audio_stream_out *stream,
                                   uint32_t *dsp_frames)
{
    return -EINVAL;
}

static int out_add_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int out_remove_audio_effect(const struct audio_stream *stream, effect_handle_t effect)
{
    return 0;
}

static int out_get_next_write_timestamp(const struct audio_stream_out *stream,
                                        int64_t *timestamp)
{
    return -EINVAL;
}

/** audio_stream_in implementation **/

static int get_next_buffer(struct resampler_buffer_provider *buffer_provider,
                                   struct resampler_buffer* buffer);
static void release_buffer(struct resampler_buffer_provider *buffer_provider,
                                  struct resampler_buffer* buffer);

/* must be called with hw device and input stream mutexes locked */
static int start_input_stream(struct sunxi_stream_in *in)
{
	F_LOG;
    int ret = 0;
    struct sunxi_audio_device *adev = in->dev;

    adev->active_input = in;

    if (adev->mode != AUDIO_MODE_IN_CALL) {
        adev->devices &= ~AUDIO_DEVICE_IN_ALL;
        adev->devices |= in->device;
        select_input_device(adev);
    }

    if (in->need_echo_reference && in->echo_reference == NULL)
        in->echo_reference = get_echo_reference(adev,
                                        AUDIO_FORMAT_PCM_16_BIT,
                                        in->config.channels,
                                        in->requested_rate);

	int in_ajust_rate = in->requested_rate;
	// out/in stream should be both 44.1K serial
	if (!(in->requested_rate % SAMPLING_RATE_11K))
	{
		// OK
		in_ajust_rate = in->requested_rate;
	}
	else
	{
		in_ajust_rate = SAMPLING_RATE_11K * in->requested_rate / SAMPLING_RATE_8K;
		if (in_ajust_rate > SAMPLING_RATE_44K)
		{
			in_ajust_rate = SAMPLING_RATE_44K;
		}
		ALOGV("out/in stream should be both 44.1K serial, force capture rate: %d", in_ajust_rate);
	}

	int card = 0;
	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (adev->dev_manager[card].flag_exist
			&& (adev->dev_manager[card].flag_in == AUDIO_IN)
			&& adev->dev_manager[card].flag_in_active)
		{
			ALOGV("use %s to capture audio", adev->dev_manager[card].name);
			break;
		}
	}

	in->pcm = pcm_open_req(card, PORT_CODEC, PCM_IN, &in->config, in_ajust_rate);

    if (!pcm_is_ready(in->pcm)) {
        ALOGE("cannot open pcm_in driver: %s", pcm_get_error(in->pcm));
        pcm_close(in->pcm);
        adev->active_input = NULL;
        return -ENOMEM;
    }

	// 
	//set_route_by_array(adev->mixer, mic1_rec_routing, 1);

	if (adev->mode == AUDIO_MODE_IN_CALL)
	{
		//set_route_by_array(adev->mixer, line_in_rec_routing, 1);	// must after mic1_rec_routing
	}

	if (in->requested_rate != in->config.rate) {
		in->buf_provider.get_next_buffer = get_next_buffer;
		in->buf_provider.release_buffer = release_buffer;

		ret = create_resampler(in->config.rate,
							   in->requested_rate,
							   in->config.channels,
							   RESAMPLER_QUALITY_DEFAULT,
							   &in->buf_provider,
							   &in->resampler);
		if (ret != 0) {
			ALOGE("create in resampler failed, %d -> %d", in->config.rate, in->requested_rate);
			ret = -EINVAL;
			goto err;
		}

		ALOGV("create in resampler OK, %d -> %d", in->config.rate, in->requested_rate);
	}
	else
	{
		ALOGV("do not use in resampler");
	}

    /* if no supported sample rate is available, use the resampler */
    if (in->resampler) {
        in->resampler->reset(in->resampler);
        in->frames_in = 0;
    }
    return 0;

err:
    if (in->resampler) {
        release_resampler(in->resampler);
    }

	return -1;
}

static uint32_t in_get_sample_rate(const struct audio_stream *stream)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;

    return in->requested_rate;
}

static int in_set_sample_rate(struct audio_stream *stream, uint32_t rate)
{
    return 0;
}

static size_t in_get_buffer_size(const struct audio_stream *stream)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;

    return get_input_buffer_size(in->requested_rate,
                                 AUDIO_FORMAT_PCM_16_BIT,
                                 in->config.channels);
}

static uint32_t in_get_channels(const struct audio_stream *stream)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;

    if (in->config.channels == 1) {
        return AUDIO_CHANNEL_IN_MONO;
    } else {
        return AUDIO_CHANNEL_IN_STEREO;
    }
}

static audio_format_t in_get_format(const struct audio_stream *stream)
{
    return AUDIO_FORMAT_PCM_16_BIT;
}

static int in_set_format(struct audio_stream *stream, audio_format_t format)
{
    return 0;
}

/* must be called with hw device and input stream mutexes locked */
static int do_input_standby(struct sunxi_stream_in *in)
{
    struct sunxi_audio_device *adev = in->dev;

    if (!in->standby) {
        pcm_close(in->pcm);
        in->pcm = NULL;

        adev->active_input = 0;
        if (adev->mode != AUDIO_MODE_IN_CALL) {
            adev->devices &= ~AUDIO_DEVICE_IN_ALL;
            select_input_device(adev);
        }

        if (in->echo_reference != NULL) {
            /* stop reading from echo reference */
            in->echo_reference->read(in->echo_reference, NULL);
            put_echo_reference(adev, in->echo_reference);
            in->echo_reference = NULL;
        }

	    if (in->resampler) {
        	release_resampler(in->resampler);
			in->resampler = NULL;
    	}

        in->standby = 1;

		//
		// set_route_by_array(adev->mixer, line_in_rec_routing, 0);
//		set_route_by_array(adev->mixer, mic1_rec_routing, 0);
    }
    return 0;
}

static int in_standby(struct audio_stream *stream)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;
    int status;

    pthread_mutex_lock(&in->dev->lock);
    pthread_mutex_lock(&in->lock);
    status = do_input_standby(in);
    pthread_mutex_unlock(&in->lock);
    pthread_mutex_unlock(&in->dev->lock);
    return status;
}

static int in_dump(const struct audio_stream *stream, int fd)
{
    return 0;
}

static int in_set_parameters(struct audio_stream *stream, const char *kvpairs)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;
    struct sunxi_audio_device *adev = in->dev;
    struct str_parms *parms;
    char *str;
    char value[128];
    int ret, val = 0;
    bool do_standby = false;

	ALOGV("in_set_parameters: %s", kvpairs);

    parms = str_parms_create_str(kvpairs);

    ret = str_parms_get_str(parms, AUDIO_PARAMETER_STREAM_INPUT_SOURCE, value, sizeof(value));

    pthread_mutex_lock(&adev->lock);
    pthread_mutex_lock(&in->lock);
    if (ret >= 0) {
        val = atoi(value);
        /* no audio source uses val == 0 */
        if ((in->source != val) && (val != 0)) {
            in->source = val;
            do_standby = true;
        }
    }

    ret = str_parms_get_str(parms, AUDIO_PARAMETER_STREAM_ROUTING, value, sizeof(value));
    if (ret >= 0) {
        val = atoi(value);
        if ((in->device != val) && (val != 0)) {
            in->device = val;
            do_standby = true;
        }
    }

	// set audio in device
	ret = str_parms_get_str(parms, AUDIO_PARAMETER_DEVICES_IN_ACTIVE, value, sizeof(value));
	if (ret >= 0)
	{
		ALOGV("in_set_parament AUDIO_PARAMETER_DEVICES_IN_ACTIVE: %s", value);
		//set_audio_devices_active(adev, AUDIO_IN, value);
		do_standby = true;
	}


    if (do_standby)
        do_input_standby(in);
    pthread_mutex_unlock(&in->lock);
    pthread_mutex_unlock(&adev->lock);

    str_parms_destroy(parms);
    return ret;
}

static char * in_get_parameters(const struct audio_stream *stream,
                                const char *keys)
{
    return strdup("");
}

static int in_set_gain(struct audio_stream_in *stream, float gain)
{
    return 0;
}

static void get_capture_delay(struct sunxi_stream_in *in,
                       size_t frames,
                       struct echo_reference_buffer *buffer)
{

    /* read frames available in kernel driver buffer */
    size_t kernel_frames;
    struct timespec tstamp;
    long buf_delay;
    long rsmp_delay;
    long kernel_delay;
    long delay_ns;

    if (pcm_get_htimestamp(in->pcm, &kernel_frames, &tstamp) < 0) {
        buffer->time_stamp.tv_sec  = 0;
        buffer->time_stamp.tv_nsec = 0;
        buffer->delay_ns           = 0;
        ALOGW("read get_capture_delay(): pcm_htimestamp error");
        return;
    }

    /* read frames available in audio HAL input buffer
     * add number of frames being read as we want the capture time of first sample
     * in current buffer */
    buf_delay = (long)(((int64_t)(in->frames_in + in->proc_frames_in) * 1000000000)
                                    / in->config.rate);
    /* add delay introduced by resampler */
    rsmp_delay = 0;
    if (in->resampler) {
        rsmp_delay = in->resampler->delay_ns(in->resampler);
    }

    kernel_delay = (long)(((int64_t)kernel_frames * 1000000000) / in->config.rate);

    delay_ns = kernel_delay + buf_delay + rsmp_delay;

    buffer->time_stamp = tstamp;
    buffer->delay_ns   = delay_ns;
    ALOGV("get_capture_delay time_stamp = [%ld].[%ld], delay_ns: [%d],"
         " kernel_delay:[%ld], buf_delay:[%ld], rsmp_delay:[%ld], kernel_frames:[%d], "
         "in->frames_in:[%d], in->proc_frames_in:[%d], frames:[%d]",
         buffer->time_stamp.tv_sec , buffer->time_stamp.tv_nsec, buffer->delay_ns,
         kernel_delay, buf_delay, rsmp_delay, kernel_frames,
         in->frames_in, in->proc_frames_in, frames);
}

static int32_t update_echo_reference(struct sunxi_stream_in *in, size_t frames)
{
    struct echo_reference_buffer b;
    b.delay_ns = 0;

    ALOGV("update_echo_reference, frames = [%d], in->ref_frames_in = [%d],  "
          "b.frame_count = [%d]",
         frames, in->ref_frames_in, frames - in->ref_frames_in);
    if (in->ref_frames_in < frames) {
        if (in->ref_buf_size < frames) {
            in->ref_buf_size = frames;
            in->ref_buf = (int16_t *)realloc(in->ref_buf,
                                             in->ref_buf_size *
                                                 in->config.channels * sizeof(int16_t));
        }

        b.frame_count = frames - in->ref_frames_in;
        b.raw = (void *)(in->ref_buf + in->ref_frames_in * in->config.channels);

        get_capture_delay(in, frames, &b);

        if (in->echo_reference->read(in->echo_reference, &b) == 0)
        {
            in->ref_frames_in += b.frame_count;
            ALOGV("update_echo_reference: in->ref_frames_in:[%d], "
                    "in->ref_buf_size:[%d], frames:[%d], b.frame_count:[%d]",
                 in->ref_frames_in, in->ref_buf_size, frames, b.frame_count);
        }
    } else
        ALOGW("update_echo_reference: NOT enough frames to read ref buffer");
    return b.delay_ns;
}

static int set_preprocessor_param(effect_handle_t handle,
                           effect_param_t *param)
{
    uint32_t size = sizeof(int);
    uint32_t psize = ((param->psize - 1) / sizeof(int) + 1) * sizeof(int) +
                        param->vsize;

    int status = (*handle)->command(handle,
                                   EFFECT_CMD_SET_PARAM,
                                   sizeof (effect_param_t) + psize,
                                   param,
                                   &size,
                                   &param->status);
    if (status == 0)
        status = param->status;

    return status;
}

static int set_preprocessor_echo_delay(effect_handle_t handle,
                                     int32_t delay_us)
{
    uint32_t buf[sizeof(effect_param_t) / sizeof(uint32_t) + 2];
    effect_param_t *param = (effect_param_t *)buf;

    param->psize = sizeof(uint32_t);
    param->vsize = sizeof(uint32_t);
    *(uint32_t *)param->data = AEC_PARAM_ECHO_DELAY;
    *((int32_t *)param->data + 1) = delay_us;

    return set_preprocessor_param(handle, param);
}

static void push_echo_reference(struct sunxi_stream_in *in, size_t frames)
{
    /* read frames from echo reference buffer and update echo delay
     * in->ref_frames_in is updated with frames available in in->ref_buf */
    int32_t delay_us = update_echo_reference(in, frames)/1000;
    int i;
    audio_buffer_t buf;

    if (in->ref_frames_in < frames)
        frames = in->ref_frames_in;

    buf.frameCount = frames;
    buf.raw = in->ref_buf;

    for (i = 0; i < in->num_preprocessors; i++) {
        if ((*in->preprocessors[i])->process_reverse == NULL)
            continue;

        (*in->preprocessors[i])->process_reverse(in->preprocessors[i],
                                               &buf,
                                               NULL);
        set_preprocessor_echo_delay(in->preprocessors[i], delay_us);
    }

    in->ref_frames_in -= buf.frameCount;
    if (in->ref_frames_in) {
        memcpy(in->ref_buf,
               in->ref_buf + buf.frameCount * in->config.channels,
               in->ref_frames_in * in->config.channels * sizeof(int16_t));
    }
}

static int get_next_buffer(struct resampler_buffer_provider *buffer_provider,
                                   struct resampler_buffer* buffer)
{
    struct sunxi_stream_in *in;

    if (buffer_provider == NULL || buffer == NULL)
        return -EINVAL;

    in = (struct sunxi_stream_in *)((char *)buffer_provider -
                                   offsetof(struct sunxi_stream_in, buf_provider));

    if (in->pcm == NULL) {
        buffer->raw = NULL;
        buffer->frame_count = 0;
        in->read_status = -ENODEV;
        return -ENODEV;
    }

//	ALOGV("get_next_buffer: in->config.period_size: %d, audio_stream_frame_size: %d",
//		in->config.period_size, audio_stream_frame_size(&in->stream.common));
    if (in->frames_in == 0) {
        in->read_status = pcm_read(in->pcm,
                                   (void*)in->buffer,
                                   in->config.period_size *
                                       audio_stream_frame_size(&in->stream.common));
        if (in->read_status != 0) {
            ALOGE("get_next_buffer() pcm_read error %d, %s", in->read_status, strerror(errno));
            buffer->raw = NULL;
            buffer->frame_count = 0;
            return in->read_status;
        }
        in->frames_in = in->config.period_size;
    }

    buffer->frame_count = (buffer->frame_count > in->frames_in) ?
                                in->frames_in : buffer->frame_count;
    buffer->i16 = in->buffer + (in->config.period_size - in->frames_in) *
                                                in->config.channels;

    return in->read_status;

}

static void release_buffer(struct resampler_buffer_provider *buffer_provider,
                                  struct resampler_buffer* buffer)
{
    struct sunxi_stream_in *in;

    if (buffer_provider == NULL || buffer == NULL)
        return;

    in = (struct sunxi_stream_in *)((char *)buffer_provider -
                                   offsetof(struct sunxi_stream_in, buf_provider));

    in->frames_in -= buffer->frame_count;
}

/* read_frames() reads frames from kernel driver, down samples to capture rate
 * if necessary and output the number of frames requested to the buffer specified */
static ssize_t read_frames(struct sunxi_stream_in *in, void *buffer, ssize_t frames)
{
	// F_LOG;
    ssize_t frames_wr = 0;

    while (frames_wr < frames) {
        size_t frames_rd = frames - frames_wr;
        if (in->resampler != NULL) {
            in->resampler->resample_from_provider(in->resampler,
                    (int16_t *)((char *)buffer +
                            frames_wr * audio_stream_frame_size(&in->stream.common)),
                    &frames_rd);
        } else {
            struct resampler_buffer buf = {
                    { raw : NULL, },
                    frame_count : frames_rd,
            };
            get_next_buffer(&in->buf_provider, &buf);
            if (buf.raw != NULL) {
                memcpy((char *)buffer +
                           frames_wr * audio_stream_frame_size(&in->stream.common),
                        buf.raw,
                        buf.frame_count * audio_stream_frame_size(&in->stream.common));
                frames_rd = buf.frame_count;
            }
            release_buffer(&in->buf_provider, &buf);
        }
        /* in->read_status is updated by getNextBuffer() also called by
         * in->resampler->resample_from_provider() */
        if (in->read_status != 0)
            return in->read_status;

        frames_wr += frames_rd;
    }
    return frames_wr;
}

/* process_frames() reads frames from kernel driver (via read_frames()),
 * calls the active audio pre processings and output the number of frames requested
 * to the buffer specified */
static ssize_t process_frames(struct sunxi_stream_in *in, void* buffer, ssize_t frames)
{
	F_LOG;
    ssize_t frames_wr = 0;
    audio_buffer_t in_buf;
    audio_buffer_t out_buf;
    int i;

    while (frames_wr < frames) {
        /* first reload enough frames at the end of process input buffer */
        if (in->proc_frames_in < (size_t)frames) {
            ssize_t frames_rd;

            if (in->proc_buf_size < (size_t)frames) {
                in->proc_buf_size = (size_t)frames;
                in->proc_buf = (int16_t *)realloc(in->proc_buf,
                                         in->proc_buf_size *
                                             in->config.channels * sizeof(int16_t));
                ALOGV("process_frames(): in->proc_buf %p size extended to %d frames",
                     in->proc_buf, in->proc_buf_size);
            }
            frames_rd = read_frames(in,
                                    in->proc_buf +
                                        in->proc_frames_in * in->config.channels,
                                    frames - in->proc_frames_in);
            if (frames_rd < 0) {
                frames_wr = frames_rd;
                break;
            }
            in->proc_frames_in += frames_rd;
        }

        if (in->echo_reference != NULL)
            push_echo_reference(in, in->proc_frames_in);

         /* in_buf.frameCount and out_buf.frameCount indicate respectively
          * the maximum number of frames to be consumed and produced by process() */
        in_buf.frameCount 	= in->proc_frames_in;
        in_buf.s16 			= in->proc_buf;
        out_buf.frameCount 	= frames - frames_wr;
        out_buf.s16 = (int16_t *)buffer + frames_wr * in->config.channels;

        for (i = 0; i < in->num_preprocessors; i++)
            (*in->preprocessors[i])->process(in->preprocessors[i],
                                               &in_buf,
                                               &out_buf);

        /* process() has updated the number of frames consumed and produced in
         * in_buf.frameCount and out_buf.frameCount respectively
         * move remaining frames to the beginning of in->proc_buf */
        in->proc_frames_in -= in_buf.frameCount;
        if (in->proc_frames_in) {
            memcpy(in->proc_buf,
                   in->proc_buf + in_buf.frameCount * in->config.channels,
                   in->proc_frames_in * in->config.channels * sizeof(int16_t));
        }

        /* if not enough frames were passed to process(), read more and retry. */
        if (out_buf.frameCount == 0)
            continue;

        frames_wr += out_buf.frameCount;
    }
    return frames_wr;
}

static ssize_t in_read(struct audio_stream_in *stream, void* buffer,
                       size_t bytes)
{
	 F_LOG;
    int ret = 0;
    struct sunxi_stream_in *in 		= (struct sunxi_stream_in *)stream;
    struct sunxi_audio_device *adev = in->dev;
    size_t frames_rq 				= bytes / audio_stream_frame_size(&stream->common);

    /* acquiring hw device mutex systematically is useful if a low priority thread is waiting
     * on the input stream mutex - e.g. executing select_mode() while holding the hw device
     * mutex
     */
    if (adev->af_capture_flag && adev->PcmManager.BufExist) {
	    pthread_mutex_lock(&adev->lock);
    	pthread_mutex_lock(&in->lock);
	    if (in->standby) {
	        ret = start_input_stream(in);
	        if (ret == 0)
	            in->standby = 0;
	    }
	    pthread_mutex_unlock(&adev->lock);

	    if (ret < 0)
	        goto exit;

		//if (bytes > adev->PcmManager.DataLen)
			//usleep(10000);

	    ret = ReadPcmData(buffer, bytes, &adev->PcmManager);

	    if (ret > 0)
	        ret = 0;

	    if (ret == 0 && adev->mic_mute)
	        memset(buffer, 0, bytes);

	    pthread_mutex_unlock(&in->lock);
   		return bytes;
	}

    pthread_mutex_lock(&adev->lock);
    pthread_mutex_lock(&in->lock);
    if (in->standby) {
        ret = start_input_stream(in);
        if (ret == 0)
            in->standby = 0;
    }
    pthread_mutex_unlock(&adev->lock);

    if (ret < 0)
        goto exit;

    if (in->num_preprocessors != 0) {
        ret = process_frames(in, buffer, frames_rq);
    } else if (in->resampler != NULL) {
        ret = read_frames(in, buffer, frames_rq);
	} else {
        ret = pcm_read(in->pcm, buffer, bytes);
	}

    if (ret > 0)
        ret = 0;

    if (ret == 0 && adev->mic_mute)
        memset(buffer, 0, bytes);

exit:
    if (ret < 0)
        usleep(bytes * 1000000 / audio_stream_frame_size(&stream->common) /
               in_get_sample_rate(&stream->common));

    pthread_mutex_unlock(&in->lock);
    return bytes;
}

static uint32_t in_get_input_frames_lost(struct audio_stream_in *stream)
{
    return 0;
}

static int in_add_audio_effect(const struct audio_stream *stream,
                               effect_handle_t effect)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;
    int status;
    effect_descriptor_t desc;

    pthread_mutex_lock(&in->dev->lock);
    pthread_mutex_lock(&in->lock);
    if (in->num_preprocessors >= MAX_PREPROCESSORS) {
        status = -ENOSYS;
        goto exit;
    }

    status = (*effect)->get_descriptor(effect, &desc);
    if (status != 0)
        goto exit;

    in->preprocessors[in->num_preprocessors++] = effect;

    if (memcmp(&desc.type, FX_IID_AEC, sizeof(effect_uuid_t)) == 0) {
        in->need_echo_reference = true;
        do_input_standby(in);
    }

exit:

    pthread_mutex_unlock(&in->lock);
    pthread_mutex_unlock(&in->dev->lock);
    return status;
}

static int in_remove_audio_effect(const struct audio_stream *stream,
                                  effect_handle_t effect)
{
    struct sunxi_stream_in *in = (struct sunxi_stream_in *)stream;
    int i;
    int status = -EINVAL;
    bool found = false;
    effect_descriptor_t desc;

    pthread_mutex_lock(&in->dev->lock);
    pthread_mutex_lock(&in->lock);
    if (in->num_preprocessors <= 0) {
        status = -ENOSYS;
        goto exit;
    }

    for (i = 0; i < in->num_preprocessors; i++) {
        if (found) {
            in->preprocessors[i - 1] = in->preprocessors[i];
            continue;
        }
        if (in->preprocessors[i] == effect) {
            in->preprocessors[i] = NULL;
            status = 0;
            found = true;
        }
    }

    if (status != 0)
        goto exit;

    in->num_preprocessors--;

    status = (*effect)->get_descriptor(effect, &desc);
    if (status != 0)
        goto exit;
    if (memcmp(&desc.type, FX_IID_AEC, sizeof(effect_uuid_t)) == 0) {
        in->need_echo_reference = false;
        do_input_standby(in);
    }

exit:

    pthread_mutex_unlock(&in->lock);
    pthread_mutex_unlock(&in->dev->lock);
    return status;
}

static int adev_open_output_stream(struct audio_hw_device *dev,
                                   audio_io_handle_t handle,
                                   audio_devices_t devices,
                                   audio_output_flags_t flags,
                                   struct audio_config *config,
                                   struct audio_stream_out **stream_out)
{
	struct sunxi_audio_device *ladev = (struct sunxi_audio_device *)dev;
    struct sunxi_stream_out *out;
    int ret;

	ALOGV("adev_open_output_stream, flags: %x", flags);

    out = (struct sunxi_stream_out *)calloc(1, sizeof(struct sunxi_stream_out));
    if (!out)
        return -ENOMEM;

    out->buffer = malloc(RESAMPLER_BUFFER_SIZE); /* todo: allow for reallocing */

    out->stream.common.get_sample_rate 	= out_get_sample_rate;
    out->stream.common.set_sample_rate 	= out_set_sample_rate;
    out->stream.common.get_buffer_size 	= out_get_buffer_size;
    out->stream.common.get_channels 	= out_get_channels;
    out->stream.common.get_format 		= out_get_format;
    out->stream.common.set_format 		= out_set_format;
    out->stream.common.standby 			= out_standby;
    out->stream.common.dump 			= out_dump;
    out->stream.common.set_parameters 	= out_set_parameters;
    out->stream.common.get_parameters 	= out_get_parameters;
    out->stream.common.add_audio_effect = out_add_audio_effect;
    out->stream.common.remove_audio_effect = out_remove_audio_effect;
    out->stream.get_latency 			= out_get_latency;
    out->stream.set_volume 				= out_set_volume;
    out->stream.write 					= out_write;
    out->stream.get_render_position 	= out_get_render_position;
	out->stream.get_next_write_timestamp = out_get_next_write_timestamp;

    out->config 						= pcm_config_mm_out;

    out->dev 		= ladev;
    out->standby 	= 1;

    /* FIXME: when we support multiple output devices, we will want to
     * do the following:
     * adev->devices &= ~AUDIO_DEVICE_OUT_ALL;
     * adev->devices |= out->device;
     * select_output_device(adev);
     * This is because out_set_parameters() with a route is not
     * guaranteed to be called after an output stream is opened. */
	config->format 			= out_get_format(&out->stream.common);
    config->channel_mask 	= out_get_channels(&out->stream.common);
    config->sample_rate 	= out_get_sample_rate(&out->stream.common);

	ALOGV("+++++++++++++++ adev_open_output_stream: req_sample_rate: %d, fmt: %x, channel_count: %d",
		config->sample_rate, config->format, config->channel_mask);

    *stream_out = &out->stream;
    return 0;

err_open:
    free(out);
    *stream_out = NULL;
    return ret;
}

static void adev_close_output_stream(struct audio_hw_device *dev,
                                     struct audio_stream_out *stream)
{
    struct sunxi_stream_out *out = (struct sunxi_stream_out *)stream;
	unsigned int index;

    out_standby(&stream->common);
    if (out->buffer)
        free(out->buffer);
    if (out->resampler)
        release_resampler(out->resampler);
	for (index = 0; index < MAX_AUDIO_DEVICES; index++)
	{
		if (out->multi_resampler[index])
			release_resampler(out->multi_resampler[index]);
	}

    free(stream);
}

static int adev_set_parameters(struct audio_hw_device *dev, const char *kvpairs)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;
    struct str_parms *parms;
    char *str;
    char value[32];
    int ret;

	ALOGV("adev_set_parameters, %s", kvpairs);

    parms 	= str_parms_create_str(kvpairs);
    ret 	= str_parms_get_str(parms, AUDIO_PARAMETER_KEY_TTY_MODE, value, sizeof(value));
    if (ret >= 0) {
        int tty_mode;

        if (strcmp(value, AUDIO_PARAMETER_VALUE_TTY_OFF) == 0)
            tty_mode = TTY_MODE_OFF;
        else if (strcmp(value, AUDIO_PARAMETER_VALUE_TTY_VCO) == 0)
            tty_mode = TTY_MODE_VCO;
        else if (strcmp(value, AUDIO_PARAMETER_VALUE_TTY_HCO) == 0)
            tty_mode = TTY_MODE_HCO;
        else if (strcmp(value, AUDIO_PARAMETER_VALUE_TTY_FULL) == 0)
            tty_mode = TTY_MODE_FULL;
        else
            return -EINVAL;

        pthread_mutex_lock(&adev->lock);
        if (tty_mode != adev->tty_mode) {
            adev->tty_mode = tty_mode;
            if (adev->mode == AUDIO_MODE_IN_CALL)
                select_output_device(adev);
        }
        pthread_mutex_unlock(&adev->lock);
    }

    ret = str_parms_get_str(parms, AUDIO_PARAMETER_KEY_BT_NREC, value, sizeof(value));
    if (ret >= 0) {
        if (strcmp(value, AUDIO_PARAMETER_VALUE_ON) == 0)
            adev->bluetooth_nrec = true;
        else
            adev->bluetooth_nrec = false;
    }

	// set audio in device
	ret = str_parms_get_str(parms, AUDIO_PARAMETER_DEVICES_IN_ACTIVE, value, sizeof(value));
	if (ret >= 0)
	{
		ALOGV("in AUDIO_PARAMETER_DEVICES_IN_ACTIVE: %s", value);
		set_audio_devices_active(adev, AUDIO_IN, value);
	}

    str_parms_destroy(parms);
    return ret;
}

static char * adev_get_parameters(const struct audio_hw_device *dev,
                                  const char *keys)
{
	struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;

	int ret = -1;
	char devices[128];
	memset(devices, 0, sizeof(devices));

	if (!strcmp(keys, AUDIO_PARAMETER_STREAM_ROUTING))
	{
		char prop_value[512];
		property_get("audio.routing", prop_value, "");
		if (ret > 0)
		{
		    return strdup(prop_value);
		}
	}

	if (!strcmp(keys, AUDIO_PARAMETER_DEVICES_IN))
	{
		return strdup(get_audio_devices(adev, AUDIO_IN));
	}

	if (!strcmp(keys, AUDIO_PARAMETER_DEVICES_OUT))
	{
		return strdup(get_audio_devices(adev, AUDIO_OUT));
	}

	if (!strcmp(keys, AUDIO_PARAMETER_DEVICES_IN_ACTIVE))
	{
		if (!get_audio_devices_active(adev, AUDIO_IN, devices))
		{
			return strdup(devices);
		}
	}

	if (!strcmp(keys, AUDIO_PARAMETER_DEVICES_OUT_ACTIVE))
	{
		if (!get_audio_devices_active(adev, AUDIO_OUT, devices))
		{
			return strdup(devices);
		}
	}

    return strdup("");
}

static int adev_init_check(const struct audio_hw_device *dev)
{
    return 0;
}

static int adev_set_voice_volume(struct audio_hw_device *dev, float volume)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;

	if (adev->mode == AUDIO_MODE_IN_CALL)
	{
	    adev->voice_volume = volume;

		// 59, 47, 35, 23, 11, 0
		int vol = 59;
		if (volume >= 1.0f)
		{
			vol = 59;
		}
		else if (volume >= 0.8f)
		{
			vol = 55;
		}
		else if (volume >= 0.6f)
		{
			vol = 50;
		}
		else if (volume >= 0.4f)
		{
			vol = 43;
		}
		else if (volume >= 0.2f)
		{
			vol = 30;
		}
		else
		{
			vol = 0;
		}

		ALOGV("adev_set_voice_volume, volume: %f, vol: %d", volume, vol);

        call_volume = vol;
		mixer_ctl_set_value(adev->mixer_ctls.master_playback_volume, 0, vol);
	}

    return 0;
}

static int adev_set_master_volume(struct audio_hw_device *dev, float volume)
{
	F_LOG;
    return -ENOSYS;
}

static int adev_get_master_volume(struct audio_hw_device *dev, float *volume)
{
	F_LOG;
	return -ENOSYS;
}

static int adev_set_mode(struct audio_hw_device *dev, audio_mode_t mode)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;

    pthread_mutex_lock(&adev->lock);
    if (adev->mode != mode) {
        adev->mode = mode;
        select_mode(adev);
    }
    pthread_mutex_unlock(&adev->lock);

    return 0;
}

static int adev_set_mic_mute(struct audio_hw_device *dev, bool state)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;

    adev->mic_mute = state;

    return 0;
}

static int adev_get_mic_mute(const struct audio_hw_device *dev, bool *state)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)dev;

    *state = adev->mic_mute;

    return 0;
}

static size_t adev_get_input_buffer_size(const struct audio_hw_device *dev,
                                         const struct audio_config *config)
{
    size_t size;
    int channel_count = popcount(config->channel_mask);
    if (check_input_parameters(config->sample_rate, config->format, channel_count) != 0)
        return 0;

    return get_input_buffer_size(config->sample_rate, config->format, channel_count);
}

static int adev_open_input_stream(struct audio_hw_device *dev,
                                  audio_io_handle_t handle,
                                  audio_devices_t devices,
                                  struct audio_config *config,
                                  struct audio_stream_in **stream_in)
{
    struct sunxi_audio_device *ladev = (struct sunxi_audio_device *)dev;
    struct sunxi_stream_in *in;
    int ret;
    int channel_count = popcount(config->channel_mask);

    *stream_in = NULL;

    if (check_input_parameters(config->sample_rate, config->format, channel_count) != 0)
        return -EINVAL;

    in = (struct sunxi_stream_in *)calloc(1, sizeof(struct sunxi_stream_in));
    if (!in)
        return -ENOMEM;

    in->stream.common.get_sample_rate 	= in_get_sample_rate;
    in->stream.common.set_sample_rate 	= in_set_sample_rate;
    in->stream.common.get_buffer_size 	= in_get_buffer_size;
    in->stream.common.get_channels 		= in_get_channels;
    in->stream.common.get_format 		= in_get_format;
    in->stream.common.set_format 		= in_set_format;
    in->stream.common.standby 			= in_standby;
    in->stream.common.dump 				= in_dump;
    in->stream.common.set_parameters 	= in_set_parameters;
    in->stream.common.get_parameters 	= in_get_parameters;
    in->stream.common.add_audio_effect 	= in_add_audio_effect;
    in->stream.common.remove_audio_effect = in_remove_audio_effect;
    in->stream.set_gain 				= in_set_gain;
    in->stream.read 					= in_read;
    in->stream.get_input_frames_lost 	= in_get_input_frames_lost;

    in->requested_rate 	= config->sample_rate;

    // default config
    memcpy(&in->config, &pcm_config_mm_in, sizeof(pcm_config_mm_in));
    in->config.channels = channel_count;
	in->config.in_init_channels = channel_count;

	ALOGV("to malloc in-buffer: period_size: %d, frame_size: %d",
		in->config.period_size, audio_stream_frame_size(&in->stream.common));
    in->buffer = malloc(in->config.period_size *
                        audio_stream_frame_size(&in->stream.common) * 8);

    if (!in->buffer) {
        ret = -ENOMEM;
        goto err;
    }

	ladev->af_capture_flag = false;
	//devices = AUDIO_DEVICE_IN_WIFI_DISPLAY;//for test

	if (devices == AUDIO_DEVICE_IN_AF) {
		ALOGV("to malloc PcmManagerBuffer: Buffer_size: %d", AF_BUFFER_SIZE);
    	ladev->PcmManager.BufStart= (unsigned char *)malloc(AF_BUFFER_SIZE);

		if(!ladev->PcmManager.BufStart) {
			ret = -ENOMEM;
			goto err;
   		}

		ladev->PcmManager.BufExist 		= true;
		ladev->PcmManager.BufTotalLen 	= AF_BUFFER_SIZE;
		ladev->PcmManager.BufWritPtr 	= ladev->PcmManager.BufStart;
		ladev->PcmManager.BufReadPtr 	= ladev->PcmManager.BufStart;
		ladev->PcmManager.BufValideLen	= ladev->PcmManager.BufTotalLen;
		ladev->PcmManager.DataLen 		= 0;
		ladev->PcmManager.SampleRate 	= config->sample_rate;
		ladev->PcmManager.Channel 		= 2;
		ladev->af_capture_flag 			= true;
	}

    in->dev 	= ladev;
    in->standby = 1;
    in->device 	= devices;

    *stream_in 	= &in->stream;
    return 0;

err:
    if (in->resampler)
        release_resampler(in->resampler);

    free(in);
    return ret;
}

static void adev_close_input_stream(struct audio_hw_device *dev,
                                   struct audio_stream_in *stream)
{
    struct sunxi_stream_in *in 			= (struct sunxi_stream_in *)stream;
	struct sunxi_audio_device *ladev 	= (struct sunxi_audio_device *)dev;

    in_standby(&stream->common);

	if (in->buffer) {
        free(in->buffer);
		in->buffer = 0;
	}
    if (in->resampler) {
        release_resampler(in->resampler);
    }
	if (ladev->af_capture_flag) {
		ladev->af_capture_flag = false;
	}
	if (ladev->PcmManager.BufStart) {
		ladev->PcmManager.BufExist = false;
		free(ladev->PcmManager.BufStart);
		ladev->PcmManager.BufStart = 0;
	}
    free(stream);
    return;
}

static int adev_dump(const audio_hw_device_t *device, int fd)
{
    return 0;
}

static int adev_close(hw_device_t *device)
{
    struct sunxi_audio_device *adev = (struct sunxi_audio_device *)device;

	mixer_close(adev->mixer);
    free(device);
    return 0;
}

static uint32_t adev_get_supported_devices(const struct audio_hw_device *dev)
{
    return (/* OUT */
            AUDIO_DEVICE_OUT_EARPIECE |
            AUDIO_DEVICE_OUT_SPEAKER |
            AUDIO_DEVICE_OUT_WIRED_HEADSET |
            AUDIO_DEVICE_OUT_WIRED_HEADPHONE |
            AUDIO_DEVICE_OUT_AUX_DIGITAL |
            AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET |
            AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET |
            AUDIO_DEVICE_OUT_ALL_SCO |
            AUDIO_DEVICE_OUT_DEFAULT |
            /* IN */
            AUDIO_DEVICE_IN_COMMUNICATION |
            AUDIO_DEVICE_IN_AMBIENT |
            AUDIO_DEVICE_IN_BUILTIN_MIC |
            AUDIO_DEVICE_IN_WIRED_HEADSET |
            AUDIO_DEVICE_IN_AUX_DIGITAL |
            AUDIO_DEVICE_IN_BACK_MIC |
			AUDIO_DEVICE_IN_AF |
            AUDIO_DEVICE_IN_ALL_SCO |
            AUDIO_DEVICE_IN_DEFAULT);
}

static int adev_open(const hw_module_t* module, const char* name,
                     hw_device_t** device)
{
    struct sunxi_audio_device *adev;
    int ret;

    if (strcmp(name, AUDIO_HARDWARE_INTERFACE) != 0)
        return -EINVAL;

    adev = calloc(1, sizeof(struct sunxi_audio_device));
    if (!adev)
        return -ENOMEM;

    adev->hw_device.common.tag 		= HARDWARE_DEVICE_TAG;
    adev->hw_device.common.version = AUDIO_DEVICE_API_VERSION_2_0;
    adev->hw_device.common.module 	= (struct hw_module_t *) module;
    adev->hw_device.common.close 	= adev_close;

    adev->hw_device.get_supported_devices 	= adev_get_supported_devices;
    adev->hw_device.init_check 				= adev_init_check;
    adev->hw_device.set_voice_volume 		= adev_set_voice_volume;
    adev->hw_device.set_master_volume 		= adev_set_master_volume;
	adev->hw_device.get_master_volume 		= adev_get_master_volume;
    adev->hw_device.set_mode 				= adev_set_mode;
    adev->hw_device.set_mic_mute 			= adev_set_mic_mute;
    adev->hw_device.get_mic_mute 			= adev_get_mic_mute;
    adev->hw_device.set_parameters 			= adev_set_parameters;
    adev->hw_device.get_parameters 			= adev_get_parameters;
    adev->hw_device.get_input_buffer_size 	= adev_get_input_buffer_size;
    adev->hw_device.open_output_stream 		= adev_open_output_stream;
    adev->hw_device.close_output_stream 	= adev_close_output_stream;
    adev->hw_device.open_input_stream 		= adev_open_input_stream;
    adev->hw_device.close_input_stream 		= adev_close_input_stream;
    adev->hw_device.dump 					= adev_dump;


	adev->raw_flag = false;
	adev->af_capture_flag = false;
	adev->first_set_audio_routing = true;

    int disp_fp;
    unsigned long arg[4]={0};
    __u32 hpd_state = 0;
    disp_fp = open("/dev/disp", O_RDWR);
    if (disp_fp < 0){
        ALOGE("audio open hdimstate: disp_fp=%d", hpd_state);
    }

    arg[0] = 0;
    hpd_state = ioctl(disp_fp, DISP_CMD_HDMI_GET_HPD_STATUS, (unsigned long)arg);
    ALOGE("audio find hdimstate: hpd_state=%d", hpd_state);
    if (hpd_state){
        property_set(PRO_AUDIO_OUTPUT_ACTIVE, "AUDIO_HDMI");

    }else {
        property_set(PRO_AUDIO_OUTPUT_ACTIVE, "AUDIO_CODEC");
    }


	init_audio_devices(adev);
	init_audio_devices_active(adev);

	int card = 0;
	for (card = 0; card < MAX_AUDIO_DEVICES; card++)
	{
		if (adev->dev_manager[card].flag_exist
			&& (adev->dev_manager[card].flag_in == AUDIO_IN)
			&& !strcmp(adev->dev_manager[card].name, AUDIO_NAME_CODEC))
		{
			ALOGV("use %s mixer control", adev->dev_manager[card].name);
			break;
		}
	}

	if (card == MAX_AUDIO_DEVICES)
	{
		ALOGE("can not find audio codec mixer control");
	}

    adev->mixer = mixer_open(card);
    if (!adev->mixer) {
		free(adev);
		ALOGE("Unable to open the mixer, aborting.");
		return -EINVAL;
    }

#if !LOG_NDEBUG
    // dump list of mixer controls
	tinymix_list_controls(adev->mixer);
#endif

    adev->mixer_ctls.audio_speaker_out = mixer_get_ctl_by_name(adev->mixer,
									   MIXER_AUDIO_SPEAKER_OUT);
	if (!adev->mixer_ctls.audio_speaker_out) {
		ALOGE("Unable to find '%s' mixer control",MIXER_AUDIO_SPEAKER_OUT);
		goto error_out;
	}


    /* Set the default route before the PCM stream is opened */
    pthread_mutex_lock(&adev->lock);

	char prop_value[16];
	adev->support_multi_ouput = false;
	ret = property_get(PRO_AUDIO_MULTI_OUTPUT, prop_value, "");
	if (ret > 0)
	{
		ALOGV("get property %s: %s", PRO_AUDIO_MULTI_OUTPUT, prop_value);
		if (strcmp(prop_value, "true") == 0)
		{
			adev->support_multi_ouput = true;
		}
	}

//    set_route_by_array(adev->mixer, defaults, 1);
    adev->mode 				= AUDIO_MODE_NORMAL;
    adev->devices 			= AUDIO_DEVICE_OUT_SPEAKER | AUDIO_DEVICE_IN_BUILTIN_MIC;
    select_output_device(adev);

    adev->pcm_modem_dl 		= NULL;
    adev->pcm_modem_ul 		= NULL;
    adev->voice_volume 		= 1.0f;
    adev->tty_mode 			= TTY_MODE_OFF;
    adev->bluetooth_nrec 	= true;
    adev->wb_amr = 0;
	
    pthread_mutex_unlock(&adev->lock);

    *device = &adev->hw_device.common;

    return 0;

error_out:     
 
#if !LOG_NDEBUG
    /* To aid debugging, dump all mixer controls */
    {
            unsigned int cnt = mixer_get_num_ctls(adev->mixer);
            unsigned int i;
            ALOGD("Mixer dump: Nr of controls: %d",cnt);
            for (i = 0; i < cnt; i++) {
                    struct mixer_ctl* x = mixer_get_ctl(adev->mixer,i);
                    if (x != NULL) {
                            char * name;
                            const char* type;
                            name = mixer_ctl_get_name(x);
                            type = mixer_ctl_get_type_string(x);
                            ALOGD("#%d: '%s' [%s]",i,name,type);
                    }
            }
    }
#endif

    mixer_close(adev->mixer);
    free(adev);
    return -EINVAL;
}

static struct hw_module_methods_t hal_module_methods = {
    .open = adev_open,
};

struct audio_module HAL_MODULE_INFO_SYM = {
    .common = {
        .tag 				= HARDWARE_MODULE_TAG,
        .module_api_version = AUDIO_MODULE_API_VERSION_0_1,
		.hal_api_version 	= HARDWARE_HAL_API_VERSION,
        .id 				= AUDIO_HARDWARE_MODULE_ID,
        .name 				= "sunxi audio HW HAL",
        .author 			= "author",
        .methods 			= &hal_module_methods,
    },
};
