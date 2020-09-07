package ru.xlv.packetapi.common.registry;

import ru.xlv.packetapi.common.packet.IPacket;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serves for convenient registration of packets within the api.
 * <p>
 * This register does not take the packet identifier as input, but assigns it itself, depending on the order in which this packet is added to the register.
 * Before identifying packets, they will be sorted based on the modid under which they were added to the register.
 * <p>
 * This can be useful when you are working with the same packet handler from different mods.
 * */
public class PacketRegistry extends AbstractPacketRegistry {

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

    private final List<RegisterElement> registerElements = new ArrayList<>();

    /**
     * This method lets you register packets.
     * Their ids will be automatically generated.
     * @param modid is the modid of your mod.
     * */
    public PacketRegistry register(@Nonnull String modid, @Nonnull IPacket... packets) {
        for (IPacket packet : packets) {
            register(modid, packet);
        }
        return this;
    }

    /**
     * This method lets you register a packet.
     * Their ids will be automatically generated.
     * @param modid is the modid of your mod.
     * */
    public PacketRegistry register(@Nonnull String modid, @Nonnull IPacket packet) {
        RegisterElement element = findRegisterElement(modid);
        if (element == null) {
            registerElements.add(element = new RegisterElement(modid));
        }
        element.packets.add(packet);
        return this;
    }

    /**
     * The main method for sorting and registering packets in the system.
     * For the api to work, you should call this method after registering all packets.
     * */
    public PacketRegistry applyRegistration() {
        Collections.sort(registerElements);
        for (RegisterElement registerElement : registerElements) {
            for (IPacket packet : registerElement.packets) {
                registerWithGeneratedId(packet);
            }
        }
        registerElements.clear();
        return this;
    }

    private RegisterElement findRegisterElement(String modid) {
        for (RegisterElement registerElement : registerElements) {
            if(registerElement.modid.equals(modid)) {
                return registerElement;
            }
        }
        return null;
    }
}
