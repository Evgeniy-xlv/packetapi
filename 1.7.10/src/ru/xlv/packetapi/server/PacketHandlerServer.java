package ru.xlv.packetapi.server;

import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;

import javax.annotation.Nonnull;

/**
 * @see PacketHandlerServerRaw
 * */
public class PacketHandlerServer extends PacketHandlerServerRaw<EntityPlayerMP> {
    public PacketHandlerServer() {
    }

    public PacketHandlerServer(@Nonnull String channelName) {
        super(channelName);
    }

    public PacketHandlerServer(@Nonnull AbstractPacketRegistry packetRegistry) {
        super(packetRegistry);
    }

    public PacketHandlerServer(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        super(packetRegistry, channelName);
    }
}
