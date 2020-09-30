package ru.xlv.packetapi.client.packet;

import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public interface IPacketInClient extends IPacket {

    /**
     * This method will be called when the packet is received.
     * */
    void read(ByteBufInputStream bbis) throws IOException;
}
