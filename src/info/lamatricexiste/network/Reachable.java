package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {
    
    private int[] ports = {135,139,445,554,2869,5357,10243,80,8080,443,21,22};
    private final int timeout = 100;
    
    public Boolean request(InetAddress host) {
        int i = 0;
        while(i<ports.length){
            int port = ports[i];
            Socket s = new Socket();
            try {
                s.bind(null);
                s.connect(new InetSocketAddress(host, port), timeout);
                s.close();
                return true;
            } catch (IOException e) {}
            i++;
        }
        return false;
    }
}
