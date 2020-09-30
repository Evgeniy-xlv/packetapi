package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.IPacketInClient;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod;

import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "SecondPacketExample")
public class SecondPacketClientExample implements IPacketOutClient, IPacketInClient {
    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        bbos.writeUTF("Hello");
    }

    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        System.out.println(bbis.readUTF());
    }
}
