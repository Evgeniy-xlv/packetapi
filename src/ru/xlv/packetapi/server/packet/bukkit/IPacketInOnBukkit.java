package ru.xlv.packetapi.server.packet.bukkit;


import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, от которого пришел пакет.
 * */
public interface IPacketInOnBukkit extends IPacketIn {

    void read(Player player, ByteBufInputStream bbis) throws IOException;

    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}
}
