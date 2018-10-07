package org.eap.time.consume;

import android.os.SystemClock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MethodConsumeTest {

    public static void test() {
        long t1 = SystemClock.elapsedRealtime();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            AndroidLogger1.d("eap.MethodConsumeTest.finally block");
        }
        AndroidLogger1.i("eap.MethodConsumeTest method consume "
                + (SystemClock.elapsedRealtime() - t1));
    }

    public Set<String> test(Map<String, String> input, int a, long b, float c, double d, String e) {
        long t1 = SystemClock.elapsedRealtime();
        try {
            Set<String> res = new HashSet<>();
            AndroidLogger1.d("" + a);
            AndroidLogger1.d("" + b);
            AndroidLogger1.d("" + c);
            AndroidLogger1.d("" + d);
            AndroidLogger1.d("" + e);
            Thread.sleep(100);
            res.addAll(input.values());
            return res;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            AndroidLogger1.d("eap.MethodConsumeTest.finally block");
            AndroidLogger1.i("eap.MethodConsumeTest method consume "
                    + (SystemClock.elapsedRealtime() - t1));
        }
    }
}
