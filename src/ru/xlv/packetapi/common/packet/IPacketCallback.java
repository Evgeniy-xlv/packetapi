package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * This packet differs from all packets in that it is additionally signing and entered into the cache until it waits
 * for a response from the server signed by the same pid, or receives a timeout.
 * <p>
 * When (if) a response comes from the server, it will call {@link IPacketCallback#read(ByteBufInputStream)}.
 * <p>
 * It is important to understand that this packet can only be sent from client to server and not otherwise. The server packet must also inherit this interface.
 * */
public interface IPacketCallback extends IPacket {

    /**
     * This method will be called when the packet is received.
     * */
    void write(ByteBufOutputStream bbos) throws IOException;

    /**
     * This method will be called when (if) the client receives a response from the server.
     * */
    void read(ByteBufInputStream bbis) throws IOException;
}
