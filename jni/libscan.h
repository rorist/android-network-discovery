//#include <string.h>
//#include <jni.h>
#include <android/log.h>
/*
#include<stdio.h>
#include<stdlib.h>
#include<sys/socket.h>
#include<features.h>
#include<linux/if_packet.h>
#include<linux/if_ether.h>
#include<errno.h>
#include<sys/ioctl.h>
#include<net/if.h>
#include<fcntl.h>
*/

#define PACKET_LENGTH	1024

#ifndef LIBSCAN_H
#define LIBSCAN_H

extern char debug(char* str);
extern int scan();
int CreateRawSocket(int protocol);
int SendRawPacket(int rawsock, unsigned char *pkt, int pkt_len);
int BindRawSocketToInterface(char *device, int rawsock, int protocol);

#endif /* LIBSCAN_H */

#define LOG_TAG "scand"

#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <ctype.h>
#include <fcntl.h>
#include <errno.h>
#include <utime.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/un.h>


#include <cutils/sockets.h>
#include <cutils/log.h>
#include <cutils/properties.h>

#include <private/android_filesystem_config.h>

#if INCLUDE_SYS_MOUNT_FOR_STATFS
#include <sys/mount.h>
#else
#include <sys/statfs.h>
#endif
