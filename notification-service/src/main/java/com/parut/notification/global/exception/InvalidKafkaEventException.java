package com.parut.notification.global.exception;

public class InvalidKafkaEventException extends RuntimeException {
    public InvalidKafkaEventException(String message) {
        super(message);
    }
}
