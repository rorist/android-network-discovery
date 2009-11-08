package info.lamatricexiste.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Observable;

public class ScanTCP extends Observable implements Runnable
{
//    private final String TAG       =  "ScanTCP";
    private final int TIMEOUT      =  100;
    private final int MAX_CLOSED   =  2;
    private final int MAX_FILTERED =  1;
    private String    state        =  "";
    private String    ip           =  "";
    private int       port         =  0;
    private int       cnt_closed   =  0;
    private int       cnt_filtered =  0;
    
    public ScanTCP(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public void run() {
        scan();
        setChanged();
        notifyObservers(state);
        Thread.currentThread().interrupt();
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
//        catch (Exception e) {
//        	e.printStackTrace();
//        }
        catch (ConnectException e){
        	setFiltered();
        }
        catch (SocketTimeoutException e){
        	setFiltered();
        }
        catch (IOException e) {
        	setClosed();
        }
    }
    
    private void setFiltered(){
    	cnt_filtered++;
    	if(cnt_filtered < MAX_FILTERED){
    		this.scan();
    	}
    	else {
            state = "filtered";
    	}
    }
    
    private void setClosed(){
    	cnt_closed++;
    	if(cnt_closed < MAX_CLOSED){
    		this.scan();
    	}
    	else {
    		state = "closed";
    	}
    }
}
