package ru.xlv.packetapi.common.packet.registration;

import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {

    private final Map<String, SubPacketRegistry> REGISTRIES = new HashMap<>();

    /**
     * Registers packets for the specified channel name.
     * Their ids will be automatically generated.
     * */
    public <T extends IPacket> int[] register(@Nonnull String channelName, @Nonnull T... packets) throws PacketRegistrationException {
        int[] ii = new int[packets.length];
        for (int i = 0; i < packets.length; i++) {
            ii[i] = register(channelName, packets[i]);
        }
        return ii;
    }

    /**
     * Registers a packet for the specified channel name.
     * Their ids will be automatically generated.
     * */
    public <T extends IPacket> int register(@Nonnull String channelName, @Nonnull T packet) throws PacketRegistrationException {
        SubPacketRegistry subPacketRegistry = REGISTRIES.computeIfAbsent(channelName, s -> new SubPacketRegistry());
        for (String channel : REGISTRIES.keySet()) {
            SubPacketRegistry subPacketRegistry1 = REGISTRIES.get(channel);
            if(subPacketRegistry1.getClassRegistry().containsKey(packet.getClass())) {
                throw new PacketRegistrationException("An error has occurred during registration of packet " + packet.getClass().getName() + " in channel " + channelName + ". It is already registered in channel " + channel);
            }
        }
        return subPacketRegistry.registerWithGeneratedId(packet);
    }

    @Nullable
    public <T extends IPacket> String findChannelNameByPacket(@Nonnull T packet) {
        return findChannelNameByPacket(packet.getClass());
    }

    @Nullable
    public <T extends IPacket> String findChannelNameByPacket(@Nullable Class<T> packet) {
        if(packet == null) return null;
        for (String channelName : REGISTRIES.keySet()) {
            SubPacketRegistry subPacketRegistry = REGISTRIES.get(channelName);
            if (subPacketRegistry.getClassRegistry().containsKey(packet)) {
                return channelName;
            }
        }
        return null;
    }

    public SubPacketRegistry getSubRegistryByChannel(String channelName) {
        return REGISTRIES.get(channelName);
    }

    public static class SubPacketRegistry {

        protected final TIntObjectMap<IPacket> registry = TCollections.synchronizedMap(new TIntObjectHashMap<>());
        protected final TObjectIntMap<Class<? extends IPacket>> classRegistry = TCollections.synchronizedMap(new TObjectIntHashMap<>());

        private int packetIDCounter;

        private SubPacketRegistry() {}

        private int registerWithGeneratedId(@Nonnull IPacket packet) {
            int pid = getNextPacketIDCounter();
            register(pid, packet);
            return pid;
        }

        private void register(int pid, @Nonnull IPacket packet) {
            if(registry.containsKey(pid)) {
                throw new RuntimeException("An error has occurred during registration of a packet with id=" + pid + ". A packet with the specified id is already exist!");
            }
            registry.put(pid, packet);
            classRegistry.put(packet.getClass(), pid);
        }

        private int getNextPacketIDCounter() {
            int packetId = packetIDCounter;
            packetIDCounter++;
            return packetId;
        }

        public TIntObjectMap<IPacket> getRegistry() {
            return registry;
        }

        public TObjectIntMap<Class<? extends IPacket>> getClassRegistry() {
            return classRegistry;
        }
    }
}
