package ru.xlv.packetapi.common.packet;

public class EmptyConstructorException extends RuntimeException {

    public EmptyConstructorException() {
    }

    public EmptyConstructorException(String message) {
        super(message);
    }

    public EmptyConstructorException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyConstructorException(Throwable cause) {
        super(cause);
    }

    public EmptyConstructorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String toString() {
        return this.getClass() + ": " + this.getMessage();
    }
}
