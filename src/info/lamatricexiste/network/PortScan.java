package info.lamatricexiste.network;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
    
public class PortScan implements Observer {
	
//	private final String TAG = "PortScan";
    private final int    PORT_START =  1;
    private final int    PORT_END   =  1024;
	ArrayList<CharSequence> result = new ArrayList<CharSequence>();
	
	public CharSequence[] scan(String host){
        // http://www.iana.org/assignments/port-numbers

        //ScanTCP
        for(int i=PORT_START; i<=PORT_END; i++){
        	ScanTCP s = new ScanTCP(host, i);
        	s.addObserver(this);
        	Thread scanPortThread = new Thread(s);
        	scanPortThread.setPriority(Thread.MAX_PRIORITY);
        	scanPortThread.start();
        }

		//ScanUDP

		return result.toArray(new CharSequence[result.size()]);
	}

	public void update(Observable observable, Object data) {
		if(observable instanceof ScanTCP){
			String state = (String)data;
			if(state.equals("open")){
				ScanTCP s = (ScanTCP)observable;
				int port = s.getPort();
				result.add(port+"/tcp "+state);
			}
		}
	}
}
