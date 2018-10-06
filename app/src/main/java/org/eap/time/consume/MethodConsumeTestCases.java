package org.eap.time.consume;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MethodConsumeTestCases {
    private static void loadUrl(String url) {
        final OkHttpClient okHttpClient = new OkHttpClient();
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                String headers = response.headers().toString();
//                Log.i("eap", "run: " + body);
            } else {
//                Log.e("eap", "run: " + response.code() + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void noInputNoOutputNoException() {
        loadUrl("https://www.baidu.com/");
    }

    public long inputAndLongOutputNoException(Set<String> set, int a, double b, long c) {
        Map<String, Object> res = new HashMap<>();
        res.put("set", set);
        res.put("a", a);
        res.put("b", b);
        res.put("c", c);
        loadUrl("https://segmentfault.com/a/1190000007276536");
        return 122L;
    }

    public float inputAndFloatOutputNoException(Set<String> set, int a, double b, long c) {
        Map<String, Object> res = new HashMap<>();
        res.put("set", set);
        res.put("a", a);
        res.put("b", b);
        res.put("c", c);
        loadUrl("https://segmentfault.com/a/1190000007276536");
        return 122.2F;
    }

    public Map<String, Object> inputAndOutputNoException(Set<String> set, int a, double b, long c) {
        Map<String, Object> res = new HashMap<>();
        res.put("set", set);
        res.put("a", a);
        res.put("b", b);
        res.put("c", c);
        loadUrl("https://blog.csdn.net/qq_25566921/article/details/80241064");
        return res;
    }

    public Map<String, Object> inputAndOutputExceptionThrows(Set<String> set, int a, double b, long c) throws InterruptedException {
        Map<String, Object> res = new HashMap<>();
        res.put("set", set);
        res.put("a", a);
        res.put("b", b);
        res.put("c", c);
        loadUrl("https://blog.csdn.net/qq_25566921/article/details/80241064");
        if (a % 2 == 0) {
            throw new InterruptedException("");
        }
        return res;
    }

    public Map<String, Object> inputAndOutputAndException(Set<String> set, int a, double b, long c) {
        try {
            Map<String, Object> res = new HashMap<>();
            res.put("set", set);
            res.put("a", a);
            Thread.sleep(100);
            res.put("b", b);
            res.put("c", c);
            AndroidLogger.debug("Running block!");
            return res;
        } catch (InterruptedException e) {
            e.printStackTrace();
            AndroidLogger.debug("Exception catch block!");
            return null;
        } finally {
            AndroidLogger.debug("finally block!");
        }
    }
}
