package ru.xlv.packetapi.capability;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public abstract class AbstractNetworkAdapter<PLAYER> {

    protected final String channelName;

    private final ThrConsumer<ByteBufInputStream> clientPacketReceived;
    private final ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived;

    public AbstractNetworkAdapter(String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        this.channelName = channelName;
        this.clientPacketReceived = clientPacketReceived;
        this.serverPacketReceived = serverPacketReceived;
    }

    protected void onClientPacketReceived(ByteBufInputStream byteBufInputStream) throws IOException {
        clientPacketReceived.accept(byteBufInputStream);
    }

    protected void onServerPacketReceived(PLAYER player, ByteBufInputStream byteBufInputStream) throws IOException {
        serverPacketReceived.accept(player, byteBufInputStream);
    }

    public abstract void sendTo(PLAYER player, ByteBufOutputStream byteBufOutputStream);

    public abstract void sendToServer(ByteBufOutputStream byteBufOutputStream);
}
