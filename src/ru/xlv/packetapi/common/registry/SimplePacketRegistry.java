package ru.xlv.packetapi.common.registry;

import ru.xlv.packetapi.common.packet.IPacket;

import javax.annotation.Nonnull;

public class SimplePacketRegistry extends AbstractPacketRegistry {

    /**
     * This method lets you register packets.
     * Their ids will be automatically generated.
     * */
    public SimplePacketRegistry register(@Nonnull IPacket... packets) {
        for (IPacket packet : packets) {
            registerWithGeneratedId(packet);
        }
        return this;
    }

    /**
     * This method lets you register a packet with specified id.
     * */
    @Override
    public SimplePacketRegistry register(int pid, @Nonnull IPacket packet) {
        return (SimplePacketRegistry) super.register(pid, packet);
    }
}
