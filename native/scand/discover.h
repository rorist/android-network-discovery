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
#define REPLY_MAX     256   /* largest reply allowed */

int do_discover(char **arg, char reply[REPLY_MAX]);
int CreateRawSocket(int protocol);
int SendRawPacket(int rawsock, unsigned char *pkt, int pkt_len);
int BindRawSocketToInterface(char *device, int rawsock, int protocol);

// IP INJECTION

#define SRC_ETHER_ADDR	"aa:aa:aa:aa:aa:aa"
#define DST_ETHER_ADDR	"bb:bb:bb:bb:bb:bb"
#define SRC_IP	"10.0.10.4"
#define DST_IP	"10.0.10.2"

#include<arpa/inet.h>
#include <net/if.h>
//#include <net/if_ether.h>
//#include<net/if_packet.h>
#include <netinet/in.h>
#include<linux/ip.h>

void inject_ip_packet();
unsigned char* CreateEthernetHeader(char *src_mac, char *dst_mac, int protocol);
unsigned short ComputeIpChecksum(unsigned char *header, int len);
unsigned char *CreateIPHeader();
