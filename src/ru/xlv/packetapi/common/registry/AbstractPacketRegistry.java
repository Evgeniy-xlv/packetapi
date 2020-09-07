package ru.xlv.packetapi.common.registry;

import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ru.xlv.packetapi.common.packet.IPacket;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public abstract class AbstractPacketRegistry {

    protected static final Logger log = Logger.getLogger(PacketRegistry.class.getSimpleName());

    protected final TIntObjectMap<IPacket> registry = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    protected final TObjectIntMap<Class<? extends IPacket>> classRegistry = TCollections.synchronizedMap(new TObjectIntHashMap<>());

    private int packetIDCounter;

    protected void registerWithGeneratedId(@Nonnull IPacket packet) {
        int pid = getNextPacketIDCounter();
        register(pid, packet);
    }

    protected AbstractPacketRegistry register(int pid, @Nonnull IPacket packet) {
        if(registry.containsKey(pid)) {
            throw new RuntimeException("An error has occurred during registration of packet with pid=" + pid + ". Packet with specified pid is already exist!");
        }
        registry.put(pid, packet);
        classRegistry.put(packet.getClass(), pid);
        log.info("registered a " + packet.getClass().getName() + " with id " + pid);
        return this;
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
