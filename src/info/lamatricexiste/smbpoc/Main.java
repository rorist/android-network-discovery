package info.lamatricexiste.smbpoc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.lang.NullPointerException;
import java.util.ConcurrentModificationException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Main extends Activity {
    
    //    private final String TAG = "Network hardening";
    private static ArrayAdapter<String> adapter = null;
    public static ListView              list    = null;
    public static TextView              info    = null;
    public static List<InetAddress>     hosts   = new ArrayList<InetAddress>();
    protected Button                    btn     = null;
    protected Button                    btn1    = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        info = (TextView) findViewById(R.id.info); 
//        EditText ipedit = (EditText) findViewById(R.id.ip);
        btn = (Button) findViewById(R.id.btn);
        btn1 = (Button) findViewById(R.id.btn1);

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateList();
            }
        });
        
        adapter = new ArrayAdapter<String>(this, R.layout.list);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);

        Network.setMainActivity(this);
    }
    
    @Override
    public void onResume(){
        super.onResume();
        startService(new Intent(this, Network.class));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, Network.class));
    }
    
    public static void updateList(){
        try {
            adapter.clear();
        }
        catch (ConcurrentModificationException e){
            addText(e.getMessage());
        }
        listHosts();
    }
    
    public static void listHosts(){
        try {
            for(InetAddress h : hosts){
                addText(h.getHostAddress());
            }
        }
        catch (ConcurrentModificationException e){
            addText(e.getMessage());
        }
    }
    
    static void addText(String text){
        try {
            adapter.add(text);
            list.setSelection(View.FOCUS_DOWN);
            //list.requestLayout();
            //list.computeScroll();
        }
        catch (ConcurrentModificationException e){} //FIXME: do smth
        catch (NullPointerException e){}
    }
    
    static void addTextInfo(String text){
        try {
            info.setText(text);
        }
        catch (ConcurrentModificationException e){
            addText(e.getMessage());
        }
        catch (NullPointerException e){
            addText(e.getMessage());
        }
    }
}
