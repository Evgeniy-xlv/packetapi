package ru.xlv.packetapi.server.packet;

/**
 * Serves as a tool for manually managing sending a packet back to the client.
 * <p>
 * It is applicable when working with a packet asynchronously, or when using {@link ru.xlv.packetapi.server.RequestController}.
 * */
public interface PacketCallbackSender {

    /**
     * Sends a packet at the same time.
     * I advise you to always make sure that you don't call the method twice.
     * */
    void send();
}
