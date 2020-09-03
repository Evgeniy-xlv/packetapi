package ru.xlv.packetapi.server.packet.forge;

import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.packet.IPacketCallback;

public interface IPacketCallbackOnServer extends IPacketCallbackOnServerRaw<EntityPlayerMP> {
}
