package ru.xlv.packetapi.server;

import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.IPacketHandler;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public interface IPacketHandlerServer<PLAYER, PACKET_OUT extends IPacketOutServerRaw<PLAYER>> extends IPacketHandler {

    default <T extends IPacket> void processServerAnnotations(T packet, Map<Class<? extends IPacket>, PacketData> packetDataMap) {
        PacketData packetData = new PacketData();
        {
            AsyncPacket annotation = packet.getClass().getAnnotation(AsyncPacket.class);
            if (annotation != null) {
                packetData.isAsync = true;
            }
        }
        {
            ControllablePacket annotation = packet.getClass().getAnnotation(ControllablePacket.class);
            if (annotation != null) {
                packetData.callWriteAnyway = annotation.callWriteAnyway();
                packetData.requestLimit = annotation.limit();
                packetData.requestPeriod = annotation.period();
            }
        }
        if(packetData.requestPeriod != -1) {
            packetData.requestController = new RequestController.Periodic<>(packetData.requestPeriod);
        } else if(packetData.requestLimit != -1) {
            packetData.requestController = new RequestController.Limited<>(packetData.requestLimit);
        }
        packetDataMap.put(packet.getClass(), packetData);
    }

    /**
     * Sends a composable object to the client side.
     * @see Composable
     * */
    <T extends Composable> void sendComposable(@Nonnull PLAYER player, @Nonnull T composable);

    default void sendPacketsToPlayer(@Nonnull String channelName, @Nonnull PLAYER player, @Nonnull PACKET_OUT... packets) {
        for (PACKET_OUT packet : packets) {
            sendPacketToPlayer(channelName, player, packet);
        }
    }

    /**
     * Sends a packet to all players on the server.
     * */
    default void sendPacketToAll(@Nonnull String channelName, @Nonnull PACKET_OUT packetOut) {
        getOnlinePlayers()
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    /**
     * Sends a packet to all players on the server, except for the specified player.
     * */
    default void sendPacketToAllExcept(@Nonnull String channelName, @Nonnull PLAYER player, @Nonnull PACKET_OUT packetOut) {
        getOnlinePlayers()
                .filter(p -> player != p)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    /**
     * Sends a packet to all players around the point in radius.
     * */
    default void sendPacketToAllAround(@Nonnull String channelName, double x, double y, double z, double radius, @Nonnull PACKET_OUT packetOut) {
        getOnlinePlayers()
                .filter(p -> PacketAPI.getCapabilityAdapter().getDistanceBetween(p, x, y, z) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    /**
     * Sends a packet to all players around the specified player in radius.
     * */
    default void sendPacketToAllAround(@Nonnull String channelName, @Nonnull PLAYER player, double radius, @Nonnull PACKET_OUT packetOut) {
        getOnlinePlayers()
                .filter(p -> PacketAPI.getCapabilityAdapter().getDistanceBetween(p, player) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    /**
     * Sends a packet to all players around the specified player in radius, except for this player.
     * */
    default void sendPacketToAllAroundExcept(@Nonnull String channelName, @Nonnull PLAYER player, double radius, @Nonnull PACKET_OUT packetOut) {
        getOnlinePlayers()
                .filter(p -> player != p)
                .filter(p -> PacketAPI.getCapabilityAdapter().getDistanceBetween(p, player) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    void sendPacketToPlayer(@Nonnull String channelName, @Nonnull PLAYER player, @Nonnull PACKET_OUT packet);

    Stream<? extends PLAYER> getOnlinePlayers();

    class PacketData {
        public RequestController<UUID> requestController;
        public boolean isAsync;
        public long requestPeriod;
        public int requestLimit;
        public boolean callWriteAnyway;
    }
}
