package ru.xlv.packetapi.client;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.PacketRegistry;

public class PacketHandlerClient extends PacketHandlerClientRaw<EntityPlayer> {
    public PacketHandlerClient(PacketRegistry packetRegistry, String channelName) {
        super(packetRegistry, channelName);
    }
}
