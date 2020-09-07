package ru.xlv.packetapi.server.packet.forge;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacketOut;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player for whom the packet is sending.
 * */
public interface IPacketOutServerRaw<PLAYER> extends IPacketOut {

    /**
     * This method will be called before sending.
     * */
    void write(PLAYER entityPlayer, ByteBufOutputStream bbos) throws IOException;

    /**
     * @deprecated This method is not called for packets of this type, use {@link IPacketOutServerRaw#write(PLAYER, ByteBufOutputStream)}.
     * */
    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}
}
