#include "discover.h"

int CreateRawSocket(int protocol) {
    int rawsock;
    if((rawsock = socket(PF_PACKET, SOCK_RAW, htons(protocol)))==-1) {
        LOGE("Error creating raw socket");
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
        LOGE("Error getting Interface index");
        exit(-1);
    }

    sll.sll_family = AF_PACKET;
    sll.sll_ifindex = ifr.ifr_ifindex;
    sll.sll_protocol = htons(protocol); 

    if((bind(rawsock, (struct sockaddr *)&sll, sizeof(sll)))== -1) {
        LOGE("Error binding raw socket to interface");
        exit(-1);
    }
    return 1;
}

int test()
{
    int raw;
    unsigned char packet[PACKET_LENGTH];
    int num_of_pkts = 1;
    
    memset(packet, 'A', PACKET_LENGTH);
    
    raw = CreateRawSocket(ETH_P_ALL);
    
    BindRawSocketToInterface("tiwlan0", raw, ETH_P_ALL);
    
    while((num_of_pkts--)>0){
        if(!SendRawPacket(raw, packet, PACKET_LENGTH)) {
            LOGE("Error sending packet");
        }
    }
    
    close(raw);
    
    return 0; 
}
