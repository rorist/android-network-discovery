package info.lamatricexiste.smbpoc;

interface NetworkInterface {
    List<String> inGetReachableHosts();
    String inGetIp();
    String inGetIpNet();
    String inGetIpBc();
}
