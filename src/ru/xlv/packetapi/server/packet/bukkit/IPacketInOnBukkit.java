package ru.xlv.packetapi.server.packet.bukkit;


import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player from whom the package came.
 * */
public interface IPacketInOnBukkit extends IPacketIn {

    /**
     * This method will be called when the packet is received.
     * */
    void read(Player player, ByteBufInputStream bbis) throws IOException;

    /**
     * @deprecated This method is not called for packets of this type, use {@link IPacketInOnBukkit#read(Player, ByteBufInputStream)}.
     * */
    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}
}
