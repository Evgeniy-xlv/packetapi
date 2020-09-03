package ru.xlv.packetapi.common;

import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ru.xlv.packetapi.common.packet.IPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Служит для удобной регистрации пакетов в рамках апи.
 *
 * Данный регистр не берет на вход идентификатор пакета, а присваевает его сам, в зависимости от очередности добавления этого пакета в регистр.
 * Перед присвоением идентификаторов пакетам, они будут отсортированы в зависимости от modid, под эгидой которого они были добавлены в регистр.
 * */
public class PacketRegistry {

    private static final Logger log = Logger.getLogger(PacketRegistry.class.getName());

    private static class RegisterElement implements Comparable<RegisterElement> {
        private final String modid;
        private final List<IPacket> packets = new ArrayList<>();

        public RegisterElement(String modid) {
            this.modid = modid;
        }

        @Override
        public int compareTo(RegisterElement o) {
            return modid.compareTo(o.modid);
        }
    }

    private final TIntObjectMap<IPacket> REGISTRY = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    private final TObjectIntMap<Class<? extends IPacket>> CLASS_REGISTRY = TCollections.synchronizedMap(new TObjectIntHashMap<>());

    private final List<RegisterElement> registerElements = new ArrayList<>();

    private int packetIDCounter;

    public PacketRegistry register(String modid, IPacket... packets) {
        for (IPacket packet : packets) {
            register(modid, packet);
        }
        return this;
    }

    public PacketRegistry register(String modid, IPacket packet) {
        RegisterElement element = getRegisterElement(modid);
        if (element == null) {
            registerElements.add(element = new RegisterElement(modid));
        }
        element.packets.add(packet);
        return this;
    }

    /**
     * Основной метод сортировки и регистрации пакетов в системе.
     * Для работы апи, следует вызывать этот метод после регистрации всех пакетов.
     * */
    public PacketRegistry applyRegistration() {
        Collections.sort(registerElements);
        for (RegisterElement registerElement : registerElements) {
            for (IPacket packet : registerElement.packets) {
                register(packet);
            }
        }
        registerElements.clear();
        return this;
    }

    private RegisterElement getRegisterElement(String modid) {
        for (RegisterElement registerElement : registerElements) {
            if(registerElement.modid.equals(modid)) {
                return registerElement;
            }
        }
        return null;
    }

    private void register(IPacket packet) {
        int pid = getPacketIDCounter();
        REGISTRY.put(pid, packet);
        CLASS_REGISTRY.put(packet.getClass(), pid);
        log.info("registered a " + packet.getClass().getName() + " with id " + pid);
    }

    private int getPacketIDCounter() {
        int packetId = packetIDCounter;
        packetIDCounter++;
        return packetId;
    }

    public TIntObjectMap<IPacket> getRegistry() {
        return REGISTRY;
    }

    public TObjectIntMap<Class<? extends IPacket>> getClassRegistry() {
        return CLASS_REGISTRY;
    }
}
