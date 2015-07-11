package ch.cyberwit.lgcontrol;

import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by cyberwitch on 7/10/15.
 */
public class LGClient {
    public LGClient(String ip, SharedPreferences preferences) {
        this.ip = ip;
        this.preferences = preferences;
    }

    public void startPairing() throws IOException {
        sendMessage("/hdcp/api/auth", "<?xml version='1.0' encoding='utf-8'?><auth><type>AuthKeyReq</type></auth>");
    }

    public void pair(String key) throws IOException {
        sendMessage("/hdcp/api/auth", "<?xml version='1.0' encoding='utf-8'?><auth><type>AuthReq</type><value>" + key + "</value></auth>");
    }

    public void messageCallback(String response) {
        if (preferences.getClass().equals(SharedPreferences.class)) {
            Matcher m = sessionPattern.matcher(response);

            while (m.find()) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("session", m.group(1));
                editor.apply();
            }
        }
    }

    public void sendCommand(int command) throws IOException {
        sendMessage("/hdcp/api/dtv_wifirc", "<?xml version='1.0' encoding='utf-8'?><command><session>" + preferences.getString("session", "") + "</session><type>HandleKeyInput</type><value>" + command + "</value></command>");
    }

    private void sendMessage(String endpoint, String data) throws IOException {
        HttpPost httpPost = new HttpPost("http://" + ip + ":8080" + endpoint);

        httpPost.setHeader("Content-Type", "application/atom+xml");

        Log.d(TAG, "trying to send message: " + data);

        httpPost.setEntity(new StringEntity(data));

        new SendMessageTask().execute(httpPost);
    }

    private class SendMessageTask extends AsyncTask<HttpPost, Void, Void> {

        @Override
        protected Void doInBackground(HttpPost... params) {
            HttpClient httpClient = new DefaultHttpClient();

            try {
                HttpResponse httpResponse = httpClient.execute(params[0]);

                String encoding = EntityUtils.getContentCharSet(httpResponse.getEntity());
                encoding = encoding == null ? "UTF-8" : encoding;

                InputStream stream = AndroidHttpClient.getUngzippedContent(httpResponse.getEntity());
                InputStreamEntity unzEntity = new InputStreamEntity(stream, -1);

                messageCallback(EntityUtils.toString(unzEntity, encoding));
            } catch (IOException e) {
                Log.e(TAG, "doInBackground", e);
            }

            return null;
        }

        private final String TAG = SendMessageTask.class.getName();
    }

    private String ip;
    private SharedPreferences preferences;

    private Pattern sessionPattern = Pattern.compile("<session>(\\S+)</session>");

    private static final String TAG = LGClient.class.getName();
}
