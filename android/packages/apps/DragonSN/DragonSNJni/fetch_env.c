/*
* (C) Copyright 2007-2013
* Allwinner Technology Co., Ltd. <www.allwinnertech.com>
* Charles <yanjianbo@allwinnertech.com>
*
* See file CREDITS for list of people who contributed to this
* project.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License as
* published by the Free Software Foundation; either version 2 of
* the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston,
* MA 02111-1307 USA
*/
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdlib.h>
#include <errno.h>

#define MAGIC                       ("sunxi")
#define PART_NAME               ("private")
#define PRIVATE_SIZE        (16 * 1024 * 1024)
#define USER_DATA_MAXSIZE               (8 * 1024)
#define USER_DATA_PARAMETER_MAX_COUNT   (30)
#define NANDI_PATH2          ("/dev/block/private")
//#define NANDI_PATH          ("/dev/block/nandi")
#define NANDI_PATH          ("/dev/block/by-name/private")

#define NAME_SIZE       (32)
#define VALUE_SIZE  (128)

#include <android/log.h>
#define LOG_TAG "dragonenter_so"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    char magic_name[8];
    int count;
    int reserved[3];
}USER_DATA_HEAR;

typedef struct {
    char name[NAME_SIZE];
    char value[VALUE_SIZE];
    int valid;
    int reserved[3];
}USER_PRIVATE_DATA;



/*
 * Write Content to /dev/block/nandi partition
 */
int env_write(char *buf)
{
	
	int private_fd, n;
	if ((private_fd = open(NANDI_PATH, O_WRONLY)) < 0)
	{
        if ((private_fd = open(NANDI_PATH2, O_WRONLY)) < 0) {
            LOGD("Open nandi Error: %s\n", strerror(errno));
            return -1;
        }
	}
	lseek(private_fd, PRIVATE_SIZE - USER_DATA_MAXSIZE, SEEK_SET);
	if ((n = write(private_fd, buf, USER_DATA_MAXSIZE)) < 0)
	{
		LOGD("Read nandi Error: %s\n", strerror(errno));
		close(private_fd);
		return -1;
	}
	close(private_fd);
	return 0;
}

/*
 * Read Content from /dev/block/nandi partition,
 * Store content to buf
 */
int env_read(char *buf)
{
	int private_fd, n;
	if ((private_fd = open(NANDI_PATH, O_RDONLY)) < 0)
	{
        if ((private_fd = open(NANDI_PATH2, O_RDONLY)) < 0)
        {
            LOGD("Open nandi Error: %s\n", strerror(errno));
            return -1;
        }
	}
	lseek(private_fd, PRIVATE_SIZE - USER_DATA_MAXSIZE, SEEK_SET);
	if ((n = read(private_fd, buf, USER_DATA_MAXSIZE)) < 0)
	{
		LOGD("Read nandi Error: %s\n", strerror(errno));
		close(private_fd);
		return -1;
	}
	close(private_fd);
	return 0;
}

/*
************************************************************************************************************
*
*
*
*
*
*               修改mac地址、sn
*
*
*
*
*
************************************************************************************************************
*/
int modify_env_parameter(char *private_buf, char *name, char *value)
{
    int j;
    char *user_data_buffer = NULL;  //
    USER_PRIVATE_DATA *user_data_p = NULL;
    USER_DATA_HEAR *user_data_head = NULL;
    char cmp_data_name[8] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff};

    if (!private_buf || !name || !value) {
        LOGD("error: the private_buf or name or value is null\n");
        return -1;
    }

	user_data_buffer = (char *)private_buf;
    user_data_head = (USER_DATA_HEAR *)user_data_buffer;
    user_data_p = (USER_PRIVATE_DATA *)(user_data_buffer + sizeof(USER_DATA_HEAR));
    if (strncmp(user_data_head->magic_name, MAGIC, 5)) {
        memset(user_data_buffer, 0xff, USER_DATA_MAXSIZE);
        strcpy(user_data_head->magic_name, MAGIC);
        user_data_head->count = 0;
        LOGD("init the (user) private space\n");
    }

    if (strncmp(cmp_data_name, (char *)user_data_p, 8))
    {
        if (user_data_head->count > 0)
        {
            for (j = 0; j < user_data_head->count && j < USER_DATA_PARAMETER_MAX_COUNT; j++)
            {
                if (!strcmp(name, user_data_p->name))
                {
                    strcpy(user_data_p->value, value);
                    user_data_p->valid = 1;
                    LOGD("Saving Environment to (1)\n");
                    break;
                }
                user_data_p++;
            }
            if (j == user_data_head->count)
            {
                    strcpy(user_data_p->name, name);
                    strcpy(user_data_p->value, value);
                    user_data_p->valid = 1;
                    user_data_head->count++;
                    LOGD("Saving Environment to (2)\n");
            }
        }
        else
        {
            strcpy(user_data_p->name, name);
            strcpy(user_data_p->value, value);
            user_data_p->valid = 1;
            user_data_head->count++;
            LOGD("Saving Environment to (3)\n");
        }
    }
    else
    {
        user_data_head->count = 1;
        user_data_p = (USER_PRIVATE_DATA *)(user_data_buffer + sizeof(USER_DATA_HEAR));
        strcpy(user_data_p->name, name);
        strcpy(user_data_p->value, value);
        user_data_p->valid = 1;
        LOGD("Saving Environment to (3)\n");
    }
    return 0;
}

/*
************************************************************************************************************
*
*
*
*
*
*
*   查看 mac、sn
*
*
*
*
************************************************************************************************************
*/
int check_env_parameter(char *private_buf, char *name, char *value)
{
    int j;
    char *user_data_buffer = NULL;  //
    USER_PRIVATE_DATA *user_data_p = NULL;
    USER_DATA_HEAR *user_data_head = NULL;
    char cmp_data_name[8] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff};

    if (!private_buf || !name) {
        LOGD("error: the private_buf or name is null\n");
        return 0;
    }

	user_data_buffer = (char *)private_buf;
    user_data_head = (USER_DATA_HEAR *)user_data_buffer;
    user_data_p = (USER_PRIVATE_DATA *)(user_data_buffer + sizeof(USER_DATA_HEAR));
    if (strncmp(user_data_head->magic_name, MAGIC, 5)) {
        LOGD("the user_data is space\n");
        return -1;
    }

    if (strncmp(cmp_data_name, (char *)user_data_p, 8))
    {
        if (user_data_head->count > 0)
        {
            for (j = 0; j < user_data_head->count && j < USER_DATA_PARAMETER_MAX_COUNT; j++)
            {
                if (!strcmp(name, user_data_p->name))
                {
                    strcpy(value, user_data_p->value);
                    return 0;
                }
                user_data_p++;
            }
        }
    }
    value = NULL;
    return 0;
}
