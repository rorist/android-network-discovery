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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.ClosedSelectorException;
import java.util.Iterator;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncPortscan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "AsyncPortscan";
    private final int TIMEOUT_SELECT = 300;
    private final int TIMEOUT_CONNECT = 1500;
    private int cnt_selected = 0;
    private long time;
    private boolean select = true;
    private Selector selector;

    protected String[] mBanners = null;
    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;
    
    public final static int OPEN = 0;
    public final static int CLOSED = 1;
    public final static int FILTERED = -1;
    public final static int UNREACHABLE = -2;

    protected AsyncPortscan(Activity activity, String host, int rate) {
        ipAddr = host;
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
                    start(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                start(ina, port_start, port_end);
            }

        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
            publishProgress(0, UNREACHABLE);
        }
        return null;
    }

    private void start(InetAddress ina, final int PORT_START, final int PORT_END) {
        select = true;
        long size = PORT_END - PORT_START;
        try {
            selector = Selector.open();
            for (int j = PORT_START; j <= PORT_END; j++) {
                connectSocket(ina, j);
            }
            time = System.nanoTime();
            while(select && selector.keys().size() > 0) {
                synchronized (selector) {
                    if (selector.select(TIMEOUT_SELECT) > 0) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            try {
                                if (!key.isValid()) {
                                    continue;
                                }
                                if (key.isConnectable()) {
                                    //Log.i(TAG, "connectable=" + (Integer) key.attachment());
                                    if (((SocketChannel) key.channel()).finishConnect()) {
                                        Log.i(TAG, "connected=" + (Integer) key.attachment());
                                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    }
                                } else if (key.isWritable()) {
                                    Log.i(TAG, "writable=" + (Integer) key.attachment());
                                    // write something
                                    ByteBuffer data = Charset.forName("ISO-8859-1").encode("asd\r\n\r\n");
                                    SocketChannel sock = (SocketChannel) key.channel();
                                    while (data.hasRemaining()) {
                                        sock.write(data);
                                    }
                                    data.clear();
                                    key.interestOps(SelectionKey.OP_READ);
                                } else if (key.isReadable()) {
                                    Log.i(TAG, "readable=" + (Integer) key.attachment());
                                    finishKey(key, OPEN);
                                }
                            } catch (ConnectException e) {
                                if (e.getMessage().equals("Connection refused")) {
                                    finishKey(key, CLOSED);
                                } else if (e.getMessage().equals("The operation timed out")) {
                                    finishKey(key, FILTERED);
                                } else {
                                    Log.e(TAG, e.getMessage());
                                }
                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage());
                                finishKey(key, FILTERED);
                            } finally {
                                iterator.remove();
                            }
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
            socket.register(selector, SelectionKey.OP_CONNECT, new Integer(port));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

     @Override
    protected void onPostExecute(Void unused) {
        closeSelector();
        super.onPostExecute(unused);
    }

    protected void onCancelled() {
        closeSelector();
    }

    private void closeSelector(){
        select = false;
        synchronized (selector) {
            try {
                if (selector.isOpen()) {
                    Iterator<SelectionKey> iterator = selector.keys().iterator();
                    while (iterator.hasNext()) {
                        finishKey((SelectionKey) iterator.next(), FILTERED);
                    }
                    selector.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (ClosedSelectorException e) {
            }
        }
    }

    private void clearTimeout(){
        long now = System.nanoTime();
        if (now - time > TIMEOUT_CONNECT && selector.isOpen()) {
            Iterator<SelectionKey> iterator = selector.keys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.interestOps()==SelectionKey.OP_CONNECT) {
                    finishKey(key, FILTERED);
                }
            }
        }
    }

    private void finishKey(SelectionKey key, int state) {
        synchronized (key) {
            try {
                ((SocketChannel) key.channel()).close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                cnt_selected++;
                publishProgress((Integer) key.attachment(), state);
                key.cancel();
            }
        }
    }
}
