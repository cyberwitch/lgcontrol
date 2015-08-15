package ch.cyberwit.lgcontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText textBox = (EditText)findViewById(R.id.pairCode);

        final SharedPreferences preferences =
                getSharedPreferences("user_settings", MODE_WORLD_READABLE);

        textBox.setText(preferences.getString("pair_code", ""));

        final Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DiscoverTVTask().execute();
            }
        });

        final Button pairButton = (Button)findViewById(R.id.pairButton);
        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lgClient != null) {
                    String pairCode = textBox.getText().toString().toUpperCase();

                    try {
                        lgClient.pair(pairCode);

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("pair_code", pairCode);
                        editor.apply();
                    } catch (IOException e) {
                        Log.e(TAG, "pairButton onClick", e);
                    }
                }
            }
        });
    }

    private class DiscoverTVTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final DatagramSocket msocket = new DatagramSocket();

                String SEARCH = "M-SEARCH * HTTP/1.1" + "\r\n" +
                        "HOST: 239.255.255.250:1900" + "\r\n" +
                        "MAN: \"ssdp:discover\"" + "\r\n" +
                        "MX: 2" + "\r\n" +
                        "ST: urn:schemas-upnp-org:device:MediaRenderer:1" + "\r\n" + "\r\n";

                // Wait only TIMEOUT seconds when receiving reply packets
                msocket.setSoTimeout(2 * 1000);
                msocket.send(new DatagramPacket(SEARCH.getBytes(), SEARCH.length(),
                        InetAddress.getByName("239.255.255.250"), 1900));

                try {
                    while (true) {
                                        /*
                                         * We must use new buffer and packets - reusing objects can
                                         * cause corruption if there are multiple devices in LAN.
                                         */
                        final byte[] buf = new byte[1000];
                        final DatagramPacket reply = new DatagramPacket(buf, buf.length);
                        msocket.receive(reply);
                        if ((new String(reply.getData())).contains("LG")) {
                            String ip = reply.getAddress().getHostAddress();
                            lgClient = new LGClient(ip);

                            SharedPreferences preferences =
                                    getSharedPreferences("user_settings", MODE_WORLD_READABLE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("ip", ip);
                            editor.apply();
                            lgClient.startPairing();
                            Log.d(TAG, "Found TV at " + ip);
                        }
                    }
                } catch (final SocketTimeoutException exception) {
                    Log.d(TAG, "Timed out discovering TV");
                }
            } catch (IOException e) {
                Log.d(TAG, "IOException discovering TV", e);
            }

            return null;
        }
    }

    private LGClient lgClient;

    private static final String TAG = MainActivity.class.getName();
}
