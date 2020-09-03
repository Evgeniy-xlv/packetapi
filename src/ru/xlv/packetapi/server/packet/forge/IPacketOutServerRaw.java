package ru.xlv.packetapi.server.packet.forge;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacketOut;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, для которого отсылается пакет.
 * */
public interface IPacketOutServerRaw<PLAYER> extends IPacketOut {

    /**
     * Вызывается перед отправкой
     * */
    void write(PLAYER entityPlayer, ByteBufOutputStream bbos) throws IOException;

    /**
     * Метод не вызывается для пакетов этого типа, используйте {@link IPacketOutServerRaw#write(PLAYER, ByteBufOutputStream)}
     * */
    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}
}
