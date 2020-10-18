package ru.xlv.packetapi.example.a1_12_2.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.registration.Packet;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;

import java.io.IOException;

@Packet
public class PacketOnlineSend implements IPacketOutBukkit {
    @Override
    public void write(Player player, ByteBufOutputStream bbos) throws IOException {
        bbos.writeInt((int) ((System.currentTimeMillis() - OnlineTimeSenderPlugin.ONLINE_MAP.get(player.getUniqueId())) / 1000));
        bbos.writeUTF("I'm a string, just a string");
    }
}
