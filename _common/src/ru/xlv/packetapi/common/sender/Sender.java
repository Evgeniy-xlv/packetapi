package ru.xlv.packetapi.common.sender;

import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.server.bukkit.PacketAPIBukkitPlugin;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

import static ru.xlv.packetapi.common.sender.SenderStuff.*;

public abstract class Sender<PACKET extends IPacket, T> {

    protected final PACKET packet;

    Sender(PACKET packet) {
        this.packet = packet;
    }

    abstract T send();

    public static <T extends Composable> ComposableSide<T> composable(@Nonnull T composable) {
        return new ComposableSide<>(composable);
    }

    public static <PACKET extends ICallbackOut<RESULT>, RESULT> CallbackSender<PACKET, RESULT> callback(@Nonnull PACKET packet) {
        return new CallbackSender<>(packet);
    }

    public static <PACKET extends IPacketOutClient> ClientSender<PACKET> packet(@Nonnull PACKET packet) {
        return new ClientSender<>(packet);
    }

    public static <PACKET extends IPacketOutServer> Target<PACKET, EntityPlayer, ServerSender<PACKET>> packet(@Nonnull PACKET packet) {
        return new Target<>(new ServerSender<>(packet), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
    }

    public static <PACKET extends IPacketOutBukkit> Target<PACKET, Player, BukkitSender<PACKET>> packet(@Nonnull PACKET packet) {
        return new Target<>(new BukkitSender<>(packet), PacketAPIBukkitPlugin::getOnlinePlayers);
    }
}
