/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

/**
 * Java NIO Documentation:
 * http://jfarcand.wordpress.com/2006/05/30/tricks-and-tips-with-nio-part-i-why-you-must-handle-op_write
 * http://jfarcand.wordpress.com/2006/06/06/tricks-and-tips-with-nio-part-ii-why-selectionkeyattach-evil
 * http://jfarcand.wordpress.com/2006/07/07/tricks-and-tips-with-nio-part-iii-thread-or-not-thread
 * http://jfarcand.wordpress.com/2006/07/19/tricks-and-tips-with-nio-part-iv-meet-selectors
 * http://jfarcand.wordpress.com/2006/09/21/tricks-and-tips-with-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
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
import android.util.SparseArray;

public class DefaultPortscan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "DefaultPortscan";
    private final int MAX_READ = 75;
    private final int TIMEOUT_SELECT = 300; // milliseconds
    private final long TIMEOUT_READ = 1500;
    private int cnt_selected;
    private long timeout = 0;
    private long time;
    private Selector connSelector = null;
    private Selector readSelector = null;

    protected WeakReference<Activity> mActivity;
    protected String[] mBanners = null;
    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;

    protected DefaultPortscan(Activity activity, String host, final int rate) {
        mActivity = new WeakReference<Activity>(activity);
        this.ipAddr = host;
        // From milliseconds to nanoseconds
        this.timeout = (long) (rate * 1000 * 1000);
        Log.v(TAG, "timeout=" + rate + "ms");
    }

    protected Void doInBackground(Void... params) {
        try {
            int step = 127;
            InetAddress ina = InetAddress.getByName(ipAddr);
            if (nb_port > step) {
                // FIXME: Selector leaks file descriptors (Dalvik bug)
                // http://code.google.com/p/android/issues/detail?id=4825
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    time = System.nanoTime();
                    start(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                time = System.nanoTime();
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
        // Register the Channel with port as attachement
        SparseArray<Integer> data = new SparseArray<Integer>(1);
        data.append(0, port);
        socket.register(connSelector, SelectionKey.OP_CONNECT, data);
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
                final Activity d = mActivity.get();
                if (d != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d
                            .getApplicationContext());
                    if (prefs.getBoolean(Prefs.KEY_BANNER, Prefs.DEFAULT_BANNER)) {
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
                        // Create a new selector and register for reading
                        SelectionKey tmpKey = ((SocketChannel) key.channel()).register(
                                readSelector, SelectionKey.OP_READ);
                        tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                        int code = readSelector.select(TIMEOUT_READ);
                        tmpKey.interestOps(tmpKey.interestOps() & (~SelectionKey.OP_READ));
                        if (code != 0) {
                            handleRead(tmpKey, ((SparseArray<Integer>) key.attachment()).get(0));
                            time = System.nanoTime(); // Reset selector timeout
                            finishKey(key);
                            return;
                        }
                        time = System.nanoTime();
                        finishKey(tmpKey);
                    }
                }
                publishProgress(((SparseArray<Integer>) key.attachment()).get(0), (int) 1);
                finishKey(key);
            }
        } catch (IOException e) { // Closed
            publishProgress(((SparseArray<Integer>) key.attachment()).get(0), (int) 0);
            finishKey(key);
        }
    }

    private void handleRead(SelectionKey key, int port) {
        ByteBuffer bbuf = ByteBuffer.allocate(MAX_READ);
        int numRead = 0;
        try {
            // while (numRead > 0) {
            numRead = ((SocketChannel) key.channel()).read(bbuf);
            // }
            if (numRead > 8) {
                mBanners[port] = new String(bbuf.array()).substring(0, numRead).trim();
            }
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
