package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Observable;

public class ScanTCP extends Observable implements Runnable
{
//    private final String TAG       =  "ScanTCP";
    private final int   TIMEOUT    =  100;
    private String		state = "";
    private String 		ip = "";
    private int 		port = 0;
    
    public ScanTCP(String ip, int port){
    	this.ip = ip;
    	this.port = port;
    }

	public void run() {
		scan();
		setChanged();
		notifyObservers(state);
	}
	
	public int getPort(){
		return this.port;
	}

    protected void scan(){
        try {
            Socket s = new Socket();
            s.bind(null);
            s.connect(new InetSocketAddress(ip, port), TIMEOUT);
            s.close();
            state = "open";
        }
        catch (SocketTimeoutException e){
        	state = "filtered";
        }
        catch (IOException e) {
        	state = "closed";
        }
    }
}
