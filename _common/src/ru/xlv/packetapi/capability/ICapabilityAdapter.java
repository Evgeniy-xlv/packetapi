package ru.xlv.packetapi.capability;

import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.packet.registration.PacketRegistrationRouterForge;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;
import ru.xlv.packetapi.server.forge.PacketHandlerServer;

import java.util.UUID;
import java.util.stream.Stream;

public interface ICapabilityAdapter {

    default void preConstruct(boolean isServerSide, Stream<Class<?>> modClasses) {
        if (!isServerSide) {
            PacketHandlerClient.getInstance();
        } else {
            PacketHandlerServer.getInstance();
        }
        modClasses
                .filter(aClass -> aClass.isAnnotationPresent(PacketSubscriber.class))
                .forEach(PacketRegistrationRouterForge.getInstance()::scanThenRegister);
    }

    void scheduleTaskSync(Runnable runnable);

    void scheduleServerTaskSync(Runnable runnable);

    boolean isServerThread(Thread thread);

    UUID getPlayerEntityUniqueId(Object playerEntity);

    String getPlayerEntityName(Object playerEntity);

    double getDistanceBetween(Object entity, Object entity1);

    double getDistanceBetween(Object entity, double x, double y, double z);

    int getPlayerDimension(Object playerEntity);

    <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass);

    <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName);

    interface IVersionChecker {
        boolean check();
    }
}
