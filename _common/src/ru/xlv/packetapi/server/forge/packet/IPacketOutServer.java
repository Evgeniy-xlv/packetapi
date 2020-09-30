package ru.xlv.packetapi.server.forge.packet;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.server.IPacketOutServerRaw;

/**
 * A special packet for the server side, allows you to work with the player for whom the packet is sending.
 * */
public interface IPacketOutServer extends IPacketOutServerRaw<EntityPlayer> {
}
