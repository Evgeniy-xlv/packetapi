package ru.xlv.packetapi.common;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.capability.AbstractNetworkAdapter;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.packet.IPacketOut;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.logging.Logger;

public abstract class PacketHandler<PLAYER> implements IPacketHandler {

    private final Logger logger;

    private final AbstractNetworkAdapter<PLAYER> networkAdapter;

    private final PacketRegistry packetRegistry;

    private final String channelName;

    public PacketHandler(@Nonnull PacketRegistry packetRegistry, @Nonnull String channelName) {
        this.packetRegistry = packetRegistry;
        this.channelName = channelName;
        //noinspection UnstableApiUsage
        this.networkAdapter = PacketAPI.INSTANCE.getCapabilityAdapter().newNetworkAdapter(new TypeToken<PLAYER>(getClass()) {}.getRawType(), channelName, this::onClientPacketReceived, this::onServerPacketReceived);
        this.logger = Logger.getLogger(this.getClass().getSimpleName() + ":" + channelName);
    }

    protected void onClientPacketReceived(ByteBufInputStream bbis) throws IOException {}

    protected void onServerPacketReceived(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException {}

    public void sendPacketToServer(@Nonnull IPacketOut packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(packet));
            packet.write(byteBufOutputStream);
            sendPacketToServer(byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToServer(@Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        networkAdapter.sendToServer(bbos);
        bbos.close();
    }

    public void sendPacketToPlayer(@Nullable PLAYER entityPlayer, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if (entityPlayer == null) {
            logger.warning("Unable to send a packet to null player!");
            return;
        }
        networkAdapter.sendTo(entityPlayer, bbos);
        bbos.close();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected AbstractNetworkAdapter<PLAYER> getNetworkAdapter() {
        return networkAdapter;
    }

    public PacketRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    public String getChannelName() {
        return this.channelName;
    }
}
