package com.example.androidthings.bluetooth.audio;

import android.os.AsyncTask;
import android.security.keystore.KeyProperties;

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

import java.security.KeyPair;
import com.google.android.things.iotcore.ConnectionParams;
import com.google.android.things.iotcore.IotCoreClient;

import java.security.GeneralSecurityException;

public class OpcUaTask extends AsyncTask<String, Void, String> {

    private String OperationResult = "";

    @Override
    protected String doInBackground(String... uaa) {

        // Connect to OPC UA Server and retrieve status
        try {
            UaClient client = new UaClient();
            for(String url : uaa){
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
    protected void onPostExecute(String result){

        //TODO Implement IoT Event

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

            // Initialize the IoT Core client
            IotCoreClient client = new IotCoreClient.Builder()
                    .setConnectionParams(connectionParams)
                    .setKeyPair(keyGenerator.getKeyPair())
                    .build();

            // Connect to Cloud IoT Core
            client.connect();

            if (client.isConnected()) {
                // Start sending data!
                System.out.println("Connected and publishing event to IoT Core.");
                String escapeResult = result.replace("\"", "\\\"");
                String outJson = "{\"opc-pi-iot-statusresult\":\"" + escapeResult + "\"}";
                System.out.println("JSON: " + outJson);
                client.publishDeviceState(outJson.getBytes());

                // Disconnect Cloud IoT Core (SB Add)
                client.disconnect();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
