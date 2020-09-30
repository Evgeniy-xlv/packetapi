package ru.xlv.packetapi.server.bukkit.packet;


import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player from whom the package came.
 * */
public interface IPacketInBukkit extends IPacket {

    /**
     * This method will be called when the packet is received.
     * */
    void read(Player player, ByteBufInputStream bbis) throws IOException;
}
