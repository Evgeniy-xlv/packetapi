package ru.xlv.packetapi.server.forge.packet;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * A special packet for the server side, allows you to work with the player from whom the package came.
 * */
public interface IPacketInServer extends IPacket {

    /**
     * This method will be called when the packet is received.
     * */
    void read(EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException;
}
