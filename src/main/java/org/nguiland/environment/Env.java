package org.nguiland.environment;

public final class Env {

    private Env() {
    }

    public static String get(String name) {
        return System.getenv(name);
    }
}
