package ru.xlv.packetapi.server;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.bukkit.IPacketCallbackOnBukkit;
import ru.xlv.packetapi.server.packet.bukkit.IPacketInOnBukkit;
import ru.xlv.packetapi.server.packet.bukkit.IPacketOutBukkit;

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
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PacketHandlerBukkitServer implements IPacketHandlerServer<Player, IPacketOutBukkit> {

    private final Map<Class<? extends IPacket>, PacketData> packetMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final Logger logger;
    private final JavaPlugin javaPlugin;
    private final AbstractPacketRegistry packetRegistry;
    private final String channelName;

    public PacketHandlerBukkitServer(@Nonnull JavaPlugin javaPlugin, @Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        this.javaPlugin = javaPlugin;
        this.channelName = channelName;
        this.packetRegistry = packetRegistry;
        this.logger = Logger.getLogger(this.getClass().getSimpleName() + ":" + channelName);
        processServerAnnotations(packetRegistry, packetMap);
        Bukkit.getMessenger().registerIncomingPluginChannel(javaPlugin, channelName, (channel, player, message) -> {
            try {
                onServerPacketReceived(player, new ByteBufInputStream(Unpooled.wrappedBuffer(message)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Bukkit.getMessenger().registerOutgoingPluginChannel(javaPlugin, channelName);
    }

    private void onServerPacketReceived(Player player, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = findPacketById(pid);
        if (packet != null) {
            PacketData packetData = packetMap.get(packet.getClass());
            if(packetData != null && packetData.isAsync) {
                executorService.submit(() -> handlePacket(packet, packetData, pid, player, bbis));
            } else {
                handlePacket(packet, packetData, pid, player, bbis);
            }
        } else if(pid == -1) {
            try {
                PacketAPI.getComposableCatcherBus().post(IPacket.COMPOSER.decompose(bbis), player);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePacket(IPacket packet, PacketData packetData, int pid, Player player, ByteBufInputStream byteBufInputStream) {
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(player.getUniqueId())) {
                return;
            }
        }
        try {
            if (packet instanceof IPacketCallback) {
                processPacketCallbackOnServer((IPacketCallback) packet, pid, packetData != null && packetData.callWriteAnyway, player, byteBufInputStream);
            } else if (packet instanceof IPacketInOnBukkit) {
                ((IPacketInOnBukkit) packet).read(player, byteBufInputStream);
            } else if (packet instanceof IPacketIn) {
                ((IPacketIn) packet).read(byteBufInputStream);
            }
        } catch (Exception e) {
            logger.warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + player.getName());
            e.printStackTrace();
        }
    }

    private void processPacketCallbackOnServer(IPacketCallback packet, int pid, boolean callWriteOnly, Player player, ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        if(!callWriteOnly) {
            if (packet instanceof IPacketCallbackOnBukkit) {
                ((IPacketCallbackOnBukkit) packet).read(player, bbis, () -> {
                    if (((IPacketCallbackOnBukkit) packet).handleCallback()) {
                        Runnable runnable = () -> {
                            try {
                                sendPacketCallbackToClient(packet, pid, callbackId, player);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                        Bukkit.getScheduler().runTask(javaPlugin, runnable);
                    }
                });
            } else {
                packet.read(bbis);
            }
        }
        if(!(packet instanceof IPacketCallbackOnBukkit) || !((IPacketCallbackOnBukkit) packet).handleCallback()) {
            sendPacketCallbackToClient(packet, pid, callbackId, player);
        }
    }

    private void sendPacketCallbackToClient(IPacketCallback packet, int pid, int callbackId, Player player) throws IOException {
        ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
        bbos.writeInt(pid);
        bbos.writeInt(callbackId);
        if(packet instanceof IPacketCallbackOnBukkit) {
            ((IPacketCallbackOnBukkit) packet).write(player, bbos);
        } else {
            packet.write(bbos);
        }
        sendPacketToPlayer(player, bbos);
    }

    /**
     * Sends the composable object to the client side.
     * @see Composable
     * */
    public <T extends Composable> void sendComposable(@Nonnull Player player, @Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            IPacket.COMPOSER.compose(composable, byteBufOutputStream);
            sendPacketToPlayer(player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToPlayer(@Nullable Player player, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if(player == null) {
            logger.warning("Unable to send a packet to null player!");
            return;
        }
        bbos.flush();
        player.sendPluginMessage(javaPlugin, getChannelName(), bbos.buffer().array());
        bbos.close();
    }

    @Override
    public void sendPacketToPlayer(@Nonnull Player player, @Nonnull IPacketOutBukkit packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(packet));
            packet.write(player, byteBufOutputStream);
            sendPacketToPlayer(player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPacketToAll(@Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .forEach(p -> sendPacketToPlayer(p, packetOut));
    }

    @Override
    public void sendPacketToAllExcept(@Nonnull Player player, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> p != player)
                .forEach(p -> sendPacketToPlayer(p, packetOut));
    }

    @Override
    public void sendPacketToAllAround(double x, double y, double z, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        Location loc = new Location(null, x, y, z);
        getOnlinePlayers()
                .filter(p -> p.getLocation().distance(loc) < radius)
                .forEach(p -> sendPacketToPlayer(p, packetOut));
    }

    @Override
    public void sendPacketToAllAround(@Nonnull Player player, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> p.getLocation().distance(player.getLocation()) < radius)
                .forEach(p -> sendPacketToPlayer(p, packetOut));
    }

    @Override
    public void sendPacketToAllAroundExcept(@Nonnull Player player, @Nonnegative double radius, @Nonnull IPacketOutBukkit packetOut) {
        getOnlinePlayers()
                .filter(p -> player != p)
                .filter(p -> p.getLocation().distance(player.getLocation()) < radius)
                .forEach(p -> sendPacketToPlayer(p, packetOut));
    }

    @Override
    public Stream<? extends Player> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream();
    }

    public AbstractPacketRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    public String getChannelName() {
        return this.channelName;
    }
}
