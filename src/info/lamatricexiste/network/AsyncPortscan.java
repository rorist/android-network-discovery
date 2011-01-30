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
 * http://svn.apache.org/viewvc/mina/trunk/core/src/main/java/org/apache/mina/transport/socket/nio/
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncPortscan extends AsyncTask<Void, Object, Void> {

    private final String TAG = "AsyncPortscan";
    private static final int TIMEOUT_SELECT = 300;
    private static final long TIMEOUT_CONNECT = 5000000000l; // ns - 5s
    private static final int TIMEOUT_WRITE = 5000; // ms
    private static final int TIMEOUT_READ = 5000; // ms
    private static final String E_REFUSED = "Connection refused";
    private static final String E_TIMEOUT = "The operation timed out";
    private int rate;
    private boolean select = true;
    private Selector selector;

    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;

    public final static int OPEN = 0;
    public final static int CLOSED = 1;
    public final static int FILTERED = -1;
    public final static int UNREACHABLE = -2;
    public final static int TIMEOUT = -3;

    protected AsyncPortscan(Activity activity, String host, int _rate) {
        ipAddr = host;
        rate = _rate;
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

    private void start(InetAddress ina, final int PORT_START, final int PORT_END) {
        select = true;
        try {
            selector = Selector.open();
            for (int j = PORT_START; j <= PORT_END; j++) {
                connectSocket(ina, j);
            }
            while (select && selector.keys().size() > 0) {
                Log.e(TAG, "select=" + selector.keys().size());
                if (selector.select(TIMEOUT_SELECT) > 0) {
                    synchronized (selector.selectedKeys()) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            Log.e(TAG, "key=" + key.isValid());
                            try {
                                if (!key.isValid()) {
                                    continue;
                                }
                                // States
                                if (key.isConnectable()) {
                                    // Log.v(TAG, "connectable=" + data.port);
                                    if (((SocketChannel) key.channel()).finishConnect()) {
                                        // Data data = (Data) key.attachment();
                                        // Log.v(TAG, "connected=" + data.port);
                                        // key.interestOps(SelectionKey.OP_READ
                                        // | SelectionKey.OP_WRITE);
                                        // key.interestOps(SelectionKey.OP_WRITE);
                                        finishKey(key, OPEN, "FIXME Banner");
                                    }
                                }
                                // } else if (key.isWritable()) {
                                // Log.v(TAG, "writable=" + data.port);
                                // // write something (blocking)
                                // // ByteBuffer data =
                                // //
                                // Charset.forName("ISO-8859-1").encode("asd\r\n\r\n");
                                // // SocketChannel sock = (SocketChannel)
                                // // key.channel();
                                // // while (data.hasRemaining()) {
                                // // sock.write(data);
                                // // }
                                // // data.clear();
                                // // key.interestOps(SelectionKey.OP_READ);
                                // finishKey(key, OPEN);
                                // } else if (key.isReadable()) {
                                // Log.v(TAG, "readable=" + data.port);
                                // finishKey(key, OPEN);
                                // }
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
                    // Remove old keys
                    final long now = System.nanoTime();
                    final Iterator<SelectionKey> iterator = selector.keys().iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = (SelectionKey) iterator.next();
                        if ((now - ((Data) key.attachment()).start) > TIMEOUT_CONNECT) {
                            finishKey(key, FILTERED);
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
        } finally {
            Log.v(TAG, "Selector closed");
        }
    }

    private void finishKey(SelectionKey key, int state) {
        finishKey(key, state, null);
    }

    private void finishKey(SelectionKey key, int state, String banner) {
        synchronized (key) {
            try {
                ((SocketChannel) key.channel()).close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                Data data = (Data) key.attachment();
                publishProgress(data.port, state, banner);
                key.cancel();
            }
        }
    }

    // Port private object
    private static class Data {
        public int port;
        public long start;
    }
}
