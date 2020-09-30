package ru.xlv.packetapi.server.bukkit.packet;

import org.bukkit.entity.Player;
import ru.xlv.packetapi.server.IPacketOutServerRaw;

/**
 * A special packet for the server side, allows you to work with the player for whom the packet is sending.
 * */
public interface IPacketOutBukkit extends IPacketOutServerRaw<Player> {
}
