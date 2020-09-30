package ru.xlv.packetapi.common.packet;

public class PacketNotFoundException extends RuntimeException {

    public PacketNotFoundException() {
    }

    public PacketNotFoundException(String message) {
        super(message);
    }

    public PacketNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PacketNotFoundException(Throwable cause) {
        super(cause);
    }

    public PacketNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String toString() {
        return this.getClass() + ": " + this.getMessage();
    }
}
