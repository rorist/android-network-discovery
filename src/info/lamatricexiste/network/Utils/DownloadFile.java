// Taken from: http://github.com/ctrlaltdel/TahoeLAFS-android

package info.lamatricexiste.network.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class DownloadFile {

    private static String TAG = "DownloadFile";
    private HttpClient httpclient;

    public DownloadFile(String url, String dst) throws IOException {
        Log.i(TAG, "Downloading " + url + " to " + dst);
        httpclient = new DefaultHttpClient();

        Log.d(TAG, "Opening url " + url);
        InputStream in = openURL(url);

        Log.d(TAG, "Opening destination file " + dst);
        OutputStream out = new FileOutputStream(dst);

        Log.d(TAG, "Writing file");
        final ReadableByteChannel inputChannel = Channels.newChannel(in);
        final WritableByteChannel outputChannel = Channels.newChannel(out);

        fastChannelCopy(inputChannel, outputChannel);

        inputChannel.close();
        outputChannel.close();

        Log.d(TAG, "Closing HTTP connection");
        in.close();

        Log.d(TAG, "Closing file");
        out.close();
    }

    private InputStream openURL(String url) {
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;

        Log.i(TAG, "GET " + url);

        try {
            try {
                response = httpclient.execute(httpget);
            } catch (SSLException e) {
                Log.i(TAG, "SSL Certificate is not trusted");
                response = httpclient.execute(httpget);
            }
            Log.i(TAG, "Status:[" + response.getStatusLine().toString() + "]");
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return entity.getContent();
            }
        } catch (ClientProtocolException e) {
            Log.e("REST", "There was a protocol based error", e);
        } catch (IOException e) {
            Log.e("REST", "There was an IO Stream related error", e);
        }

        return null;
    }

    public static void fastChannelCopy(final ReadableByteChannel src,
            final WritableByteChannel dest) throws IOException {
        if (src != null) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            while (src.read(buffer) != -1) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                dest.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
        }
    }
}
