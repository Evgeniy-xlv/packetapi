package ru.xlv.packetapi.example.a1_7_10.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.example.a1_7_10.autoreg.main.AutoRegMod;
import ru.xlv.packetapi.server.forge.packet.ICallbackInServer;
import ru.xlv.packetapi.server.forge.packet.PacketCallbackSender;

import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "FirstPacketExample")
public class FirstPacketServerExample implements ICallbackInServer {
    @Override
    public void read(EntityPlayer entityPlayer, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException {
        System.out.println(readObject(bbis, String.class));
    }

    @Override
    public void write(EntityPlayer entityPlayer, ByteBufOutputStream bbos) throws IOException {
        writeObject(bbos, "Pong.");
    }
}
