package info.lamatricexiste.network;

interface NetworkInterface {
    List<String> inGetHosts();
    void inSearchReachableHosts();
    void inSendPacket(in List<String> hosts, in boolean repeat);
    String inNetInfo();
}
