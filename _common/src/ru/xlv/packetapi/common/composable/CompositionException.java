package ru.xlv.packetapi.common.composable;

public class CompositionException extends Exception {

    public CompositionException() {
    }

    public CompositionException(String message) {
        super(message);
    }

    public CompositionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompositionException(Throwable cause) {
        super(cause);
    }

    public CompositionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
