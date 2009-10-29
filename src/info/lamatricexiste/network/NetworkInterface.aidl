package info.lamatricexiste.network;

interface NetworkInterface {
    List<String> inGetHosts();
    void inSearchReachableHosts(in int method);
    void inSendPacket(in List<String> hosts, in int request, in boolean repeat);
}
