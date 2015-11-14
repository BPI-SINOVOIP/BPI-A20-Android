/*
* \file        spdiftester.c
* \brief       
*
* \version     1.0.0
* \date        2014年04月17日
* \author      Huanghuibao <huanghuibao@allwinnertech.com>
*
* Copyright (c) 2014 Allwinner Technology. All Rights Reserved.
*
*/

#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/unistd.h>
#include <sys/mman.h>
#include <pthread.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>

#include "include/tinyalsa/asoundlib.h"

#include "dragonboard_inc.h"

static int sound_play_stop;
static int sound_pcm_hw_ready = 0;

#define BUF_LEN                         4096
char *buf[BUF_LEN];


//static int xrun_recovery(snd_pcm_t *handle, int err)
//{
//	if (err == -EPIPE) {
//		err = snd_pcm_prepare(handle);
//	}
//
//	if (err < 0) {
//		db_warn("Can't recovery from underrun, prepare failed: %s\n", snd_strerror(err));
//	}
//	else if (err == -ESTRPIPE) {
//		while ((err = snd_pcm_resume(handle)) == -EAGAIN) {
//			sleep(1);
//
//			if (err < 0) {
//				err = snd_pcm_prepare(handle);
//			}
//			if (err < 0) {
//				db_warn("Can't recovery from suspend, prepare failed: %s\n", snd_strerror(err));
//			}
//		}
//
//		return 0;
//	}
//
//	return err;
//}

static void *sound_play(void *args)
{
    struct pcm_config config;
    struct pcm *pcm0;
    char *buffer;
    FILE *file;
    int size;
    int num_read;
    int device = 0;
    int channels = 2;
    int rate = 44100;
    int bits	= 16;
        char path[256];
    int samplerate;
    int err;


    db_msg("spdif prepare play sound...\n");
    if (script_fetch("hdmi", "sound_file", (int *)path, sizeof(path) / 4)) {
        db_warn("unknown sound file, use default\n");
        strcpy(path, "/dragonboard/data/test48000.pcm");
    }
    if (script_fetch("hdmi", "samplerate", &samplerate, 1)) {
        db_warn("unknown samplerate, use default #48000\n");
        rate = 48000;
    }
    db_msg("spdif samplerate #%d\n", rate);
    file = fopen(path, "r");
    if (file == NULL) {
        db_error("cannot open test pcm file(%s)\n", strerror(errno));
		sound_pcm_hw_ready = 0;
        pthread_exit((void *)-1);
    }

/*
channels = 2,period_size = 8092;  buffer_size = 8092*4 = 32k
channels = 4, period_size = 4096, buffer_size = 4096*8 = 32k
channels = 4, period_size = 2048, buffer_size = 2048*8 = 16k
channels = 4, period_size = 1024, buffer_size = 1024*8 = 8k
*/
    config.channels = 2;
    config.rate = rate;
    config.period_size = 2048;//4096;//2048
    config.period_count = 1;
    if (bits == 32)
        config.format = PCM_FORMAT_S32_LE;
    else if (bits == 16)
        config.format = PCM_FORMAT_S16_LE;
    config.start_threshold = 0;
    config.stop_threshold = 0;
    config.silence_threshold = 0;

	/*0 is audiocodec, 1 is hdmiaudio, 2 is spdif*/
    pcm0 = pcm_open(2, device, PCM_OUT, &config);
    if (!pcm0 || !pcm_is_ready(pcm0)) {
		fprintf(stderr, "Unable to open PCM device %u (%s)\n", device, pcm_get_error(pcm0));
		sound_pcm_hw_ready = 0;
		return;
	}

    size = pcm_get_buffer_size(pcm0);
    buffer = malloc(size);
    if (!buffer) {
        fprintf(stderr, "Unable to allocate %d bytes\n", size);
        free(buffer);
        pcm_close(pcm0);
		sound_pcm_hw_ready = 0;
        return;
    }
    size =size;
    printf("spdif hx-Playing sample:size:%d, %u ch, %u hz, %u bit\n", size, channels, rate, bits);

	sound_pcm_hw_ready = 1;

    do {
		if (sound_play_stop) {
			goto out;
		}
		
        num_read = fread(buffer, 1, size, file);
        if (num_read > 0) {
            if (pcm_write(pcm0, buffer, num_read)) {
                fprintf(stderr, "Error playing sample\n");
                break;
            }
        }
		
		if (feof(file)) {
			fseek(file, 0L, SEEK_SET);
		}
    } while (num_read > 0);
out:
    free(buffer);
    pcm_close(pcm0);
}

