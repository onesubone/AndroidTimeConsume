package org.eap.time.consume;

import android.util.Log;

public class AndroidLogger {
    private static final String TAG = "EAP.MethodConsume";

    public static void debug(String msg) {
        Log.d(TAG, msg);
    }

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void info(String msg) {
        Log.i(TAG, msg);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }
}
