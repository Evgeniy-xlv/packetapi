package ru.xlv.packetapi.common;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.capability.AbstractNetworkAdapter;
import ru.xlv.packetapi.common.composable.Composer;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.registry.PacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class PacketHandlerForge implements IPacketHandler {

    private static final Map<String, AbstractNetworkAdapter<EntityPlayer>> NETWORK_ADAPTER_REGISTRY = new HashMap<>();

    private final PacketRegistry packetRegistry = new PacketRegistry();

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    private final Composer composer = IPacket.COMPOSER;

    protected void createNetworkAdapter(String channelName) {
        getLogger().info("Trying to register a new network channel " + channelName + "...");
        ThrConsumer<ByteBufInputStream> byteBufInputStreamThrConsumer = bbis -> onClientPacketReceived(channelName, bbis);
        ThrBiConsumer<EntityPlayer, ByteBufInputStream> entityPlayerByteBufInputStreamThrBiConsumer = (entityPlayer, bbis) -> onServerPacketReceived(channelName, entityPlayer, bbis);
        if(NETWORK_ADAPTER_REGISTRY.containsKey(channelName)) {
            getLogger().warning("A new network channel " + channelName + " is already registered.");
            AbstractNetworkAdapter<EntityPlayer> entityPlayerAbstractNetworkAdapter = NETWORK_ADAPTER_REGISTRY.get(channelName);
            entityPlayerAbstractNetworkAdapter.addClientPacketReceiveListener(byteBufInputStreamThrConsumer);
            entityPlayerAbstractNetworkAdapter.addServerPacketReceiveListener(entityPlayerByteBufInputStreamThrBiConsumer);
            return;
        }
        AbstractNetworkAdapter<EntityPlayer> networkAdapter = PacketAPI.getCapabilityAdapter().newNetworkAdapter(EntityPlayer.class, channelName, byteBufInputStreamThrConsumer, entityPlayerByteBufInputStreamThrBiConsumer);
        NETWORK_ADAPTER_REGISTRY.put(channelName, networkAdapter);
        getLogger().info("A new network channel " + channelName + " successfully registered.");
    }

    protected void onClientPacketReceived(String channelName, ByteBufInputStream bbis) throws IOException {}

    protected void onServerPacketReceived(String channelName, EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {}

    protected AbstractNetworkAdapter<EntityPlayer> getNetworkAdapter(String channelName) {
        return NETWORK_ADAPTER_REGISTRY.get(channelName);
    }

    @Override
    public PacketRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    protected Logger getLogger() {
        return logger;
    }

    protected Composer getComposer() {
        return composer;
    }
}
