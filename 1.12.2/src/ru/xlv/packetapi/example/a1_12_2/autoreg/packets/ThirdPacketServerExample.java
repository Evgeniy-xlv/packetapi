package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;

import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "ThirdPacketExample")
public class ThirdPacketServerExample implements IPacketInServer {
    @Override
    public void read(EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {
        System.out.println("ThirdPacketExample");
    }
}
