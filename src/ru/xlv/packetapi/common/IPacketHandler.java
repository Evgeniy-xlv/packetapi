package ru.xlv.packetapi.common;

import ru.xlv.packetapi.common.packet.IPacket;

import javax.annotation.Nonnull;

public interface IPacketHandler {

    String getChannelName();

    PacketRegistry getPacketRegistry();

    default int getPacketId(@Nonnull IPacket packet) {
        if(!getPacketRegistry().getClassRegistry().containsKey(packet.getClass())) {
            throw new RuntimeException("The id of packet " + packet + " not found!");
        }
        return getPacketRegistry().getClassRegistry().get(packet.getClass());
    }

    default IPacket getPacketById(int pid) {
        IPacket iPacket = getPacketRegistry().getRegistry().get(pid);
        if (iPacket != null) {
            try {
                iPacket = iPacket.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return iPacket;
    }
}
