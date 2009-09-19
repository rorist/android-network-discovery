package info.lamatricexiste.network;

interface NetworkInterface {
    List<String> inGetHosts();
    void inSearchReachableHosts();
    void inSendPacket(boolean repeat);
    String inGetIp();
    String inGetIpNet();
    String inGetIpBc();
}
