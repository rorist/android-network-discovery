//#include <string.h>
//#include <jni.h>

#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <cutils/sockets.h>
#include <cutils/log.h>
#include <cutils/properties.h>
#include <linux/if_ether.h>
#include <linux/if.h>
#include <linux/if_packet.h>

#define LOG_TAG       "scand"
#define PACKET_LENGTH 1024

int discover(char *interface);
int CreateRawSocket(int protocol);
int SendRawPacket(int rawsock, unsigned char *pkt, int pkt_len);
int BindRawSocketToInterface(char *device, int rawsock, int protocol);
