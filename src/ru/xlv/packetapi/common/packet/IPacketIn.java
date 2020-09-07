package ru.xlv.packetapi.common.packet;

import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public interface IPacketIn extends IPacket {

    /**
     * This method will be called when the packet is received.
     * */
    void read(ByteBufInputStream bbis) throws IOException;
}
