package com.example.android.bluetoothlegatt;

import java.util.HashMap;

public class BleGattAttributes {
    @SuppressWarnings("unchecked")
    private static HashMap<String, String> attributes = new HashMap();

    public static String SERVICE = "ad670100-59ef-4245-a4f1-434a147fad5a";

    public static String CALIBRATION_RATE_MEASUREMENT = "ad670101-59ef-4245-a4f1-434a147fad5a";
    public static String CALIBRATION_READ_MEASUREMENT = "ad670102-59ef-4245-a4f1-434a147fad5a";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    static {
        // Services.
        // Characteristics.
        attributes.put(CALIBRATION_RATE_MEASUREMENT, "Start Calibration");
        attributes.put(CALIBRATION_READ_MEASUREMENT,"Read Calibation");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
