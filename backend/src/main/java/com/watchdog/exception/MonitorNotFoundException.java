package com.watchdog.exception;

public class MonitorNotFoundException extends RuntimeException {
    public MonitorNotFoundException(String id) {
        super("Monitor not found for device: " + id);
    }
}
