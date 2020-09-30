package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "SecondPacketExample")
public class SecondPacketServerExample implements IPacketInServer, IPacketOutServer {
    @Override
    public void read(EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {
        System.out.println(bbis.readUTF());
    }

    @Override
    public void write(EntityPlayer entityPlayer, ByteBufOutputStream bbos) throws IOException {
        bbos.writeUTF("Hello for you");
    }
}
