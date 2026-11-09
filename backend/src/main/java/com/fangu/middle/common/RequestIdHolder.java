package com.fangu.middle.common;

public final class RequestIdHolder {
    private static final ThreadLocal<String> CTX = new ThreadLocal<>();

    public static void set(String id) { CTX.set(id); }
    public static String get() {
        String id = CTX.get();
        return id == null ? "" : id;
    }
    public static void clear() { CTX.remove(); }

    private RequestIdHolder() {}
}
