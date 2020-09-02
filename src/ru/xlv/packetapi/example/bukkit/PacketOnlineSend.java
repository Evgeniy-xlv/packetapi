package ru.xlv.packetapi.example.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.server.packet.bukkit.IPacketOutBukkit;

import java.io.IOException;

public class PacketOnlineSend implements IPacketOutBukkit {
    @Override
    public void write(Player player, ByteBufOutputStream bbos) throws IOException {
        bbos.writeInt((int) ((System.currentTimeMillis() - OnlineTimeSenderPlugin.ONLINE_MAP.get(player.getUniqueId())) / 1000));
        bbos.writeUTF("Я срока, я строка");
    }
}
