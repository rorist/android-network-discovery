package info.lamatricexiste.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Observable;

import android.util.Log;

public class ScanTCP extends Observable implements Runnable {
	private final String TAG = "ScanTCP";
	private final int TIMEOUT = 60;
	private final int MAX_CLOSED = 1;
	private final int MAX_FILTERED = 1;
	private String ip = "";
	private int port = 0;
	private int cnt_closed = 0;
	private int cnt_filtered = 0;

	public ScanTCP(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public void run() {
		scan();
	}

	public int getPort() {
		return this.port;
	}

	protected void scan() {
		Socket s = new Socket();
		InetSocketAddress addr = new InetSocketAddress(ip, port);
		setChanged();
		try {
			s.connect(addr, TIMEOUT);
			notifyObservers(port + "/tcp open");
		} catch (SocketTimeoutException e) {
			setFiltered();
		} catch (ConnectException e) {
			setClosed();
		} catch (IOException e) {
			setClosed();
		} catch (Exception e) {
			Log.e(TAG, "FIXME: " + port + ": " + e.toString());
		} finally {
			try {
				if (s != null) {
					s.close();
				}
			} catch (IOException e) {
				Log.e(TAG, "FIXME: " + port + ": " + e.toString());
			}
			notifyObservers(null);
		}
	}

	private void setFiltered() {
		cnt_filtered++;
		if (cnt_filtered < MAX_FILTERED) {
			this.scan();
		}
	}

	private void setClosed() {
		cnt_closed++;
		if (cnt_closed < MAX_CLOSED) {
			this.scan();
		}
	}
}
