package ru.xlv.packetapi.common;

import com.google.common.reflect.TypeToken;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.capability.AbstractNetworkAdapter;
import ru.xlv.packetapi.common.composable.Composer;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class PacketHandler<PLAYER> implements IPacketHandler {

    private final Logger logger;

    private final AbstractNetworkAdapter<PLAYER> networkAdapter;

    private final AbstractPacketRegistry packetRegistry;

    private final String channelName;

    private final Composer composer = IPacket.COMPOSER;

    public PacketHandler(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        this.packetRegistry = packetRegistry;
        this.channelName = channelName;
        //noinspection UnstableApiUsage
        this.networkAdapter = PacketAPI.getCapabilityAdapter().newNetworkAdapter(new TypeToken<PLAYER>(getClass()) {}.getRawType(), channelName, this::onClientPacketReceived, this::onServerPacketReceived);
        this.logger = Logger.getLogger(this.getClass().getSimpleName() + ":" + channelName);
    }

    protected void onClientPacketReceived(ByteBufInputStream bbis) throws IOException {}

    protected void onServerPacketReceived(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException {}

    protected Logger getLogger() {
        return logger;
    }

    protected AbstractNetworkAdapter<PLAYER> getNetworkAdapter() {
        return networkAdapter;
    }

    protected Composer getComposer() {
        return composer;
    }

    public AbstractPacketRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    public String getChannelName() {
        return this.channelName;
    }
}
