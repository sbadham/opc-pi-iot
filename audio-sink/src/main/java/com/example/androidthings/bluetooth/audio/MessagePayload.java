package com.example.androidthings.bluetooth.audio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * This class handles the serialization of the data objects to/from Strings used as
 * payloads on IotCore events.
 *
 * IotCore accepts arbitrary binary payloads and doesn't enforce any particular format.
 *
 */
public class MessagePayload {

    /**
     * De-serialize IotCore device configuration message payload as a JSON string.
     *
     * Format of the message should be similar to:
     * <pre>
     * {
     *      "version": 1,
     *      "ua-client": "opc.tcp://192.168.43.158:49320"
     * }
     * </pre>
     *
     * @param jsonPayload JSON of the device config message
     * @return JSON String
     */
    public static DeviceConfig parseDeviceConfigPayload(String jsonPayload) {
        try {
            JSONObject message = new JSONObject(jsonPayload);
            DeviceConfig deviceConfig = new DeviceConfig();
            deviceConfig.version = message.getInt("version");
            deviceConfig.uaClient = message.getString("ua-client");
            return deviceConfig;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid message: \"" + jsonPayload + "\"", e);
        }
    }

    public static class DeviceConfig {
        public int version;
        public String uaClient;

        @Override
        public String toString() {
            return "DeviceConfig{" +
                    "version=" + version +
                    ", uaClient=" + uaClient +
                    '}';
        }
    }
}