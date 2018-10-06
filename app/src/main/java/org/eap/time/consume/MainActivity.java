package org.eap.time.consume;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashSet;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MethodConsumeTestCases consumeTestCases = new MethodConsumeTestCases();
                consumeTestCases.noInputNoOutputNoException();
                consumeTestCases.inputAndOutputNoException(new HashSet<String>(), 1, 2, 3);
                consumeTestCases.inputAndLongOutputNoException(new HashSet<String>(), 1, 2, 3);
                consumeTestCases.inputAndFloatOutputNoException(new HashSet<String>(), 1, 2, 3);
                try {
                    consumeTestCases.inputAndOutputExceptionThrows(new HashSet<String>(), 1, 2, 3);
                    consumeTestCases.inputAndOutputExceptionThrows(new HashSet<String>(), 4, 2, 3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                consumeTestCases.inputAndOutputAndException(new HashSet<String>(), 1, 2, 3);
            }
        }.start();
    }
}
