package com.example.androidthings.bluetooth.audio;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;

import com.google.android.things.iotcore.OnConfigurationListener;
import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.client.UaClient;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.transport.security.SecurityMode;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;

import com.google.android.things.iotcore.ConnectionParams;
import com.google.android.things.iotcore.IotCoreClient;
import com.google.android.things.iotcore.ConnectionCallback;

import java.security.GeneralSecurityException;

import static android.content.Context.MODE_PRIVATE;

public class OpcUaTask extends AsyncTask<String, Void, String> {

    private String OperationResult = "";
    private IotCoreClient mClient;
    private int configurationVersion;
    private SharedPreferences prefs;

    public OpcUaTask(SharedPreferences iPrefs){
        //Constructor receives preferences for persisting IoT Core config changes
        prefs = iPrefs;
    }

    @Override
    protected String doInBackground(String... uaa) {

        configurationVersion = 0;

        // Connect to OPC UA Server and retrieve status
        try {
            UaClient client = new UaClient();
            for (String url : uaa) {
                client.setUri(url);
            }
            client.setSecurityMode(SecurityMode.NONE);
            initialize(client);
            client.connect();
            DataValue value = client.readValue(Identifiers.Server_ServerStatus_State);
            System.out.println(value);
            OperationResult = value.toString();
            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return OperationResult;
    }

    // OPC Server Connection Initialise
    protected static void initialize(UaClient client) throws SecureIdentityException, IOException, UnknownHostException {
        // *** Application Description is sent to the server
        ApplicationDescription appDescription = new ApplicationDescription();
        appDescription.setApplicationName(new LocalizedText("SimpleClient", Locale.ENGLISH));
        // 'localhost' (all lower case) in the URI is converted to the actual
        // host name of the computer in which the application is run
        appDescription.setApplicationUri("urn:localhost:UA:SimpleClient");
        appDescription.setProductUri("urn:prosysopc.com:UA:SimpleClient");
        appDescription.setApplicationType(ApplicationType.Client);

        final ApplicationIdentity identity = new ApplicationIdentity();
        identity.setApplicationDescription(appDescription);
        client.setApplicationIdentity(identity);
    }

    @Override
    protected void onPostExecute(String result) {

        try {
            // Generate or get keys
            AuthKeyGenerator keyGenerator = null;
            try {
                keyGenerator = new AuthKeyGenerator(KeyProperties.KEY_ALGORITHM_RSA);
            } catch (GeneralSecurityException | IOException e) {
                throw new IllegalArgumentException("Cannot create a key generator", e);
            }

            // Configure Cloud IoT Core project information
            ConnectionParams connectionParams = new ConnectionParams.Builder()
                    .setProjectId("jlr-dl-dev")
                    .setRegistry("my-registry", "us-central1")
                    .setDeviceId("opc-pi-iot")
                    .build();

            // Construct JSON
            String escapeResult = result.replace("\"", "\\\"");
            final String outJson = "{\"opc-pi-iot-statusresult\":\"" + escapeResult + "\"}";
            System.out.println("JSON: " + outJson);

            // Initialize the IoT Core client
            mClient = new IotCoreClient.Builder()
                    .setConnectionParams(connectionParams)
                    .setKeyPair(keyGenerator.getKeyPair())
                    .setConnectionCallback(new ConnectionCallback() {
                        @Override
                        public void onConnected() {
                            System.out.println("onConnected");
                            doPublish(outJson);
                        }

                        @Override
                        public void onDisconnected(int i) {
                            System.out.println("onDisconnected Reason Code:" + i);
                        }
                    })
                    .setOnConfigurationListener(new OnConfigurationListener() {
                        @Override
                        public void onConfigurationReceived(byte[] bytes) {
                            thisConfigurationReceived(bytes);
                        }
                    })
                    .build();

            // Connect to Cloud IoT Core
            mClient.connect();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    protected void doPublish(String outJson) {
        System.out.println("Publishing: " + outJson);
        mClient.publishDeviceState(outJson.getBytes());
    }

    private void thisConfigurationReceived(byte[] bytes) {
        if (bytes.length == 0) {
            System.out.println("Ignoring empty device config event");
            return;
        }
        MessagePayload.DeviceConfig deviceConfig = MessagePayload.parseDeviceConfigPayload(
                new String(bytes));
        if (deviceConfig.version <= configurationVersion) {
            System.out.println("Ignoring device config message with old version. Current version: " +
                    configurationVersion + ", Version received: " + deviceConfig.version);
            return;
        }
        System.out.println("Applying device config: " + deviceConfig);
        configurationVersion = deviceConfig.version;

        // Config stored in SharedPreferences for retrieval by the calling object
        SharedPreferences.Editor prefEdit = prefs.edit();
        prefEdit.putString("ua-client", deviceConfig.uaClient);
        prefEdit.commit();
    }
}
