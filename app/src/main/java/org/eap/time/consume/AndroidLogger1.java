package org.eap.time.consume;

import android.util.Log;

public class AndroidLogger1 {
    public static void debug(String msg) {
        Log.d("EAP.MethodConsume", msg);
    }

    public static void d(String msg) {
        Log.d("EAP.MethodConsume", msg);
    }

    public static void info(String msg) {
        Log.i("EAP.MethodConsume", msg);
    }

    public static void i(String msg) {
        Log.i("EAP.MethodConsume", msg);
    }
}
