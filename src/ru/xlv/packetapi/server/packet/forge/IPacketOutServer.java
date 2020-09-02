package ru.xlv.packetapi.server.packet.forge;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.IPacketOut;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, для которого отсылается пакет.
 * */
public interface IPacketOutServer extends IPacketOut {

    /**
     * Вызывается перед отправкой
     * */
    void write(EntityPlayer entityPlayer, ByteBufOutputStream bbos) throws IOException;

    /**
     * Метод не вызывается для пакетов этого типа, используйте {@link IPacketOutServer#write(EntityPlayer, ByteBufOutputStream)}
     * */
    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}
}
