package ch.cyberwit.lgcontrol;

import android.app.Activity;
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

        lgClient = new LGClient("192.168.1.119", getSharedPreferences("user_settings", MODE_WORLD_READABLE));

        final Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
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
                try {
                    lgClient.pair(textBox.getText().toString());
                } catch (IOException e) {
                    Log.e(TAG, "pairButton onClick", e);
                }
            }
        });
    }

    private LGClient lgClient;

    private static final String TAG = MainActivity.class.getName();
}