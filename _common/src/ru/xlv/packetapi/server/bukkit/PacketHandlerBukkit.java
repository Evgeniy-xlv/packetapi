package ru.xlv.packetapi.server.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.common.packet.registration.PacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.IPacketHandlerServer;
import ru.xlv.packetapi.server.RequestController;
import ru.xlv.packetapi.server.bukkit.packet.ICallbackInBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketInBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;

import javax.annotation.Nonnegative;
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
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The main server-side packet handler for bukkit.
 * The handler deals with both receiving and sending packets. Using this handler, you can send both
 * simple packets and callbacks.
 * <p>
 * Supported packet types:
 * @see IPacketOutBukkit
 * @see IPacketInBukkit
 * @see ICallbackInBukkit
 * */
public class PacketHandlerBukkit implements IPacketHandlerServer<Player, IPacketOutBukkit> {

    private final Map<Class<? extends IPacket>, PacketData> packetDataMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(PacketAPI.getAsyncPacketThreadPoolSize());

    private static PacketHandlerBukkit instance;

    private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private final PacketRegistry packetRegistry = new PacketRegistry();
    private final JavaPlugin javaPlugin;

    private PacketHandlerBukkit(@Nonnull JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
    }

    private void onServerPacketReceived(String channelName, Player player, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = createPacketById(channelName, pid);
        if (packet != null) {
            PacketData packetData = packetDataMap.get(packet.getClass());
            if(packetData != null && packetData.isAsync) {
                executorService.submit(() -> handlePacket(channelName, packet, packetData, pid, player, bbis));
            } else {
                handlePacket(channelName, packet, packetData, pid, player, bbis);
            }
        } else if(pid == -1) {
            try {
                PacketAPI.getComposableCatcherBus().post(Composable.decompose(bbis), player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePacket(String channelName, IPacket packet, PacketData packetData, int pid, Player player, ByteBufInputStream byteBufInputStream) {
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(player.getUniqueId())) {
                return;
            }
        }
        try {
            if (packet instanceof ICallbackInBukkit) {
                processPacketCallbackOnServer(channelName, (ICallbackInBukkit) packet, pid, packetData != null && packetData.callWriteAnyway, player, byteBufInputStream);
            } else if (packet instanceof IPacketInBukkit) {
                ((IPacketInBukkit) packet).read(player, byteBufInputStream);
            }
        } catch (Exception e) {
            logger.warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + player.getName());
            e.printStackTrace();
        }
    }

    private void processPacketCallbackOnServer(String channelName, ICallbackInBukkit packet, int pid, boolean callWriteOnly, Player player, ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        if(!callWriteOnly) {
            packet.read(player, bbis, () -> {
                if (packet.handleCallback()) {
                    Runnable runnable = () -> {
                        try {
                            sendPacketCallbackToClient(channelName, packet, pid, callbackId, player);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    Bukkit.getScheduler().runTask(javaPlugin, runnable);
                }
            });
        }
        if(!packet.handleCallback()) {
            sendPacketCallbackToClient(channelName, packet, pid, callbackId, player);
        }
    }

    private void sendPacketCallbackToClient(String channelName, ICallbackInBukkit packet, int pid, int callbackId, Player player) throws IOException {
        ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
        bbos.writeInt(pid);
        bbos.writeInt(callbackId);
        packet.write(player, bbos);
        sendPacketToPlayer(channelName, player, bbos);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends IPacketOutBukkit> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends IPacketInBukkit> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends ICallbackInBukkit> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    @SafeVarargs
    private final <T extends IPacket> int[] registerPackets(Function<T, Integer> function, T... packets) {
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
    public <T extends IPacketOutBukkit> int registerPacket(@Nonnull String channelName, @Nonnull T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T extends IPacketInBukkit> int registerPacket(@Nonnull String channelName, @Nonnull T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T, V extends ICallbackInBukkit> int registerPacket(@Nonnull String channelName, @Nonnull V packet) {
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
    public <T extends IPacket> boolean isSupportedPacket(Class<T> aClass) {
        return IPacketOutBukkit.class.isAssignableFrom(aClass) || IPacketInBukkit.class.isAssignableFrom(aClass) || ICallbackInBukkit.class.isAssignableFrom(aClass);
    }

    @Override
    public void setupNetworkChannel(String channelName) {
        Bukkit.getMessenger().registerIncomingPluginChannel(javaPlugin, channelName, (channel, player, message) -> {
            try {
                onServerPacketReceived(channelName, player, new ByteBufInputStream(Unpooled.wrappedBuffer(message)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Bukkit.getMessenger().registerOutgoingPluginChannel(javaPlugin, channelName);
    }

    /**
     * Sends a composable object to the client side.
     * @see Composable
     * */
    @Override
    public <T extends Composable> void sendComposable(@Nonnull Player player, @Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            Composable.compose(composable, byteBufOutputStream);
            sendPacketToPlayer(PacketAPI.DEFAULT_NET_CHANNEL_NAME, player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToPlayer(@Nonnull String channelName, @Nullable Player player, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if(player == null) {
            logger.warning("Unable to send a packet to null player!");
            return;
        }
        bbos.flush();
        player.sendPluginMessage(javaPlugin, channelName, bbos.buffer().array());
        bbos.close();
    }

    @Override
    public void sendPacketToPlayer(@Nonnull String channelName, @Nonnull Player player, @Nonnull IPacketOutBukkit packet) {
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
        if (!(playerObject instanceof Player)) {
            throw new IOException("The input player object is not a player. Object:" + playerObject);
        }
        if(!(packetOut instanceof IPacketOutBukkit)) {
            throw new IOException("The input packet is not an implementation of IPacketOutBukkit. Packet:" + packetOut);
        }
        sendPacketToPlayer(channelName, (Player) playerObject, (IPacketOutBukkit) packetOut);
    }

    @Override
    public void sendPacketToAll(@Nonnull String channelName, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    @Override
    public void sendPacketToAllExcept(@Nonnull String channelName, @Nonnull Player player, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> p != player)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    @Override
    public void sendPacketToAllAround(@Nonnull String channelName, double x, double y, double z, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        Location loc = new Location(null, x, y, z);
        getOnlinePlayers()
                .filter(p -> p.getLocation().distance(loc) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    @Override
    public void sendPacketToAllAround(@Nonnull String channelName, @Nonnull Player player, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> p.getLocation().distance(player.getLocation()) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    @Override
    public void sendPacketToAllAroundExcept(@Nonnull String channelName, @Nonnull Player player, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> player != p)
                .filter(p -> p.getLocation().distance(player.getLocation()) < radius)
                .forEach(p -> sendPacketToPlayer(channelName, p, packetOut));
    }

    @Override
    public Stream<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream();
    }

    @Override
    public PacketRegistry getPacketRegistry() {
        return packetRegistry;
    }

    public static PacketHandlerBukkit getInstance() {
        if (instance == null) {
            instance = PacketAPIBukkitPlugin.getPacketHandler();
            instance.setupNetworkChannel(PacketAPI.DEFAULT_NET_CHANNEL_NAME);
            if(instance == null) {
                throw new RuntimeException("Unexpected error! The instance of PacketHandlerBukkitServer is null.");
            }
        }
        return instance;
    }
}
