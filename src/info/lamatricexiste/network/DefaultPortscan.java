/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

/**
 * Java NIO Documentation:
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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.util.Log;
import android.util.SparseArray;

public class DefaultPortscan extends AbstractPortScan {

    private final int MAX_READ = 75;
    private final String TAG = "PortScan";
    private final int TIMEOUT_SELECT = 300;
    private final int TIMEOUT_READ = 5000;
    private int cnt_selected;
    private Selector selector = null;
    protected String[] mBanners = null;

    protected DefaultPortscan(String host, final int timeout) {
        super(host, timeout);
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
        cnt_selected = 0;
        selector = Selector.open();
        for (int i = PORT_START; i <= PORT_END; i++) {
            connectSocket(ina, i);
            // Thread.sleep(timeout);
        }
        doSelect(PORT_END - PORT_START);
    }

    protected void stop() {
        if (selector != null) {
            synchronized (selector) {
                if (selector.isOpen()) {
                    try {
                        // Force invalidate keys
                        Iterator<SelectionKey> iterator = selector.keys().iterator();
                        while (iterator.hasNext()) {
                            synchronized (iterator) {
                                publishProgress(0, -2);
                                finishKey(iterator.next());
                            }
                        }
                        // Close the selector
                        selector.close();
                    } catch (ClosedSelectorException e) {
                        Log.e(TAG, "ClosedSelectorException");
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }
    }

    private void connectSocket(InetAddress ina, int port) throws IOException {
        // Create the socket
        InetSocketAddress addr = new InetSocketAddress(ina, port);
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(addr);
        // Register the Channel with port as attachement
        SparseArray<Integer> data = new SparseArray<Integer>(1);
        data.append(0, port);
        socket.register(selector, SelectionKey.OP_CONNECT, data);
    }

    private void doSelect(final int NB) {
        try {
            while (selector.isOpen()) {
                selector.select(TIMEOUT_SELECT);
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
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

    private void handleConnect(SelectionKey key) {
        try {
            if (((SocketChannel) key.channel()).finishConnect()) { // Open
                boolean prout = true; // FIXME: get from preferences
                if (prout) {
                    // Create a new selector and register for reading
                    Selector readSelector = Selector.open();
                    SelectionKey tmpKey = ((SocketChannel) key.channel()).register(readSelector, SelectionKey.OP_READ);
                    tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                    int code = readSelector.select(TIMEOUT_READ);
                    tmpKey.interestOps(tmpKey.interestOps() & (~SelectionKey.OP_READ));
                    if (code != 0) {
                        handleRead(tmpKey, ((SparseArray<Integer>) key.attachment()).get(0));
                        time = System.currentTimeMillis(); // Reset selector timeout
                        finishKey(key);
                        return;
                    }
                    time = System.currentTimeMillis(); // Reset the selector timeout
                    finishKey(tmpKey);
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
        // new Banner(host, ((SparseArray<Integer>) key.attachment()).get(0),
        // 8000).execute();

        ByteBuffer bbuf = ByteBuffer.allocate(MAX_READ);
        int numRead = 0;
        try {
            // while (numRead > 0) {
            numRead = ((SocketChannel) key.channel()).read(bbuf);
            // }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        if (numRead != -1) {
            mBanners[port] = new String(bbuf.array()).substring(0, numRead).trim();
        }
        publishProgress(port, (int) 1);
        finishKey(key);
        cnt_selected--; // Hack for finishKey();
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
