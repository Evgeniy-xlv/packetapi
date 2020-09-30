package ru.xlv.packetapi.client.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacket;

import java.io.IOException;

public interface IPacketOutClient extends IPacket {

    /**
     * This method will be called before sending.
     * */
    void write(ByteBufOutputStream bbos) throws IOException;
}
