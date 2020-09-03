package ru.xlv.packetapi.example.bukkit;

import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public class PacketOnlineReceive implements IPacketIn {
    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        int seconds = bbis.readInt();
        System.out.println("Ваш текущий онлайн: " + seconds + " сек");
        System.out.println(bbis.readUTF());
    }
}
