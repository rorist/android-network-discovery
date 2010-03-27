/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

/**
 * Un peu de doc:
 * http://weblogs.java.net/blog/2006/05/30/tricks-and-tips-nio-part-i-why-you-must-handle-opwrite
 * http://www.java.net/blog/2006/06/06/tricks-and-tips-nio-part-ii-why-selectionkeyattach-evil
 * http://weblogs.java.net/blog/2006/07/07/tricks-and-tips-nio-part-iii-thread-or-not-thread
 * http://weblogs.java.net/blog/2006/07/19/tricks-and-tips-nio-part-iv-meet-selectors
 * http://weblogs.java.net/blog/2006/09/21/tricks-and-tips-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.util.Log;
import android.util.SparseArray;

public class DefaultPortscan extends AbstractPortScan {

    private final String TAG = "PortScan";
    private final int TIMEOUT_SELECT = 100;
    // private final int SCAN_RATE = 0;
    private int cnt_selected;
    private Selector selector = null;

    protected DefaultPortscan(String host) {
        super(host);
    }

    protected DefaultPortscan(String host, final int timeout) {
        super(host);
        TIMEOUT_SOCKET = timeout;
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
        cnt_selected = 0;
        selector = Selector.open();
        for (int i = PORT_START; i <= PORT_END; i++) {
            connectSocket(ina, i);
            // Thread.sleep(SCAN_RATE);
        }
        doSelect(PORT_END - PORT_START);
    }

    protected void stop() {
        if (selector != null) {
            synchronized (selector) {
                if (selector.isOpen()) {
                    // Force invalidate keys
                    Iterator<SelectionKey> iterator = selector.keys().iterator();
                    while (iterator.hasNext()) {
                        publishProgress(0, -2);
                        finishKey((SelectionKey) iterator.next());
                    }
                    // Close the selector
                    try {
                        selector.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void connectSocket(InetAddress ina, int port) throws IOException {
        // Create the socket
        InetSocketAddress addr = new InetSocketAddress(ina, port);
        SocketChannel socket;
        socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(addr);
        // Register the Channel with port as attachement
        SparseArray<Integer> data = new SparseArray<Integer>(1);
        data.append(0, port);
        socket.register(selector, SelectionKey.OP_CONNECT, data);
    }

    private void doSelect(int STEP) {
        try {
            while (selector.isOpen()) {
                selector.select(TIMEOUT_SELECT);
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            handleConnect(key);
                        }
                        // else if (key.isReadable()) {
                        // handleRead(key);
                        // }
                    }
                }
                cancelTimeouts(); // Filtered or Unresponsive
                if (cnt_selected >= STEP) {
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
                publishProgress(((SparseArray<Integer>) key.attachment()).get(0), (int) 1);
                finishKey(key);
                // Register for reading
                // SparseArray<Long> data = (SparseArray<Long>)
                // key.attachment();
                // data.setValueAt(1, System.currentTimeMillis());
                // ((SocketChannel) key.channel()).register(selector,
                // SelectionKey.OP_READ, data);
                // key.interestOps(SelectionKey.OP_READ);
                // Log.v(TAG, "port=" + data.get(0));
            }
        } catch (IOException e) { // Closed
            publishProgress(((SparseArray<Integer>) key.attachment()).get(0), (int) 0);
            finishKey(key);
        }
    }

    private void finishKey(SelectionKey key) {
        synchronized (key) {
            try {
                ((SocketChannel) key.channel()).close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                key.cancel();
                cnt_selected++;
            }
        }
    }

    // @SuppressWarnings("unchecked")
    // private void handleRead(SelectionKey key) {
    // publishProgress(((SparseArray<Long>) key.attachment()).get(0));
    // finishKey(key);
    //
    // ByteBuffer buf = ByteBuffer.allocateDirect(2);
    // buf.clear();
    // int numRead = 0;
    // try {
    // numRead = ((SocketChannel) key.channel()).read(buf);
    // } catch (IOException e) {
    // Log.e(TAG, e.getMessage());
    // } finally {
    // if (numRead > 0) {
    // try {
    // Log.v(TAG, "read=" + new String(buf.array()));
    // } catch (UnsupportedOperationException e) {
    // Log.e(TAG, "UnsupportedOperationException");
    // }
    // }
    // finishKey(key);
    // }
    // }
}
