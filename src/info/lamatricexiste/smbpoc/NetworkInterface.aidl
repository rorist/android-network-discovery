package info.lamatricexiste.smbpoc;

interface NetworkInterface {
    List<String> inGetReachableHosts();
    List<String> SendPacket();
    String inGetIp();
    String inGetIpNet();
    String inGetIpBc();
}
