package ru.xlv.packetapi.example.a1_7_10.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.packet.registration.Packet;

import java.io.IOException;

@Packet(registryName = "SecondPacketExample")
public class SecondPacketClientExample implements IPacketOutClient {
    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        bbos.writeUTF("Hello");
    }
}
