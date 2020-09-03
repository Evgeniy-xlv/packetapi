package ru.xlv.packetapi.server;

import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.PacketRegistry;

public class PacketHandlerServer extends PacketHandlerServerRaw<EntityPlayerMP> {
    public PacketHandlerServer(PacketRegistry packetRegistry, String channelName) {
        super(packetRegistry, channelName);
    }
}
