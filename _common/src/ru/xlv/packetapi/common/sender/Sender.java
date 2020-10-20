package ru.xlv.packetapi.common.sender;

import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.server.bukkit.PacketAPIBukkitPlugin;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.xlv.packetapi.common.sender.SenderStuff.*;

public final class Sender {

    /**
     * Starts a new pipeline to send the composable
     * */
    public static <T extends Composable> ComposableSide<T> composable(@Nonnull T composable) {
        return new ComposableSide<>(Stream.of(composable));
    }

    /**
     * Starts a new pipeline to send the callback to the server
     * */
    public static <CALLBACK extends ICallbackOut<RESULT>, RESULT> CallbackSender<CALLBACK, RESULT> callback(@Nonnull CALLBACK callback) {
        return new CallbackSender<>(Stream.of(callback));
    }

    /**
     * Starts a new pipeline to send the packet to the server
     * */
    public static <PACKET extends IPacketOutClient> ClientSender<PACKET> packet(@Nonnull PACKET packet) {
        return new ClientSender<>(Stream.of(packet));
    }

    /**
     * Starts a new pipeline to send the packet to the client from the forge-server side
     * */
    public static <PACKET extends IPacketOutServer> Target<PACKET, EntityPlayer, ServerSender<PACKET>> packet(@Nonnull PACKET packet) {
        return new Target<>(new ServerSender<>(Stream.of(packet)), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
    }

    /**
     * Starts a new pipeline to send the packet to the client from the bukkit-server side
     * */
    public static <PACKET extends IPacketOutBukkit> Target<PACKET, Player, BukkitSender<PACKET>> packet(@Nonnull PACKET packet) {
        return new Target<>(new BukkitSender<>(Stream.of(packet)), PacketAPIBukkitPlugin::getOnlinePlayers);
    }

    /**
     * Starts a new pipeline to send composables
     * */
    public static ComposableSide<Composable> composables(@Nonnull Composable... composables) {
        return new ComposableSide<>(Arrays.stream(composables));
    }

    /**
     * Starts a new pipeline to send composables
     * */
    public static ComposableSide<? extends Composable> composables(@Nonnull Collection<? extends Composable> composables) {
        return new ComposableSide<>(composables.stream());
    }

    /**
     * Starts a new pipeline to send composables
     * */
    public static ComposableSide<? extends Composable> composables(@Nonnull Stream<? extends Composable> composables) {
        return new ComposableSide<>(composables);
    }

    /**
     * Starts a new pipeline to send packets to the server
     * */
    @SafeVarargs
    public static <PACKET extends IPacketOutClient> ClientSender<PACKET> packetsClient(@Nonnull PACKET... packets) {
        return new ClientSender<>(Arrays.stream(packets));
    }

    /**
     * Starts a new pipeline to send packets to the server
     * */
    public static <PACKET extends IPacketOutClient> ClientSender<PACKET> packetsClient(@Nonnull Collection<PACKET> packets) {
        return new ClientSender<>(packets.stream());
    }

    /**
     * Starts a new pipeline to send packets to the server
     * */
    public static <PACKET extends IPacketOutClient> ClientSender<PACKET> packetsClient(@Nonnull Stream<PACKET> packetStream) {
        return new ClientSender<>(packetStream);
    }

    /**
     * Starts a new pipeline to send packets to the client from the forge-server side
     * */
    @SafeVarargs
    public static <PACKET extends IPacketOutServer> Target<PACKET, EntityPlayer, ServerSender<PACKET>> packetsServer(@Nonnull PACKET... packets) {
        return new Target<>(new ServerSender<>(Arrays.stream(packets)), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
    }

    /**
     * Starts a new pipeline to send packets to the client from the forge-server side
     * */
    public static <PACKET extends IPacketOutServer> Target<PACKET, EntityPlayer, ServerSender<PACKET>> packetsServer(@Nonnull Collection<PACKET> packets) {
        return new Target<>(new ServerSender<>(packets.stream()), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
    }

    /**
     * Starts a new pipeline to send packets to the client from the forge-server side
     * */
    public static <PACKET extends IPacketOutServer> Target<PACKET, EntityPlayer, ServerSender<PACKET>> packetsServer(@Nonnull Stream<PACKET> packetStream) {
        return new Target<>(new ServerSender<>(packetStream), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
    }

    /**
     * Starts a new pipeline to send packets to the client from the bukkit-server side
     * */
    @SafeVarargs
    public static <PACKET extends IPacketOutBukkit> Target<PACKET, Player, BukkitSender<PACKET>> packetsBukkit(@Nonnull PACKET... packets) {
        return new Target<>(new BukkitSender<>(Arrays.stream(packets)), PacketAPIBukkitPlugin::getOnlinePlayers);
    }

    /**
     * Starts a new pipeline to send packets to the client from the bukkit-server side
     * */
    public static <PACKET extends IPacketOutBukkit> Target<PACKET, Player, BukkitSender<PACKET>> packetsBukkit(@Nonnull Collection<PACKET> packets) {
        return new Target<>(new BukkitSender<>(packets.stream()), PacketAPIBukkitPlugin::getOnlinePlayers);
    }

    /**
     * Starts a new pipeline to send packets to the client from the bukkit-server side
     * */
    public static <PACKET extends IPacketOutBukkit> Target<PACKET, Player, BukkitSender<PACKET>> packetsBukkit(@Nonnull Stream<PACKET> packetStream) {
        return new Target<>(new BukkitSender<>(packetStream), PacketAPIBukkitPlugin::getOnlinePlayers);
    }
}
