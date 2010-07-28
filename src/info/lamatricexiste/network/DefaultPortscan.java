/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

/**
 * Java NIO Documentation:
 * http://jfarcand.wordpress.com/2006/05/30/tricks-and-tips-with-nio-part-i-why-you-must-handle-op_write
 * http://jfarcand.wordpress.com/2006/07/06/tricks-and-tips-with-nio-part-ii-why-selectionkey-attach-is-evil/ 
 * http://jfarcand.wordpress.com/2006/07/07/tricks-and-tips-with-nio-part-iii-thread-or-not-thread
 * http://jfarcand.wordpress.com/2006/07/19/tricks-and-tips-with-nio-part-iv-meet-selectors
 * http://jfarcand.wordpress.com/2006/09/21/tricks-and-tips-with-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
import java.lang.StringIndexOutOfBoundsException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class DefaultPortscan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "DefaultPortscan";
    private final int MAX_READ = 75;
    private final int TIMEOUT_SELECT = 300; // milliseconds
    private final long TIMEOUT_READ = 1500;
    private boolean getBanner = false;
    private int cnt_selected;
    private long timeout = 0;
    private long time;
    private Selector connSelector = null;
    private Selector readSelector = null;

    protected String[] mBanners = null;
    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;

    protected DefaultPortscan(Activity activity, String host, int rate) {
        WeakReference<Activity> mActivity = new WeakReference<Activity>(activity);
        final Activity d = mActivity.get();
        if (d != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d.getApplicationContext());
            getBanner = prefs.getBoolean(Prefs.KEY_BANNER, Prefs.DEFAULT_BANNER);
        }
        this.ipAddr = host;
        // Not less than 100ms
        if (rate < 100) {
            rate = 100;
        }
        // From milliseconds to nanoseconds
        this.timeout = (long) (rate * 1000 * 1000);
        Log.i(TAG, "timeout=" + rate + "ms");
    }

    protected Void doInBackground(Void... params) {
        try {
            int step = 127;
            InetAddress ina = InetAddress.getByName(ipAddr);
            if (nb_port > step) {
                // FIXME: Selector leaks file descriptors (Dalvik bug)
                // http://code.google.com/p/android/issues/detail?id=4825
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    start(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                start(ina, port_start, port_end);
            }
        } catch (UnknownHostException e) {
            publishProgress((int) -1, (int) -1);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stop();
        }
        return null;
    }

    protected void cancelTimeouts() throws IOException {
        if ((System.nanoTime() - time) > timeout) {
            stop();
        }
    }

    protected void onCancelled() {
        stop();
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
        cnt_selected = 0;
        connSelector = Selector.open();
        readSelector = Selector.open();
        for (int i = PORT_START; i <= PORT_END; i++) {
            connectSocket(ina, i);
            // Thread.sleep(timeout);
        }
        time = System.nanoTime();
        doSelect(PORT_END - PORT_START);
    }

    protected void stop() {
        stopSelector(connSelector);
        stopSelector(readSelector);
    }

    private void stopSelector(Selector selector) {
        if (selector != null && selector.isOpen()) {
            synchronized (selector) {
                try {
                    // Force invalidate keys
                    Iterator<SelectionKey> iterator = selector.keys().iterator();
                    synchronized (iterator) {
                        while (iterator.hasNext()) {
                            publishProgress(0, -2);
                            finishKey(iterator.next());
                        }
                    }
                    // Close the selector
                    selector.close();
                } catch (ClosedSelectorException e) {
                    Log.e(TAG, "ClosedSelectorException: " + selector.toString());
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                } catch (NullPointerException e) {
                    // FIXME: Bug in 2.2 Froyo
                    // http://code.google.com/p/android/issues/detail?id=9431
                    Log.e(TAG, "IPv6 not supported, so java.nio.channels.Selector crashes");
                }
            }
        }
    }

    private void connectSocket(InetAddress ina, int port) throws IOException {
        // Create the socket
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(new InetSocketAddress(ina, port));
        socket.register(connSelector, SelectionKey.OP_CONNECT, new Integer(port));
    }

    private void doSelect(final int NB) {
        try {
            while (connSelector.isOpen()) {
                connSelector.select(TIMEOUT_SELECT);
                Iterator<SelectionKey> iterator = connSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (key.isValid() && key.isConnectable()) {
                        handleConnect(key);
                    }
                }
                cancelTimeouts(); // Filtered or Unresponsive
                if (cnt_selected >= NB) {
                    stop();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stop();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConnect(SelectionKey key) {
        try {
            if (((SocketChannel) key.channel()).finishConnect()) { // Open
                if (getBanner) {
                    // TODO: Send a Probe before reading !
                    // SelectionKey tmpKey = ((SocketChannel)
                    // key.channel()).register(
                    // readSelector, SelectionKey.OP_WRITE);
                    // tmpKey.interestOps(tmpKey.interestOps() |
                    // SelectionKey.OP_WRITE);
                    // int code = readSelector.select(TIMEOUT_READ);
                    // tmpKey.interestOps(tmpKey.interestOps() &
                    // (~SelectionKey.OP_WRITE));
                    // if (code != 0) {
                    // ByteBuffer data =
                    // Charset.forName("ISO-8859-1").encode(
                    // "GET / HTTP/1.0\r\n\r\n");
                    // SocketChannel sock = (SocketChannel)
                    // tmpKey.channel();
                    // while (data.hasRemaining()) {
                    // sock.write(data);
                    // }
                    // data.clear();
                    // Log.v(TAG, "writing ...");
                    // }

                    // Register for reading on the read selector
                    SelectionKey tmpKey = ((SocketChannel) key.channel()).register(
                            readSelector, SelectionKey.OP_READ);
                    tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                    int code = readSelector.select(TIMEOUT_READ);
                    tmpKey.interestOps(tmpKey.interestOps() & (~SelectionKey.OP_READ));
                    if (code != 0) {
                        handleRead(tmpKey, (Integer) key.attachment());
                        time = System.nanoTime(); // Reset selector timeout
                        finishKey(key);
                        return;
                    }
                    time = System.nanoTime();
                    finishKey(tmpKey);
                }
                publishProgress((Integer) key.attachment(), (int) 1);
                finishKey(key);
            }
        } catch (IOException e) { // Closed
            publishProgress((Integer) key.attachment(), (int) 0);
            finishKey(key);
        }
    }

    private void handleRead(SelectionKey key, final int port) {
        ByteBuffer bbuf = ByteBuffer.allocate(MAX_READ);
        int numRead = 0;
        try {
            // while (numRead > 0) {
            numRead = ((SocketChannel) key.channel()).read(bbuf);
            // }
            if (numRead > 8) {
                mBanners[port] = new String(bbuf.array()).substring(0, numRead).trim();
            }
        } catch (StringIndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            publishProgress(port, (int) 1);
            finishKey(key);
            cnt_selected--; // Hack for finishKey();
        }
    }

    private void finishKey(SelectionKey key) {
        synchronized (key) {
            try {
                ((SocketChannel) key.channel()).close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (ConcurrentModificationException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                key.cancel();
                cnt_selected++;
            }
        }
    }
}
