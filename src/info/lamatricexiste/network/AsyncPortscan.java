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
    private int cnt_selected;
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
            cnt_selected = 0;
            selector = Selector.open();
            long size = 127;
            InetAddress ina = InetAddress.getByName(ipAddr);
            for (int i = 1; i <= 128; i++) {
                connectSocket(ina, i);
            }

            while (selector.isOpen()) {
                if (selector.select(TIMEOUT_SELECT) > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = (SelectionKey) iterator.next();
                        try {
                            if (key.isValid()) {
                                if (key.isAcceptable()) {
                                    Log.i(TAG, "acceptable=" + (Integer) key.attachment());
                                } else if (key.isConnectable()) {
                                    if (((SocketChannel) key.channel()).finishConnect()) {
                                        Log.i(TAG, "connected=" + (Integer) key.attachment());
                                        key.interestOps(SelectionKey.OP_WRITE);
                                    }
                                } else if (key.isWritable()) {
                                    Log.i(TAG, "writable=" + (Integer) key.attachment());
                                    key.interestOps(SelectionKey.OP_READ);
                                } else if (key.isReadable()) {
                                    Log.i(TAG, "readable=" + (Integer) key.attachment());
                                    cnt_selected++;
                                    key.cancel();
                                }
                            }
                        } catch (ConnectException e) {
                            cnt_selected++;
                            // Connection refused
                            Log.e(TAG, e.getMessage());
                        }
                        iterator.remove();
                    }
                }
                if (cnt_selected >= size) {
                    selector.close();
                }
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    private void connectSocket(InetAddress ina, int port) throws IOException {
        // Create the socket
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(new InetSocketAddress(ina, port));
        socket.register(selector, SelectionKey.OP_CONNECT, new Integer(port));
    }

}
