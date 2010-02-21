#include "discover.h"

// Using PF_PACKET interface (OSI Level 2)

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

int do_discover(char **arg, char reply[REPLY_MAX])
{
    int raw;
    unsigned char packet[PACKET_LENGTH];
    int num_of_pkts = 1;
    char *interface = arg[0];
    char *start = arg[1];
    char *end = arg[2];
    
    memset(packet, 'A', PACKET_LENGTH);
    
    raw = CreateRawSocket(ETH_P_ALL);
    
    BindRawSocketToInterface(interface, raw, ETH_P_ALL);
    
    while((num_of_pkts--)>0){
        if(!SendRawPacket(raw, packet, PACKET_LENGTH)) {
            LOGE("Error sending packet");
        }
    }
    
    close(raw);
    
    return 0; 
}

void inject_ip_packet(){
    
}

unsigned char* CreateEthernetHeader(char *src_mac, char *dst_mac, int protocol)
{
    struct ethhdr *ethernet_header;
    
    ethernet_header = (struct ethhdr *)malloc(sizeof(struct ethhdr));

    /* copy the Src mac addr */
    memcpy(ethernet_header->h_source, (void *)ether_aton(src_mac), 6);

    /* copy the Dst mac addr */
    memcpy(ethernet_header->h_dest, (void *)ether_aton(dst_mac), 6);

    /* copy the protocol */
    ethernet_header->h_proto = htons(protocol);

    /* done ...send the header back */
    return ((unsigned char *)ethernet_header);
}

/* Ripped from Richard Stevans Book */

unsigned short ComputeIpChecksum(unsigned char *header, int len)
{
    long sum = 0;  /* assume 32 bit long, 16 bit short */
    unsigned short *ip_header = (unsigned short *)header;

    while(len > 1){
        sum += *((unsigned short*) ip_header)++;
        if(sum & 0x80000000)   /* if high order bit set, fold */
          sum = (sum & 0xFFFF) + (sum >> 16);
        len -= 2;
    }

    if(len)       /* take care of left over byte */
        sum += (unsigned short) *(unsigned char *)ip_header;
     
    while(sum>>16)
        sum = (sum & 0xFFFF) + (sum >> 16);

   return ~sum;
}


unsigned char *CreateIPHeader(/* Customize this */)
{
    struct iphdr *ip_header;

    ip_header = (struct iphdr *)malloc(sizeof(struct iphdr));

    ip_header->version = 4;
    ip_header->ihl = (sizeof(struct iphdr))/4 ;
    ip_header->tos = 0;
    ip_header->tot_len = htons(sizeof(struct iphdr));
    ip_header->id = htons(111);
    ip_header->frag_off = 0;
    ip_header->ttl = 111;
    ip_header->protocol = IPPROTO_TCP;
    ip_header->check = 0; /* We will calculate the checksum later */
    ip_header->saddr = inet_addr(SRC_IP);
    ip_header->daddr = inet_addr(DST_IP);


    /* Calculate the IP checksum now : 
       The IP Checksum is only over the IP header */

    ip_header->check = ComputeIpChecksum((unsigned char *)ip_header, ip_header->ihl*4);

    return ((unsigned char *)ip_header);

}
