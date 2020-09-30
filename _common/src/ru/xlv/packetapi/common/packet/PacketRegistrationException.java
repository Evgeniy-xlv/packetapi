package ru.xlv.packetapi.common.packet;

public class PacketRegistrationException extends Exception {

    public PacketRegistrationException() {
    }

    public PacketRegistrationException(String message) {
        super(message);
    }

    public PacketRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PacketRegistrationException(Throwable cause) {
        super(cause);
    }

    public PacketRegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String toString() {
        return this.getClass() + ": " + this.getMessage();
    }
}
