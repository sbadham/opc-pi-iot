package com.example.androidthings.bluetooth.audio;

import android.os.AsyncTask;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.client.UaClient;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.transport.security.SecurityMode;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;

public class OpcUaTask extends AsyncTask<String, Void, String> {

    private String OperationResult = "";
    private MqttClient client = null;
    private static final String QUEUE_IP = "tcp://192.168.1.19:1883"; //TODO Mosquitto Broker Address Change

    @Override
    protected String doInBackground(String... uaa) {

        // Initialise MqttClient
        try {
            if (client == null) {
                client = new MqttClient(QUEUE_IP, "VpsAndroidThing", new MemoryPersistence());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

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

        // Publish the OPC UA Server status on mq
        try {
            if(!client.isConnected()){
                client.connect();
            }

            result = "OPC UA Status: " + result;

            MqttMessage message = new MqttMessage();
            message.setPayload(result
                    .getBytes());
            client.publish("topic/vps", message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
