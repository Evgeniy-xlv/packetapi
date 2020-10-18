package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.packet.registration.Packet;

import java.io.IOException;

@Packet(registryName = "ThirdPacketExample")
public class ThirdPacketClientExample implements IPacketOutClient {
    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        System.out.println("ThirdPacketClientExample");
    }
}
