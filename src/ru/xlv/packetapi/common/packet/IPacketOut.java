package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;

public interface IPacketOut extends IPacket {

    /**
     * Вызывается перед отправкой
     * */
    void write(ByteBufOutputStream bbos) throws IOException;
}
