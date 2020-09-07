package ru.xlv.packetapi.client;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;

import javax.annotation.Nonnull;

/**
 * @see PacketHandlerClientRaw
 * */
public class PacketHandlerClient extends PacketHandlerClientRaw<EntityPlayer> {
    public PacketHandlerClient() {
    }

    public PacketHandlerClient(@Nonnull String channelName) {
        super(channelName);
    }

    public PacketHandlerClient(@Nonnull AbstractPacketRegistry packetRegistry) {
        super(packetRegistry);
    }

    public PacketHandlerClient(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        super(packetRegistry, channelName);
    }

    public PacketHandlerClient(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName, long callbackResultWaitTimeout, long defaultCheckResultPeriod) {
        super(packetRegistry, channelName, callbackResultWaitTimeout, defaultCheckResultPeriod);
    }
}
