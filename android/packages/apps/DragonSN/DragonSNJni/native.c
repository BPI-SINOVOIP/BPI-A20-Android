#include <stdlib.h>
#include <stdio.h>
#include <jni.h>
/* Header for class com_allwinnertech_dragonsn_jni_ReadPrivateJNI */
#include "fetch_env.h"

#include <android/log.h>
#define LOG_TAG "dragonenter_so"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef enum __bool { false = 0, true = 1, } bool;

#define USER_DATA_MAXSIZE		(8 * 1024)

#define MAC_SIZE (128)
#define SN_SIZE (128)
#define USER_DATA_MAXSIZE		(8 * 1024)

char mac_buf[MAC_SIZE];
char sn_buf[SN_SIZE];
char *pri_buf;

extern int env_read(char *buf);
extern int env_write(char *buf);
extern int modify_env_parameter(char *private_buf, char *name, char *value);
extern int check_env_parameter(char *private_buf, char *name, char *value);

/*
 * Class:     com_allwinnertech_dragonsn_jni_ReadPrivateJNI
 * Method:    native_init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_allwinnertech_dragonsn_jni_ReadPrivateJNI_native_1init
  (JNIEnv *env, jclass thiz){
	LOGD("jni_init");

	pri_buf = (void *)malloc(USER_DATA_MAXSIZE);
	if(NULL==pri_buf){
		LOGD("jni_NULL==pri_buf");
		return false;

	}
	return true;
}

/*
 * Class:     com_allwinnertech_dragonsn_jni_ReadPrivateJNI
 * Method:    native_get_parameter
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_allwinnertech_dragonsn_jni_ReadPrivateJNI_native_1get_1parameter
  (JNIEnv *env, jclass thiz, jstring name){
	int i;
	char *check_name = (*env)->GetStringUTFChars(env, name, NULL);
	LOGD("read  name=%s",check_name);

	memset(pri_buf, 0xff, USER_DATA_MAXSIZE);
	if(env_read(pri_buf) != 0){
		LOGD("read fail");
		return false;
	}

	if(check_env_parameter(pri_buf, check_name, mac_buf) == 0){
		LOGD("read mac_buf=%s",mac_buf);
		jstring rtstr = (*env)->NewStringUTF(env, mac_buf);

		for (i = 0; i < 50; i++)
			LOGD("%x ", mac_buf[i]);

		*mac_buf=NULL;
		return rtstr;
		//return NULL;
	}
	LOGD("read _fail2");
	return NULL;
}

/*
 * Class:     com_allwinnertech_dragonsn_jni_ReadPrivateJNI
 * Method:    native_set_parameter
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_allwinnertech_dragonsn_jni_ReadPrivateJNI_native_1set_1parameter
  (JNIEnv *env, jclass thiz, jstring name, jstring value){
	char *modify_name = (*env)->GetStringUTFChars(env, name, NULL);
	LOGD("write *modify_name=%s",modify_name);
	LOGD("write *value=%s",(*env)->GetStringUTFChars(env, value, NULL));
	strcpy(mac_buf,(*env)->GetStringUTFChars(env, value, NULL));

	memset(pri_buf, 0xff, USER_DATA_MAXSIZE);
	if(env_read(pri_buf) != 0){
		LOGD("read fail");
		return false;
	}
	LOGD("**************mac_buf = %s", mac_buf);
	if(modify_env_parameter(pri_buf, modify_name, mac_buf) != 0){
		LOGD("write fail");
		*mac_buf=NULL;
		return false;
	}

	if(env_write(pri_buf) != 0){
		LOGD("write  fail1!");
		return false;
	}

	LOGD("write success!");
	return true;
}

/*
 * Class:     com_allwinnertech_dragonsn_jni_ReadPrivateJNI
 * Method:    natice_release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_allwinnertech_dragonsn_jni_ReadPrivateJNI_native_1release
  (JNIEnv *env, jclass thiz){
	LOGD("release");
	free(pri_buf);
}
