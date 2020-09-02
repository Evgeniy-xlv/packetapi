package ru.xlv.packetapi.common.packet;

import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public interface IPacketIn extends IPacket {

    /**
     * Здесь следует производить обработку пакета.
     * */
    void read(ByteBufInputStream bbis) throws IOException;
}
