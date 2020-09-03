package ru.xlv.packetapi.server;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PacketHandlerBukkitServer implements IPacketHandlerServer<Player, IPacketOutBukkit> {

    private final Map<Class<? extends IPacket>, PacketHandlerBukkitServer.PacketData> packetMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final Logger logger;
    private final JavaPlugin javaPlugin;
    private final PacketRegistry packetRegistry;
    private final String channelName;

    public PacketHandlerBukkitServer(JavaPlugin javaPlugin, PacketRegistry packetRegistry, String channelName) {
        this.javaPlugin = javaPlugin;
        this.channelName = channelName;
        this.packetRegistry = packetRegistry;
        this.logger = Logger.getLogger(this.getClass().getSimpleName() + ":" + channelName);
        for (Class<? extends IPacket> aClass : packetRegistry.getClassRegistry().keySet()) {
            PacketHandlerBukkitServer.PacketData packetData = new PacketHandlerBukkitServer.PacketData();
            {
                AsyncPacket annotation = aClass.getAnnotation(AsyncPacket.class);
                if (annotation != null) {
                    packetData.isAsync = true;
                }
            }
            {
                ControllablePacket annotation = aClass.getAnnotation(ControllablePacket.class);
                if (annotation != null) {
                    packetData.callWriteAnyway = annotation.callWriteAnyway();
                    packetData.requestLimit = annotation.limit();
                    packetData.requestPeriod = annotation.period();
                }
            }
            if(packetData.requestPeriod != -1) {
                packetData.requestController = new RequestController.Periodic<>(packetData.requestPeriod);
            } else if(packetData.requestLimit != -1) {
                packetData.requestController = new RequestController.Limited<>(packetData.requestLimit);
            }
            packetMap.put(aClass, packetData);
        }
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
        IPacket packet = getPacketById(pid);
        if (packet != null) {
            PacketHandlerBukkitServer.PacketData packetData = packetMap.get(packet.getClass());
            if(packetData != null && packetData.isAsync) {
                executorService.submit(() -> handlePacket(packet, packetData, pid, player, bbis));
            } else {
                handlePacket(packet, packetData, pid, player, bbis);
            }
        }
    }

    private void handlePacket(IPacket packet, PacketData packetData, int pid, Player player, ByteBufInputStream byteBufInputStream) {
        if(packetData != null) {
            RequestController<String> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(player.getName())) {
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
    public void sendPacketToPlayer(Player player, IPacketOutBukkit packet) {
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

    public Map<Class<? extends IPacket>, PacketData> getPacketMap() {
        return this.packetMap;
    }

    public PacketRegistry getPacketRegistry() {
        return this.packetRegistry;
    }

    public String getChannelName() {
        return this.channelName;
    }

    private static class PacketData {
        private RequestController<String> requestController;
        private boolean isAsync;
        private long requestPeriod;
        private int requestLimit;
        private boolean callWriteAnyway;
    }
}
