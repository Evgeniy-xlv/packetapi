package ru.xlv.packetapi.common.packet;

public class NoPacketDestinationException extends Exception {

    public NoPacketDestinationException() {
    }

    public NoPacketDestinationException(String message) {
        super(message);
    }

    public NoPacketDestinationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoPacketDestinationException(Throwable cause) {
        super(cause);
    }

    public NoPacketDestinationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String toString() {
        return this.getClass() + ": " + this.getMessage();
    }
}