#if 0
static void *sound_play(void *args)
{
	char path[256];
	int samplerate;
	int err;
	snd_pcm_t *playback_handle;
	snd_pcm_hw_params_t *hw_params;
	FILE *fp;

	db_msg("prepare play sound...\n");
	if (script_fetch("spdif", "sound_file", (int *)path, sizeof(path) / 4)) {
		db_warn("unknown sound file, use default\n");
		strcpy(path, "/dragonboard/data/test48000.pcm");
	}
	if (script_fetch("spdif", "samplerate", &samplerate, 1)) {
		db_warn("unknown samplerate, use default #48000\n");
		samplerate = 48000;
	}
	db_msg("samplerate #%d\n", samplerate);

	err = snd_pcm_open(&playback_handle, "hw:2,0", SND_PCM_STREAM_PLAYBACK, 0);
	if (err < 0) {
		db_error("cannot open audio device (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_malloc(&hw_params);
	if (err < 0) {
		snd_pcm_close(playback_handle);
		db_error("cannot allocate hardware parameter structure (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_any(playback_handle, hw_params);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot initialize hardware parameter structure (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_set_access(playback_handle, hw_params, SND_PCM_ACCESS_RW_INTERLEAVED);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot allocate hardware parameter structure (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_set_format(playback_handle, hw_params, SND_PCM_FORMAT_S16_LE);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot allocate hardware parameter structure (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_set_rate(playback_handle, hw_params, samplerate, 0);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot set sample rate (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	err = snd_pcm_hw_params_set_channels(playback_handle, hw_params, 2);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot set channel count (%s), err = %d\n", snd_strerror(err), err);
		goto sound_err;
	}

	err = snd_pcm_hw_params(playback_handle, hw_params);
	if (err < 0) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot set parameters (%s)\n", snd_strerror(err));
		goto sound_err;
	}

	snd_pcm_hw_params_free(hw_params);
	sound_pcm_hw_ready = 1;

	db_msg("open test pcm file: %s\n", path);
	fp = fopen(path, "r");
	if (fp == NULL) {
		snd_pcm_hw_params_free(hw_params);
		snd_pcm_close(playback_handle);
		db_error("cannot open test pcm file(%s)\n", strerror(errno));
		goto sound_err;
	}

	db_msg("play it...\n");
	while (1) {
		while (!feof(fp)) {
			if (sound_play_stop) {
				goto out;
			}

			err = fread(buf, 1, BUF_LEN, fp);
			if (err < 0) {
				db_warn("read test pcm failed(%s)\n", strerror(errno));
			}

			err = snd_pcm_writei(playback_handle, buf, BUF_LEN/4);
			if (err < 0) {
				err = xrun_recovery(playback_handle, err);
				if (err < 0) {
					db_warn("write error: %s\n", snd_strerror(err));
				}
			}

			if (err == -EBADFD) {
				db_warn("PCM is not in the right state (SND_PCM_STATE_PREPARED or SND_PCM_STATE_RUNNING)\n");
			}
			if (err == -EPIPE) {
				db_warn("an underrun occurred\n");
			}
			if (err == -ESTRPIPE) {
				db_warn("a suspend event occurred (stream is suspended and waiting for an application recovery)\n");
			}

			if (feof(fp)) {
				fseek(fp, 0L, SEEK_SET);
			}
		}
	}

out:
	db_msg("play end...\n");
	fclose(fp);
	snd_pcm_close(playback_handle);
	pthread_exit(0);

sound_err:
	sound_pcm_hw_ready = 0;
	pthread_exit(-1);
}
#endif

int main(int argc, char *argv[])
{
	unsigned int args[4];
	int status = 0;
	int retry = 0;
	int flags = 0;
	int ret;
	pthread_t tid;
	void *retval;
	int sound_playing = 0;

	INIT_CMD_PIPE();

	init_script(atoi(argv[2]));

	db_error("spdiftester: start\n");

	/* test main loop */
	while (1) {
		/* todo: how to check spdif state? */
		if (1/* spdif connected */) {
			if (!sound_playing) {
				/* create sound play thread */
				sound_play_stop = 0;
				ret = pthread_create(&tid, NULL, sound_play, NULL);
				if (ret != 0) {
					db_error("spdiftester: create sound play thread failed\n");
					args[0] = 0;
					goto err;
				}

				SEND_CMD_PIPE_OK();
				sound_playing = 1;
			}
		} else {
			if (sound_playing) {
				sound_play_stop = 1;
				db_msg("spdiftester: waiting for sound play thread finish...\n");
				if (pthread_join(tid, &retval)) {  
					db_error("spdiftester: can't join with sound play thread\n"); 
				}        
				db_msg("spdiftester: sound play thread exit code #%d\n", (int)retval);
				sound_playing = 0;
			}
		}
		sleep(5);
		if (!sound_pcm_hw_ready)
			SEND_CMD_PIPE_FAIL();
	}

err:
	SEND_CMD_PIPE_FAIL();
	deinit_script();
	return -1;
}
