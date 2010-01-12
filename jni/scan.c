#include <string.h>
#include <jni.h>
#include <android/log.h>

#include<stdio.h>
#include<stdlib.h>
#include<sys/socket.h>
#include<features.h>
#include<linux/if_packet.h>
#include<linux/if_ether.h>
#include<errno.h>
#include<sys/ioctl.h>
#include<net/if.h>
#include <endian.h>

#define PACKET_LENGTH	1024

/**
 * Functions
 */

char debug(char* str){
    char errbuf [50];
    sprintf(errbuf, "%s: %s", str, strerror(errno));
    __android_log_write(ANDROID_LOG_ERROR,"SocketTest", errbuf);
}

int CreateRawSocket(int protocol) {
    int rawsock;
    if((rawsock = socket(PF_PACKET, SOCK_RAW, htons(protocol)))==-1) {
        debug("Error creating raw socket");
        exit(-1);
    }
    return rawsock;
}

int SendRawPacket(int rawsock, unsigned char *pkt, int pkt_len) {
    int sent = 0;
    if((sent = write(rawsock, pkt, pkt_len)) != pkt_len){
        return 0;
    }
    return 1;
}

int BindRawSocketToInterface(char *device, int rawsock, int protocol) {
    struct sockaddr_ll sll;
    struct ifreq ifr;

    bzero(&sll, sizeof(sll));
    bzero(&ifr, sizeof(ifr));
    
    strncpy((char *)ifr.ifr_name, device, IFNAMSIZ);
    if((ioctl(rawsock, SIOCGIFINDEX, &ifr)) == -1) {
        debug("Error getting Interface index");
        exit(-1);
    }

    sll.sll_family = AF_PACKET;
    sll.sll_ifindex = ifr.ifr_ifindex;
    sll.sll_protocol = htons(protocol); 

    if((bind(rawsock, (struct sockaddr *)&sll, sizeof(sll)))== -1) {
        debug("Error binding raw socket to interface");
        exit(-1);
    }
    return 1;
}

int start() 
{
    int raw;
    unsigned char packet[PACKET_LENGTH];
    int num_of_pkts = 1;
    
    memset(packet, 'A', PACKET_LENGTH);
    
    raw = CreateRawSocket(ETH_P_ALL);
    
    BindRawSocketToInterface("tiwlan0", raw, ETH_P_ALL);
    
    while((num_of_pkts--)>0){
        if(!SendRawPacket(raw, packet, PACKET_LENGTH)) {
            debug("Error sending packet");
        }
    }
    
    close(raw);
    
    return 0; 
} 

void Java_info_lamatricexiste_network_Utils_NativeTask_socket( JNIEnv* env, jobject thiz )
{
    start();
}

int main(const int argc, const char *argv[]) {    
    char buf[BUFFER_MAX];
    struct sockaddr addr;
    socklen_t alen;
    int lsocket, s, count;

    lsocket = android_get_control_socket(SOCKET_PATH);
    if (lsocket < 0) {
        LOGE("Failed to get socket from environment: %s\n", strerror(errno));
        exit(1);
    }
    if (listen(lsocket, 5)) {
        LOGE("Listen on socket failed: %s\n", strerror(errno));
        exit(1);
    }
    fcntl(lsocket, F_SETFD, FD_CLOEXEC);

    for (;;) {
        alen = sizeof(addr);
        s = accept(lsocket, &addr, &alen);
        if (s < 0) {
            LOGE("Accept failed: %s\n", strerror(errno));
            continue;
        }
        fcntl(s, F_SETFD, FD_CLOEXEC);

        LOGI("new connection\n");
        for (;;) {
            unsigned short count;
            if (readx(s, &count, sizeof(count))) {
                LOGE("failed to read size\n");
                break;
            }
            if ((count < 1) || (count >= BUFFER_MAX)) {
                LOGE("invalid size %d\n", count);
                break;
            }
            if (readx(s, buf, count)) {
                LOGE("failed to read command\n");
                break;
            }
            buf[count] = 0;
            if (execute(s, buf)) break;
        }
        LOGI("closing connection\n");
        close(s);
    }

    return 0;
}
