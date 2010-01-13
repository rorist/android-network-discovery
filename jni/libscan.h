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

char debug(char* str);
int CreateRawSocket(int protocol);
int SendRawPacket(int rawsock, unsigned char *pkt, int pkt_len);
int BindRawSocketToInterface(char *device, int rawsock, int protocol);
int scan();
