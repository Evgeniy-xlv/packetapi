package ru.xlv.packetapi.server.bukkit;

import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.packet.autoreg.IAutoRegPacketScanner;
import ru.xlv.packetapi.server.bukkit.packet.ICallbackInBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketInBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AutoRegPacketScannerBukkit implements IAutoRegPacketScanner {

    private static final AutoRegPacketScannerBukkit INSTANCE = new AutoRegPacketScannerBukkit();

    private AutoRegPacketScannerBukkit() {}

    @Override
    public void register(Class<?> packetClass) {
        if (IPacket.class.isAssignableFrom(packetClass)) {
            AutoRegPacket annotation = packetClass.getAnnotation(AutoRegPacket.class);
            String channelName = annotation.channelName();
            if(isServerSidePacket(packetClass)) {
                try {
                    Constructor<?> constructor = packetClass.getConstructor();
                    constructor.setAccessible(true);
                    Object packet = constructor.newInstance();
                    int i = PacketHandlerBukkit.getInstance().registerPacket(channelName, (IPacket) packet);
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
        }
    }

    private boolean isServerSidePacket(Class<?> packetClass) {
        return IPacketOutBukkit.class.isAssignableFrom(packetClass) || IPacketInBukkit.class.isAssignableFrom(packetClass) || ICallbackInBukkit.class.isAssignableFrom(packetClass);
    }

    public static AutoRegPacketScannerBukkit getInstance() {
        return INSTANCE;
    }
}
