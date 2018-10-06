package org.eap;

public class PluginLogger {
    public static void debug(String log) {
        System.out.println(log);
    }

    public static void info(String log) {
        System.out.println(log);
    }

    public static void error(String log) {
        System.err.println(log);
    }
}
