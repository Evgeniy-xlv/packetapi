package ru.xlv.packetapi.server.packet.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacketOut;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player for whom the packet is sending.
 * */
public interface IPacketOutBukkit extends IPacketOut {

    /**
     * This method will be called before sending.
     * */
    void write(Player player, ByteBufOutputStream bbos) throws IOException;

    /**
     *  @deprecated This method is not called for packets of this type, use {@link IPacketOutBukkit#write(Player, ByteBufOutputStream)}
     * */
    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}
}
