package ru.xlv.packetapi.common;

import gnu.trove.map.TObjectIntMap;
import ru.xlv.packetapi.common.packet.EmptyConstructorException;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketNotFoundException;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.common.registry.PacketRegistry;

import javax.annotation.Nonnull;

public interface IPacketHandler {

    PacketRegistry getPacketRegistry();

    default <T extends IPacket> boolean isSupportedPacket(@Nonnull T packet) {
        return isSupportedPacket(packet.getClass());
    }

    <T extends IPacket> boolean isSupportedPacket(Class<T> aClass);

    default int getPacketId(@Nonnull String channelName, @Nonnull IPacket packet) {
        TObjectIntMap<Class<? extends IPacket>> classRegistry = getPacketRegistry().getSubRegistryByChannel(channelName).getClassRegistry();
        if(!classRegistry.containsKey(packet.getClass())) {
            throw new PacketNotFoundException("The id of packet " + packet + " not found!");
        }
        return classRegistry.get(packet.getClass());
    }

    default IPacket createPacketById(@Nonnull String channelName, int pid) {
        IPacket iPacket = null;
        PacketRegistry.SubPacketRegistry subRegistryByChannel = getPacketRegistry().getSubRegistryByChannel(channelName);
        if (subRegistryByChannel != null) {
            iPacket = subRegistryByChannel.getRegistry().get(pid);
            if (iPacket != null) {
                try {
                    iPacket = iPacket.getClass().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new EmptyConstructorException("An empty constructor of class " + iPacket.getClass() + " isn't public or doesn't exist.", e);
                }
            }
        }
        return iPacket;
    }

    void setupNetworkChannel(String channelName);

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    default int registerPacket(String channelName, IPacket packet) throws PacketRegistrationException {
        if(isSupportedPacket(packet)) {
            try {
                if (getPacketRegistry().getSubRegistryByChannel(channelName) == null) {
                    setupNetworkChannel(channelName);
                }
                return getPacketRegistry().register(channelName, packet);
            } catch (PacketRegistrationException e) {
                e.printStackTrace();
            }
            return -1;
        }
        throw new PacketRegistrationException("An input packet " + packet.getClass().getName() + " isn't a server packet.");
    }
}
