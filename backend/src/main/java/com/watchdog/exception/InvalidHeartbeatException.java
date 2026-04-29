package com.watchdog.exception;

public class InvalidHeartbeatException extends RuntimeException {
    public InvalidHeartbeatException(String message) {
        super(message);
    }
}
