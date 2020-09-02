package ru.xlv.packetapi.server.packet.forge;

import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * Специальный пакет для серверной стороны, позволяет работать с игроком, от которого пришел пакет.
 * */
public interface IPacketInOnServer extends IPacketIn {

    void read(EntityPlayerMP entityPlayer, ByteBufInputStream bbis) throws IOException;

    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}
}
