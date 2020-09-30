package ru.xlv.packetapi.common.packet.autoreg;

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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoRegPacketScanner implements IAutoRegPacketScanner {

    private static final AutoRegPacketScanner INSTANCE = new AutoRegPacketScanner();

    private AutoRegPacketScanner() {}

    @Override
    public void scanThenRegister(@Nonnull String packagePath) {
        LOGGER.info("Scanning " + packagePath + " for packets...");
        Set<Class<?>> packetClasses = scanPacketClasses(packagePath).stream()
                .sorted((o1, o2) -> {
                    AutoRegPacket annotation = o1.getAnnotation(AutoRegPacket.class);
                    AutoRegPacket annotation1 = o2.getAnnotation(AutoRegPacket.class);
                    String a = !annotation.registryName().equals("") ? annotation.registryName() : o1.getName();
                    String b = !annotation1.registryName().equals("") ? annotation1.registryName() : o2.getName();
                    return a.compareTo(b);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Class<?> packetClass : packetClasses) {
            if (IPacket.class.isAssignableFrom(packetClass)) {
                AutoRegPacket annotation = packetClass.getAnnotation(AutoRegPacket.class);
                String channelName = annotation.channelName();
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
    }

    private boolean isServerSidePacket(Class<?> packetClass) {
        return IPacketOutServer.class.isAssignableFrom(packetClass) || IPacketInServer.class.isAssignableFrom(packetClass) || ICallbackInServer.class.isAssignableFrom(packetClass);
    }

    private boolean isClientSidePacket(Class<?> packetClass) {
        return IPacketOutClient.class.isAssignableFrom(packetClass) || IPacketInClient.class.isAssignableFrom(packetClass) || ICallbackOut.class.isAssignableFrom(packetClass);
    }

    public static AutoRegPacketScanner getInstance() {
        return INSTANCE;
    }
}
