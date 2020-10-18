package ru.xlv.packetapi.common.packet.registration;

import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.client.packet.IPacketInClient;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.server.forge.PacketHandlerServer;
import ru.xlv.packetapi.server.forge.packet.ICallbackInServer;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PacketRegistrationRouterForge extends PacketRegistrationRouter {

    private static final PacketRegistrationRouterForge INSTANCE = new PacketRegistrationRouterForge();

    private PacketRegistrationRouterForge() {}

    @Override
    protected void register(@Nonnull String channelName, @Nonnull Class<?> packetClass) {
        if (IPacket.class.isAssignableFrom(packetClass)) {
            Packet annotation = packetClass.getAnnotation(Packet.class);
            if(isServerSidePacket(packetClass)) {
                try {
                    Constructor<?> constructor = packetClass.getConstructor();
                    constructor.setAccessible(true);
                    Object packet = constructor.newInstance();
                    int i = PacketHandlerServer.getInstance().registerPacket(channelName, (IPacket) packet);
                    LOGGER.info("A packet " + packetClass.getName() + " was registered. Context:" +
                            " registryName=" + (annotation.registryName().equals("") ? packetClass.getName() : annotation.registryName()) +
                            " channelName=" + channelName +
                            " id=" + i +
                            " side=SERVER"
                    );
                } catch (InstantiationException | IllegalAccessException | PacketRegistrationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if(isClientSidePacket(packetClass)) {
                try {
                    Constructor<?> constructor = packetClass.getConstructor();
                    constructor.setAccessible(true);
                    Object packet = constructor.newInstance();
                    int i = PacketHandlerClient.getInstance().registerPacket(channelName, (IPacket) packet);
                    LOGGER.info("A packet " + packetClass.getName() + " was registered. Context:" +
                            " registryName=" + (annotation.registryName().equals("") ? packetClass.getName() : annotation.registryName()) +
                            " channelName=" + channelName +
                            " id=" + i +
                            " side=CLIENT"
                    );
                } catch (PacketRegistrationException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isServerSidePacket(Class<?> packetClass) {
        return IPacketOutServer.class.isAssignableFrom(packetClass) || IPacketInServer.class.isAssignableFrom(packetClass) || ICallbackInServer.class.isAssignableFrom(packetClass);
    }

    private boolean isClientSidePacket(Class<?> packetClass) {
        return IPacketOutClient.class.isAssignableFrom(packetClass) || IPacketInClient.class.isAssignableFrom(packetClass) || ICallbackOut.class.isAssignableFrom(packetClass);
    }

    public static PacketRegistrationRouterForge getInstance() {
        return INSTANCE;
    }
}
