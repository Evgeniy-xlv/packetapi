package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod;

import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "ThirdPacketExample")
public class ThirdPacketClientExample implements IPacketOutClient {
    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        System.out.println("ThirdPacketClientExample");
    }
}
