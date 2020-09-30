package ru.xlv.packetapi.capability;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNetworkAdapter<PLAYER> {

    protected final String channelName;

    private final List<ThrConsumer<ByteBufInputStream>> clientPacketReceiveListeners = new ArrayList<>();
    private final List<ThrBiConsumer<PLAYER, ByteBufInputStream>> serverPacketReceiveListeners = new ArrayList<>();

    public AbstractNetworkAdapter(String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        this.channelName = channelName;
        addClientPacketReceiveListener(clientPacketReceived);
        addServerPacketReceiveListener(serverPacketReceived);
    }

    public void addClientPacketReceiveListener(ThrConsumer<ByteBufInputStream> clientPacketReceived) {
        clientPacketReceiveListeners.add(clientPacketReceived);
    }

    public void addServerPacketReceiveListener(ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        serverPacketReceiveListeners.add(serverPacketReceived);
    }

    protected void onClientPacketReceived(ByteBufInputStream byteBufInputStream) throws IOException {
        for (ThrConsumer<ByteBufInputStream> clientPacketReceiveListener : clientPacketReceiveListeners) {
            clientPacketReceiveListener.accept(byteBufInputStream);
        }
    }

    protected void onServerPacketReceived(PLAYER player, ByteBufInputStream byteBufInputStream) throws IOException {
        for (ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceiveListener : serverPacketReceiveListeners) {
            serverPacketReceiveListener.accept(player, byteBufInputStream);
        }
    }

    public abstract void sendTo(PLAYER player, ByteBufOutputStream byteBufOutputStream);

    public abstract void sendToServer(ByteBufOutputStream byteBufOutputStream);
}
