package com.watchdog.exception;

public class MonitorExpiredException extends RuntimeException {
    public MonitorExpiredException(String message) {
        super(message);
    }
}
