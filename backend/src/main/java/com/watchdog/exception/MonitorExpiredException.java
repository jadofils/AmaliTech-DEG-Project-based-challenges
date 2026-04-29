package com.watchdog.exception;

public class MonitorExpiredException extends RuntimeException {
    public MonitorExpiredException(String id) {
        super("Monitor is expired. Create a new monitor to resume tracking: " + id);
    }
}
