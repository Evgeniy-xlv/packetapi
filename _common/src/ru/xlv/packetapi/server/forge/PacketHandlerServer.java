package ru.xlv.packetapi.server.forge;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.PacketHandlerForge;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.IPacketHandlerServer;
import ru.xlv.packetapi.server.RequestController;
import ru.xlv.packetapi.server.forge.packet.ICallbackInServer;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The main server-side packet handler.
 * The handler deals with both receiving and sending packets. Using this handler, you can send both
 * simple packets and callbacks.
 * <p>
 * Supported packet types:
 * @see IPacketOutServer
 * @see IPacketInServer
 * @see ICallbackInServer
 * */
public class PacketHandlerServer extends PacketHandlerForge implements IPacketHandlerServer<EntityPlayer, IPacketOutServer> {

    private static PacketHandlerServer INSTANCE;

    private final Map<Class<? extends IPacket>, PacketData> packetDataMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    protected void onServerPacketReceived(String channelName, EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = createPacketById(channelName, pid);
        if (packet != null) {
            PacketData packetData = packetDataMap.get(packet.getClass());
            Runnable runnable = () -> handlePacket(channelName, packet, packetData, pid, entityPlayer, bbis);
            if(packetData != null && packetData.isAsync) {
                executorService.submit(runnable);
            } else {
                if(PacketAPI.getCapabilityAdapter().isServerThread(Thread.currentThread())) {
                    runnable.run();
                } else {
                    PacketAPI.getCapabilityAdapter().scheduleServerTaskSync(runnable);
                }
            }
        } else if(pid == -1) {
            if(PacketAPI.getCapabilityAdapter().isServerThread(Thread.currentThread())) {
                PacketAPI.getComposableCatcherBus().post(getComposer().decompose(bbis), entityPlayer);
            } else {
                PacketAPI.getCapabilityAdapter().scheduleServerTaskSync(() -> {
                    try {
                        PacketAPI.getComposableCatcherBus().post(getComposer().decompose(bbis), entityPlayer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void handlePacket(String channelName, IPacket packet, PacketData packetData, int pid, EntityPlayer entityPlayer, ByteBufInputStream byteBufInputStream) {
        boolean rejected = false;
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(PacketAPI.getCapabilityAdapter().getPlayerEntityUniqueId(entityPlayer))) {
                rejected = true;
            }
        }
        try {
            if (packet instanceof ICallbackInServer) {
                processPacketCallbackOnServer(channelName, (ICallbackInServer) packet, pid, rejected && packetData.callWriteAnyway, entityPlayer, byteBufInputStream);
            } else if (packet instanceof IPacketInServer) {
                ((IPacketInServer) packet).read(entityPlayer, byteBufInputStream);
            }
        } catch (Exception e) {
            getLogger().warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + PacketAPI.getCapabilityAdapter().getPlayerEntityName(entityPlayer));
            e.printStackTrace();
        }
    }

    private void processPacketCallbackOnServer(String channelName, ICallbackInServer packet, int pid, boolean callWriteOnly, EntityPlayer entityPlayer, ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        if(!callWriteOnly) {
            packet.read(entityPlayer, bbis, () -> {
                if (packet.handleCallback()) {
                    try {
                        sendPacketCallbackToClient(channelName, packet, pid, callbackId, entityPlayer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if(!packet.handleCallback()) {
            sendPacketCallbackToClient(channelName, packet, pid, callbackId, entityPlayer);
        }
    }

    private void sendPacketCallbackToClient(String channelName, ICallbackInServer packet, int pid, int callbackId, EntityPlayer entityPlayer) throws IOException {
        ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
        bbos.writeInt(pid);
        bbos.writeInt(callbackId);
        packet.write(entityPlayer, bbos);
        sendPacketToPlayer(channelName, entityPlayer, bbos);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    public <T extends IPacketOutServer> int[] registerPackets(String channelName, T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    public <T extends IPacketInServer> int[] registerPackets(String channelName, T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    public <T extends ICallbackInServer> int[] registerPackets(String channelName, T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    private <T extends IPacket> int[] registerPackets(Function<T, Integer> function, T... packets) {
        int[] ii = new int[packets.length];
        for (int i = 0; i < packets.length; i++) {
            ii[i] = function.apply(packets[i]);
        }
        return ii;
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T extends IPacketOutServer> int registerPacket(String channelName, T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T extends IPacketInServer> int registerPacket(String channelName, T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T, V extends ICallbackInServer> int registerPacket(String channelName, V packet) {
        return registerPacketSilently(channelName, packet);
    }

    private int registerPacketSilently(String channelName, IPacket packet) {
        try {
            return registerPacket(channelName, packet);
        } catch (PacketRegistrationException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int registerPacket(String channelName, IPacket packet) throws PacketRegistrationException {
        if(isSupportedPacket(packet)) {
            try {
                if (getPacketRegistry().getSubRegistryByChannel(channelName) == null) {
                    setupNetworkChannel(channelName);
                }
                int i = getPacketRegistry().register(channelName, packet);
                processServerAnnotations(packet, packetDataMap);
                return i;
            } catch (PacketRegistrationException e) {
                e.printStackTrace();
            }
            return -1;
        }
        throw new PacketRegistrationException("An input packet " + packet.getClass().getName() + " isn't a server packet.");
    }

    @Override
    public void setupNetworkChannel(String channelName) {
        createNetworkAdapter(channelName, null, (entityPlayer, byteBufInputStream) -> onServerPacketReceived(channelName, entityPlayer, byteBufInputStream));
    }

    @Override
    public <T extends IPacket> boolean isSupportedPacket(Class<T> aClass) {
        return IPacketOutServer.class.isAssignableFrom(aClass) || IPacketInServer.class.isAssignableFrom(aClass) || ICallbackInServer.class.isAssignableFrom(aClass);
    }

    /**
     * Sends a composable object to the client side.
     * @see Composable
     * */
    @Override
    public <T extends Composable> void sendComposable(@Nonnull EntityPlayer player, @Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            getComposer().compose(composable, byteBufOutputStream);
            sendPacketToPlayer(PacketAPI.DEFAULT_NET_CHANNEL_NAME, player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendPacketToPlayer(@Nonnull String channelName, @Nullable EntityPlayer player, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if (player == null) {
            getLogger().warning("Unable to send a packet to null player!");
            return;
        }
        getNetworkAdapter(channelName).sendTo(player, bbos);
        bbos.close();
    }

    @Override
    public void sendPacketToPlayer(@Nonnull String channelName, @Nonnull EntityPlayer player, @Nonnull IPacketOutServer packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(channelName, packet));
            packet.write(player, byteBufOutputStream);
            sendPacketToPlayer(channelName, player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToPlayerThr(@Nonnull String channelName, @Nonnull Object playerObject, @Nonnull IPacket packetOut) throws IOException {
        if (!(playerObject instanceof EntityPlayer)) {
            throw new IOException("The input player object is not a player. Object:" + playerObject);
        }
        if(!(packetOut instanceof IPacketOutServer)) {
            throw new IOException("The input packet is not an implementation of IPacketOutServer. Packet:" + packetOut);
        }
        sendPacketToPlayer(channelName, (EntityPlayer) playerObject, (IPacketOutServer) packetOut);
    }

    @Override
    public Stream<EntityPlayer> getOnlinePlayers() {
        return PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class);
    }

    public static PacketHandlerServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PacketHandlerServer();
            INSTANCE.setupNetworkChannel(PacketAPI.DEFAULT_NET_CHANNEL_NAME);
        }
        return INSTANCE;
    }
}
