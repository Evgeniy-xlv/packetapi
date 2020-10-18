package ru.xlv.packetapi.example.a1_12_2.bukkit;

import ru.xlv.packetapi.client.packet.IPacketInClient;
import ru.xlv.packetapi.common.packet.registration.Packet;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

@Packet
public class PacketOnlineReceive implements IPacketInClient {
    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        int seconds = bbis.readInt();
        System.out.println("Your actual played time: " + seconds + " sec");
        System.out.println(bbis.readUTF());
    }
}
