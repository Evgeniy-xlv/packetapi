package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.registration.Packet;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;

import java.io.IOException;

@Packet(registryName = "ThirdPacketExample")
public class ThirdPacketServerExample implements IPacketInServer {
    @Override
    public void read(EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {
        System.out.println("ThirdPacketExample");
    }
}
