package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;

public interface IPacketOut extends IPacket {

    /**
     * This method will be called before sending.
     * */
    void write(ByteBufOutputStream bbos) throws IOException;
}
