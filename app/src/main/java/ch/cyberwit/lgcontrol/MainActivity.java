package ch.cyberwit.lgcontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText ipTextBox = (EditText)findViewById(R.id.ip);

        final Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipTextBox.getText().toString();
                lgClient = new LGClient(ip);

                try {
                    SharedPreferences preferences =
                            getSharedPreferences("user_settings", MODE_WORLD_READABLE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("ip", ip);
                    editor.apply();
                    lgClient.startPairing();
                } catch (IOException e) {
                    Log.e(TAG, "startButton onClick", e);
                }
            }
        });

        final EditText textBox = (EditText)findViewById(R.id.pairCode);

        final Button pairButton = (Button)findViewById(R.id.pairButton);
        pairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lgClient != null) {
                    String pairCode = textBox.getText().toString();

                    try {
                        lgClient.pair(pairCode);

                        SharedPreferences preferences =
                                getSharedPreferences("user_settings", MODE_WORLD_READABLE);
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

    private LGClient lgClient;

    private static final String TAG = MainActivity.class.getName();
}
