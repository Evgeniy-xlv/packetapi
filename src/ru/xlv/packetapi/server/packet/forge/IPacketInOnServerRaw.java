package ru.xlv.packetapi.server.packet.forge;

import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player from whom the package came.
 * */
public interface IPacketInOnServerRaw<PLAYER> extends IPacketIn {

    /**
     * This method will be called when the packet is received.
     * */
    void read(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException;

    /**
     * @deprecated This method is not called for packets of this type, use {@link IPacketInOnServerRaw#read(PLAYER, ByteBufInputStream)}.
     * */
    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}
}
