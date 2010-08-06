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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncPortscan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "AsyncPortscan";
    private final int TIMEOUT_SELECT = 300;
    private Selector selector;

    protected String[] mBanners = null;
    protected String ipAddr = null;
    protected int port_start = 0;
    protected int port_end = 0;
    protected int nb_port = 0;

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
        }
        return null;
    }

    private void start(InetAddress ina, final int PORT_START, final int PORT_END) {
        int cnt_selected = 0;
        long size = PORT_END - PORT_START;
        try {
            selector = Selector.open();
            for (int j = PORT_START; j <= PORT_END; j++) {
                connectSocket(ina, j);
            }
            while (selector.isOpen()) {
                if (selector.select(TIMEOUT_SELECT) > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = (SelectionKey) iterator.next();
                        try {
                            if (key.isValid()) {
                                if (key.isConnectable()) {
                                    Log.i(TAG, "connectable=" + (Integer) key.attachment());
                                    // TODO: Really use finishConnect or better
                                    // use OP_WRITE state ?
                                    if (((SocketChannel) key.channel()).finishConnect()) {
                                        cnt_selected++;
                                        publishProgress((Integer) key.attachment(), 1);
                                        Log.i(TAG, "connected=" + (Integer) key.attachment());
                                        key.interestOps(SelectionKey.OP_WRITE);
                                    }
                                } else if (key.isWritable()) {
                                    Log.i(TAG, "writable=" + (Integer) key.attachment());
                                    key.interestOps(SelectionKey.OP_READ);
                                } else if (key.isReadable()) {
                                    Log.i(TAG, "readable=" + (Integer) key.attachment());
                                    key.cancel();
                                }
                            }
                        } catch (ConnectException e) {
                            Log.e(TAG, e.getMessage());
                            if (e.getMessage().equals("Connection refused")) {
                                cnt_selected++;
                                publishProgress((Integer) key.attachment(), 0);
                                key.cancel();
                            } else if (e.getMessage().equals("The operation timed out")) {
                                cnt_selected++;
                                publishProgress(0, -2);
                                key.cancel();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                            cnt_selected++;
                            publishProgress(0, -2);
                            key.cancel();
                        } finally {
                            iterator.remove();
                        }
                    }
                }
                if (cnt_selected >= size) {
                    selector.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                selector.close();
            } catch (IOException e) {
            }
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

    protected void onCancelled() {
        try {
            // selector.selectNow(); //ANR
            selector.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
