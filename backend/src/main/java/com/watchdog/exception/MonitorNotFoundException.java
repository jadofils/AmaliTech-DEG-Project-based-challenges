package com.watchdog.exception;

public class MonitorNotFoundException extends RuntimeException {
    public MonitorNotFoundException(String message) {
        super(message);
    }
}
