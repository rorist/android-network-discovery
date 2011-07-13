/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

/**
 * Java NIO Documentation:
 * http://jfarcand.wordpress.com/2006/05/30/tricks-and-tips-with-nio-part-i-why-you-must-handle-op_write
 * http://jfarcand.wordpress.com/2006/07/06/tricks-and-tips-with-nio-part-ii-why-selectionkey-attach-is-evil/ 
 * http://jfarcand.wordpress.com/2006/07/07/tricks-and-tips-with-nio-part-iii-to-thread-or-not-to-thread/
 * http://jfarcand.wordpress.com/2006/07/19/httpweblogs-java-netblog20060719tricks-and-tips-nio-part-iv-meet-selectors/
 * http://jfarcand.wordpress.com/2006/09/21/tricks-and-tips-with-nio-part-v-ssl-and-nio-friend-or-foe
 * http://svn.apache.org/viewvc/mina/trunk/core/src/main/java/org/apache/mina/transport/socket/nio/
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class AsyncPortscan extends AsyncTask<Void, Object, Void> {

    private final String TAG = "AsyncPortscan";
    private static final int TIMEOUT_SELECT = 300;
    private static long TIMEOUT_CONNECT = 1000 * 1000000; // ns
    private static final long TIMEOUT_RW = 3 * 1000 * 1000000; // ns
    private static final String E_REFUSED = "Connection refused";
    private static final String E_TIMEOUT = "The operation timed out";
    // TODO: Probe system to send other stuff than strings
    private static final String[] PROBES = new String[] { "", "\r\n\r\n", "GET / HTTP/1.0\r\n\r\n" };
    private static final int MAX_READ = 8 * 1024;
    private static final int WRITE_PASS = PROBES.length;
    private static final long WRITE_COOLDOWN = 200 * 1000000; // ns

    private int rate;
    private boolean select = true;
    private Selector selector;

    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;
    private boolean getBanner = false;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_READ);
    private Charset charset = Charset.forName("UTF-8");

    public final static int OPEN = 0;
    public final static int CLOSED = 1;
    public final static int FILTERED = -1;
    public final static int UNREACHABLE = -2;
    public final static int TIMEOUT = -3;

    protected AsyncPortscan(Activity activity, String host, int _rate) {
        ipAddr = host;
        rate = _rate;
        TIMEOUT_CONNECT = rate * 1000000; // ms to ns

        // Preferences
        WeakReference<Activity> mActivity = new WeakReference<Activity>(activity);
        final Activity d = mActivity.get();
        if (d != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d
                    .getApplicationContext());
            getBanner = prefs.getBoolean(Prefs.KEY_BANNER, Prefs.DEFAULT_BANNER);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            int step = 127;
            InetAddress ina = InetAddress.getByName(ipAddr);
            if (nb_port > step) {
                // FIXME: Selector leaks file descriptors (Dalvik bug)
                // http://code.google.com/p/android/issues/detail?id=4825
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    if (select) {
                        start(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                    }
                }
            } else {
                start(ina, port_start, port_end);
            }

        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
            publishProgress(0, UNREACHABLE, null);
        }
        return null;
    }

    private void start(final InetAddress ina, final int PORT_START, final int PORT_END) {
        select = true;
        try {
            selector = Selector.open();
            for (int j = PORT_START; j <= PORT_END; j++) {
                connectSocket(ina, j);
            }
            while (select && selector.keys().size() > 0) {
                if (selector.select(TIMEOUT_SELECT) > 0) {
                    synchronized (selector.selectedKeys()) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            try {
                                if (!key.isValid()) {
                                    continue;
                                }
                                // States
                                final Data data = (Data) key.attachment();

                                if (key.isConnectable()) {
                                    if (((SocketChannel) key.channel()).finishConnect()) {
                                        if (getBanner) {
                                            key.interestOps(SelectionKey.OP_READ
                                                    | SelectionKey.OP_WRITE);
                                            data.state = OPEN;
                                            data.start = System.nanoTime();
                                            publishProgress(data.port, OPEN, null);
                                        } else {
                                            finishKey(key, OPEN);
                                        }
                                    }

                                } else if (key.isReadable()) {
                                    try {
                                        byteBuffer.clear();
                                        final int numRead = ((SocketChannel) key.channel())
                                                .read(byteBuffer);
                                        if (numRead > 0) {
                                            String banner = new String(byteBuffer.array())
                                                    .substring(0, numRead).trim();
                                            // Log.v(TAG, "read " + data.port +
                                            // " data=" + banner);
                                            finishKey(key, OPEN, banner);
                                        } else {
                                            key.interestOps(SelectionKey.OP_WRITE);
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                    }
                                } else if (key.isWritable()) {
                                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    if (System.nanoTime() - data.start > WRITE_COOLDOWN) {
                                        if (data.pass < WRITE_PASS) {
                                            // Log.v(TAG, "write " + data.port);
                                            // write something (blocking)
                                            final ByteBuffer bytedata = charset
                                                    .encode(PROBES[data.pass]);
                                            final SocketChannel sock = (SocketChannel) key
                                                    .channel();
                                            while (bytedata.hasRemaining()) {
                                                sock.write(bytedata);
                                            }
                                            bytedata.clear();
                                            data.start = System.nanoTime();
                                            data.pass++;
                                        } else {
                                            finishKey(key, OPEN);
                                        }
                                    }
                                }

                            } catch (ConnectException e) {
                                if (e.getMessage().equals(E_REFUSED)) {
                                    finishKey(key, CLOSED);
                                } else if (e.getMessage().equals(E_TIMEOUT)) {
                                    finishKey(key, FILTERED);
                                } else {
                                    Log.e(TAG, e.getMessage());
                                    e.printStackTrace();
                                    finishKey(key, FILTERED);
                                }
                            } catch (Exception e) {
                                try {
                                    Log.e(TAG, e.getMessage());
                                } catch (java.lang.NullPointerException e1) {
                                    e1.printStackTrace();
                                } finally {
                                    e.printStackTrace();
                                    finishKey(key, FILTERED);
                                }
                            } finally {
                                iterator.remove();
                            }
                        }
                    }
                } else {
                    // Remove old/non-connected keys
                    final long now = System.nanoTime();
                    final Iterator<SelectionKey> iterator = selector.keys().iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = (SelectionKey) iterator.next();
                        final Data data = (Data) key.attachment();
                        if (data.state == OPEN && now - data.start > TIMEOUT_RW) {
                            Log.e(TAG, "TIMEOUT=" + data.port);
                            finishKey(key, TIMEOUT);
                        } else if (data.state != OPEN && now - data.start > TIMEOUT_CONNECT) {
                            finishKey(key, TIMEOUT);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeSelector();
        }
    }

    private void connectSocket(InetAddress ina, int port) {
        // Create the socket
        try {
            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(ina, port));
            Data data = new Data();
            data.port = port;
            data.start = System.nanoTime();
            socket.register(selector, SelectionKey.OP_CONNECT, data);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    protected void onCancelled() {
        select = false;
    }

    private void closeSelector() {
        try {
            if (selector.isOpen()) {
                synchronized (selector.keys()) {
                    Iterator<SelectionKey> iterator = selector.keys().iterator();
                    while (iterator.hasNext()) {
                        finishKey((SelectionKey) iterator.next(), FILTERED);
                    }
                    selector.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (ClosedSelectorException e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void finishKey(SelectionKey key, int state) {
        finishKey(key, state, null);
    }

    private void finishKey(SelectionKey key, int state, String banner) {
        synchronized (key) {
            if(key == null || !key.isValid()){
                return;
            }
            closeChannel(key.channel());
            Data data = (Data) key.attachment();
            publishProgress(data.port, state, banner);
            key.attach(null);
            key.cancel();
            key = null;
        }
    }

    private void closeChannel(SelectableChannel channel) {
        if (channel instanceof SocketChannel) {
            Socket socket = ((SocketChannel) channel).socket();
            try{
                if (!socket.isInputShutdown()) socket.shutdownInput();
            } catch (IOException ex){
            }
            try{
                if (!socket.isOutputShutdown()) socket.shutdownOutput();
            } catch (IOException ex){
            }
            try{
                socket.close();
            } catch (IOException ex){
            }
        }
        try{
            channel.close();
        } catch (IOException ex){
        }
    }

    // Port private object
    private static class Data {
        protected int state = FILTERED;
        protected int port;
        protected long start;
        protected int pass = 0;
    }
}
