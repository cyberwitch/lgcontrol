package ch.cyberwit.lgcontrol;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LGClient {
    public LGClient(String ip) {
        this.ip = ip;
    }

    public LGClient(String ip, String pairCode) {
        this.ip = ip;
        this.pairCode = pairCode;
    }

    public void setPairCode(String pairCode) {
        this.pairCode = pairCode;
    }

    public void startPairing() throws IOException {
        Log.d(TAG, "Starting start pairing task");
        new SendMessageTask(MessageType.START_PAIRING).execute();
    }

    public void pair(String pairCode) throws IOException {
        Log.d(TAG, "Starting pair task with code: " + pairCode);
        this.pairCode = pairCode;
        new SendMessageTask(MessageType.PAIR).execute();
    }

    public void toggleTV() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Log.d(TAG, "Starting toggle tv task");
        new SendMessageTask(MessageType.TOGGLE_TV).execute();
    }

    public void turnTVOff() throws IOException {
        sendCommand(8);
    }

    public void volumeUp() throws IOException {
        sendCommand(2);
    }

    public void volumeDown() throws IOException {
        sendCommand(3);
    }

    public void sendCommand(int command) throws IOException {
        Log.d(TAG, "Staring send command task with command: " + command);
        new SendMessageTask(MessageType.SEND_COMMAND, command).execute();
    }

    public void turnTVOn() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class serviceManager = loader.loadClass("android.os.ServiceManager");
        Class settingsMgr = loader.loadClass("com.amazon.device.settings.IAmazonSystemSettingsManager");
        Class settingsMgrStub = loader.loadClass("com.amazon.device.settings.IAmazonSystemSettingsManager$Stub");
        Method getService = serviceManager.getMethod("getService", new Class[]{String.class});
        Method callOneTouch = settingsMgr.getMethod("sendAsynchronousCECOneTouchCmd", new Class[]{});
        Method asInterface = settingsMgrStub.getMethod("asInterface", new Class[]{IBinder.class});
        IBinder ibinder = (IBinder)getService.invoke(null, new Object[]{"amazon_audio_video"});
        Object settingsMgrInst = asInterface.invoke(null, new Object[]{ibinder});
        callOneTouch.invoke(settingsMgrInst, new Object[]{});
    }

    private enum MessageType {
        START_PAIRING, PAIR, SEND_COMMAND, TOGGLE_TV
    };

    private class SendMessageTask extends AsyncTask<Void, Void, Void> {

        public SendMessageTask(MessageType type, int... command) {
            this.type = type;
            if (command.length > 0) {
                this.command = command[0];
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                switch(type) {
                    case START_PAIRING:
                        startPairing();
                        break;
                    case PAIR:
                        pair();
                        break;
                    case SEND_COMMAND:
                        sendCommand();
                        break;
                    case TOGGLE_TV:
                        toggleTV();
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "doInBackground", e);
            }

            return null;
        }

        private void startPairing() throws IOException {
            Log.d(TAG, "About to start pairing");
            sendMessage("/hdcp/api/auth",
                    "<?xml version='1.0' encoding='utf-8'?><auth><type>AuthKeyReq</type></auth>");
        }

        private void pair() throws IOException {
            Log.d(TAG, "About to pair");
            HttpResponse httpResponse = sendMessage("/hdcp/api/auth",
                    "<?xml version='1.0' encoding='utf-8'?><auth><type>AuthReq</type><value>" +
                            pairCode + "</value></auth>");

            String encoding = EntityUtils.getContentCharSet(httpResponse.getEntity());
            encoding = encoding == null ? "UTF-8" : encoding;

            InputStream stream = AndroidHttpClient.getUngzippedContent(httpResponse.getEntity());
            InputStreamEntity unzEntity = new InputStreamEntity(stream, -1);

            Matcher m = sessionPattern.matcher(EntityUtils.toString(unzEntity, encoding));

            while (m.find()) {
                sessionCode = m.group(1);
                Log.d(TAG, "Got session code: " + sessionCode);
            }
        }

        private void toggleTV() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            command = 8;

            try {
                sendCommand();
            }
            catch (HttpHostConnectException e) {
                turnTVOn();
            }
        }

        private void sendCommand() throws IOException {
            Log.d(TAG, "About to send command: " + command);
            HttpResponse httpResponse = sendMessage("/hdcp/api/dtv_wifirc",
                    "<?xml version='1.0' encoding='utf-8'?><command><session>" + sessionCode +
                            "</session><type>HandleKeyInput</type><value>" + command +
                            "</value></command>");
            Log.d(TAG, "Response code: " + httpResponse.getStatusLine().getStatusCode());
            if (httpResponse.getStatusLine().getStatusCode() == 401) {
                pair();
                Log.d(TAG, "Resending command: " + command);
                httpResponse = sendMessage("/hdcp/api/dtv_wifirc",
                        "<?xml version='1.0' encoding='utf-8'?><command><session>" + sessionCode +
                                "</session><type>HandleKeyInput</type><value>" + command +
                                "</value></command>");
                Log.d(TAG, "Response code: " + httpResponse.getStatusLine().getStatusCode());
            }
        }

        private HttpResponse sendMessage(String endpoint, String data) throws IOException {
            HttpPost httpPost = new HttpPost("http://" + ip + ":8080" + endpoint);

            httpPost.setHeader("Content-Type", "application/atom+xml");

            Log.d(TAG, "trying to send message: " + data);

            httpPost.setEntity(new StringEntity(data));

            HttpClient httpClient = new DefaultHttpClient();
            return httpClient.execute(httpPost);
        }

        private MessageType type;
        private int command;

        private final String TAG = SendMessageTask.class.getName();
    }

    private String ip;
    private String pairCode;
    private String sessionCode;

    private Pattern sessionPattern = Pattern.compile("<session>(\\S+)</session>");

    private static final String TAG = LGClient.class.getName();
}
