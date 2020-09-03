package ru.xlv.packetapi.server;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import ru.xlv.packetapi.common.PacketHandler;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.forge.IPacketCallbackOnServer;
import ru.xlv.packetapi.server.packet.forge.IPacketInOnServer;
import ru.xlv.packetapi.server.packet.forge.IPacketOutServer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class PacketHandlerServer extends PacketHandler implements IPacketHandlerServer<EntityPlayer, IPacketOutServer> {

    private final Map<Class<? extends IPacket>, PacketData> packetMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public PacketHandlerServer(PacketRegistry packetRegistry, String channelName) {
        super(packetRegistry, channelName);
        for (Class<? extends IPacket> aClass : packetRegistry.getClassRegistry().keySet()) {
            PacketData packetData = new PacketData();
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
    }

    @Override
    protected void onServerPacketReceived(EntityPlayerMP entityPlayer, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = getPacketById(pid);
        if (packet != null) {
            PacketData packetData = packetMap.get(packet.getClass());
            Runnable runnable = () -> handlePacket(packet, packetData, pid, entityPlayer, bbis);
            if(packetData != null && packetData.isAsync) {
                executorService.submit(runnable);
            } else {
                FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(runnable);
            }
        }
    }

    private void handlePacket(IPacket packet, PacketData packetData, int pid, EntityPlayerMP entityPlayer, ByteBufInputStream byteBufInputStream) {
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(entityPlayer.getUniqueID())) {
                return;
            }
        }
        try {
            if (packet instanceof IPacketCallback) {
                processPacketCallbackOnServer((IPacketCallback) packet, pid, packetData != null && packetData.callWriteAnyway, entityPlayer, byteBufInputStream);
            } else if (packet instanceof IPacketInOnServer) {
                ((IPacketInOnServer) packet).read(entityPlayer, byteBufInputStream);
            } else if (packet instanceof IPacketIn) {
                ((IPacketIn) packet).read(byteBufInputStream);
            }
        } catch (Exception e) {
            logger.warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + entityPlayer.getName());
            e.printStackTrace();
        }
    }

    private void processPacketCallbackOnServer(IPacketCallback packet, int pid, boolean callWriteOnly, EntityPlayerMP entityPlayer, ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        if(!callWriteOnly) {
            if (packet instanceof IPacketCallbackOnServer) {
                ((IPacketCallbackOnServer) packet).read(entityPlayer, bbis, () -> {
                    if (((IPacketCallbackOnServer) packet).handleCallback()) {
                        Runnable runnable = () -> {
                            try {
                                sendPacketCallbackToClient(packet, pid, callbackId, entityPlayer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                        if(Thread.currentThread() == FMLCommonHandler.instance().getMinecraftServerInstance().getServerThread()) {
                            runnable.run();
                        } else {
                            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(runnable);
                        }
                    }
                });
            } else {
                packet.read(bbis);
            }
        }
        if(!(packet instanceof IPacketCallbackOnServer) || !((IPacketCallbackOnServer) packet).handleCallback()) {
            sendPacketCallbackToClient(packet, pid, callbackId, entityPlayer);
        }
    }

    private void sendPacketCallbackToClient(IPacketCallback packet, int pid, int callbackId, EntityPlayerMP entityPlayer) throws IOException {
        ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
        bbos.writeInt(pid);
        bbos.writeInt(callbackId);
        if(packet instanceof IPacketCallbackOnServer) {
            ((IPacketCallbackOnServer) packet).write(entityPlayer, bbos);
        } else {
            packet.write(bbos);
        }
        sendPacketToPlayer(entityPlayer, bbos);
    }

    @Override
    public void sendPacketToPlayer(EntityPlayer entityPlayer, IPacketOutServer packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(packet));
            packet.write(entityPlayer, byteBufOutputStream);
            sendPacketToPlayer(entityPlayer, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет пакет всем на сервере.
     * */
    @Override
    public void sendPacketToAll(@Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayerMP, packetOut));
    }

    /**
     * Отправляет пакет всем на сервере, исключая игрока.
     * */
    @Override
    public void sendPacketToAllExcept(@Nonnull EntityPlayer entityPlayer, @Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .filter(entityPlayerMP -> entityPlayerMP != entityPlayer)
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayer, packetOut));
    }

    /**
     * Отправляет пакет всем вокруг точки в радиусе.
     * */
    @Override
    public void sendPacketToAllAround(double x, double y, double z, double radius, @Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .filter(entityPlayerMP -> entityPlayerMP.getDistance(x, y, z) < radius)
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayerMP, packetOut));
    }

    /**
     * Отправляет пакет всем вокруг игрока в радиусе.
     * */
    @Override
    public void sendPacketToAllAround(@Nonnull EntityPlayer entity, double radius, @Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .filter(entityPlayerMP -> entityPlayerMP.getDistance(entity) < radius)
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayerMP, packetOut));
    }

    /**
     * Отправляет пакет всем вокруг существа в радиусе.
     * */
    public void sendPacketToAllAround(@Nonnull Entity entity, double radius, @Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .filter(entityPlayerMP -> entityPlayerMP.getDistance(entity) < radius)
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayerMP, packetOut));
    }

    /**
     * Отправляет пакет всем вокруг игрока в радиусе, исключая игрока.
     * */
    @Override
    public void sendPacketToAllAroundExcept(@Nonnull EntityPlayer entityPlayer, double radius, @Nonnull IPacketOutServer packetOut) {
        getOnlinePlayers()
                .filter(entityPlayerMP -> entityPlayer != entityPlayerMP)
                .filter(entityPlayerMP -> entityPlayerMP.getDistance(entityPlayer) < radius)
                .forEach(entityPlayerMP -> sendPacketToPlayer(entityPlayerMP, packetOut));
    }

    @Override
    public Stream<EntityPlayerMP> getOnlinePlayers() {
        return Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getOnlinePlayerNames())
                .map(s -> FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(s));
    }

    private static class PacketData {
        private RequestController<UUID> requestController;
        private boolean isAsync;
        private long requestPeriod;
        private int requestLimit;
        private boolean callWriteAnyway;
    }
}
