package com.watchdog.exception;

public class MonitorAlreadyExistsException extends RuntimeException {
    public MonitorAlreadyExistsException(String id) {
        super("Monitor already exists for device: " + id);
    }
}
