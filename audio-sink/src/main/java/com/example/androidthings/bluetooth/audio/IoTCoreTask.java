package com.example.androidthings.bluetooth.audio;

import android.content.SharedPreferences;
import android.security.keystore.KeyProperties;

import com.google.android.things.iotcore.ConnectionCallback;
import com.google.android.things.iotcore.ConnectionParams;
import com.google.android.things.iotcore.IotCoreClient;
import com.google.android.things.iotcore.OnConfigurationListener;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class IoTCoreTask {

    private IotCoreClient mClient;
    private int configurationVersion;
    private SharedPreferences prefs;

    public IoTCoreTask(SharedPreferences iPrefs){
        //Constructor receives preferences for IoT Core config
        prefs = iPrefs;
    }

    protected void initIoTCore() {
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
            mClient = new IotCoreClient.Builder()
                    .setConnectionParams(connectionParams)
                    .setKeyPair(keyGenerator.getKeyPair())
                    .setConnectionCallback(new ConnectionCallback() {
                        @Override
                        public void onConnected() {
                            System.out.println("onConnected");
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

    protected void doPublish(String inJson){
        mClient.publishDeviceState(inJson.getBytes());
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
