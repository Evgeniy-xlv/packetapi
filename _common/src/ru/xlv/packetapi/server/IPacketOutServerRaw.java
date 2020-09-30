package ru.xlv.packetapi.server;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacket;

import java.io.IOException;

public interface IPacketOutServerRaw<PLAYER> extends IPacket {

    /**
     * This method will be called before sending.
     * */
    void write(PLAYER player, ByteBufOutputStream byteBufOutputStream) throws IOException;
}
