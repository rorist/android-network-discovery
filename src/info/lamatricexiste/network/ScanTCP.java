package info.lamatricexiste.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Observable;

import android.util.Log;

public class ScanTCP extends Observable implements Runnable
{
    private final String TAG       =  "ScanTCP";
    private final int TIMEOUT      =  60;
    private final int MAX_CLOSED   =  1;
    private final int MAX_FILTERED =  2;
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
        Socket s = new Socket();
        try {
            InetSocketAddress addr = new InetSocketAddress(ip, port);
            s.connect(addr, TIMEOUT);
            state = "open";
        }
        catch (SocketTimeoutException e){
            setFiltered();
//            Log.e(TAG, port+": "+e.toString());
        }
        catch (ConnectException e){
            setClosed();
//            Log.e(TAG, port+": "+e.toString());
        }
        catch (IOException e) {
            setClosed();
//            Log.e(TAG, port+": "+e.toString());
        }
        finally {
            try {
                if(s!=null){
                    s.close();
                }
            } catch (IOException e) {
                Log.e(TAG, port+": "+e.toString());
            }
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
