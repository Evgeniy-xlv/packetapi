package ru.xlv.packetapi.common;

import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.capability.AbstractNetworkAdapter;
import ru.xlv.packetapi.common.packet.registration.PacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class PacketHandlerForge implements IPacketHandler {

    private static final Map<String, AbstractNetworkAdapter<EntityPlayer>> NETWORK_ADAPTER_REGISTRY = new HashMap<>();

    private final PacketRegistry packetRegistry = new PacketRegistry();

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected void createNetworkAdapter(String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<EntityPlayer, ByteBufInputStream> serverPacketReceived) {
        getLogger().info("Trying to register a new network channel " + channelName + "...");
        AbstractNetworkAdapter<EntityPlayer> networkAdapter;
        if(NETWORK_ADAPTER_REGISTRY.containsKey(channelName)) {
            networkAdapter = NETWORK_ADAPTER_REGISTRY.get(channelName);
        } else {
            networkAdapter = PacketAPI.getCapabilityAdapter().newNetworkAdapter(EntityPlayer.class, channelName);
            NETWORK_ADAPTER_REGISTRY.put(channelName, networkAdapter);
            getLogger().info("A new network channel " + channelName + " successfully registered.");
        }
        if (clientPacketReceived != null) {
            networkAdapter.addClientPacketReceiveListener(clientPacketReceived);
        }
        if (serverPacketReceived != null) {
            networkAdapter.addServerPacketReceiveListener(serverPacketReceived);
        }
    }

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
}
