package ru.xlv.packetapi.server.packet.forge;

import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, от которого пришел пакет.
 * */
public interface IPacketInOnServerRaw<PLAYER> extends IPacketIn {

    void read(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException;

    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}
}
