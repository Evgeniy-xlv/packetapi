package ru.xlv.packetapi.server.packet.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacketOut;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, для которого отсылается пакет.
 * */
public interface IPacketOutBukkit extends IPacketOut {

    /**
     * Вызывается перед отправкой
     * */
    void write(Player player, ByteBufOutputStream bbos) throws IOException;

    /**
     * Метод не вызывается для пакетов этого типа, используйте {@link IPacketOutBukkit#write(Player, ByteBufOutputStream)}
     * */
    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}
}
