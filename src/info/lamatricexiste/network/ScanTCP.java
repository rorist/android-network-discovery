package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ScanTCP {
	// private final String TAG = "ScanTCP";
	// private final int TIMEOUT = 2000;
	// private final int MAX_CLOSED = 1;
	// private final int MAX_FILTERED = 1;
	// private int cnt_closed = 0;
	// private int cnt_filtered = 0;
	private String ip = "";
	private int port = 0;

	public ScanTCP(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	// public void run() {
	// scan();
	// }

	public int getPort() {
		return this.port;
	}

	protected SocketChannel getSocket() throws IOException {
		SocketChannel socket = null;
		socket = SocketChannel.open();
		socket.configureBlocking(false);
		socket.connect(new InetSocketAddress(ip, port));
		return socket;

		// setChanged();
		// SocketChannel chan = null;
		// try {
		// InetSocketAddress addr = new InetSocketAddress(ip, port);
		// chan = SocketChannel.open(addr);
		// notifyObservers(port + "/tcp open");
		// } catch (Exception e) {
		// Log.e(TAG, port + ": " + e.toString());
		// setClosed();
		// } finally {
		// if (chan != null) {
		// try {
		// chan.close();
		// } catch (IOException e) {
		// Log.e(TAG, "FIXME: " + port + ": " + e.toString());
		// }
		// }
		// notifyObservers();
		// }

		// Socket s = new Socket();
		// InetSocketAddress addr = new InetSocketAddress(ip, port);
		// setChanged();
		// try {
		// s.connect(addr, TIMEOUT);
		// notifyObservers(port + "/tcp open");
		// } catch (SocketTimeoutException e) {
		// setFiltered();
		// } catch (ConnectException e) {
		// setClosed();
		// } catch (IOException e) {
		// setClosed();
		// } catch (Exception e) {
		// Log.e(TAG, "FIXME: " + port + ": " + e.toString());
		// } finally {
		// try {
		// if (s != null) {
		// s.close();
		// }
		// } catch (IOException e) {
		// Log.e(TAG, "FIXME: " + port + ": " + e.toString());
		// }
		// notifyObservers();
		// }
	}

	// private void setFiltered() {
	// cnt_filtered++;
	// if (cnt_filtered < MAX_FILTERED) {
	// this.scan();
	// }
	// }
	//
	// private void setClosed() {
	// cnt_closed++;
	// if (cnt_closed < MAX_CLOSED) {
	// this.scan();
	// }
	// }
}
