package ru.xlv.packetapi.capability;

import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.util.UUID;
import java.util.stream.Stream;

public interface ICapabilityAdapter {

    void scheduleTaskSync(Runnable runnable);

    void scheduleServerTaskSync(Runnable runnable);

    boolean isServerThread(Thread thread);

    UUID getPlayerEntityUniqueId(Object playerEntity);

    String getPlayerEntityName(Object playerEntity);

    double getDistanceBetween(Object entity, Object entity1);

    double getDistanceBetween(Object entity, double x, double y, double z);

    <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass);

    <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived);
}
