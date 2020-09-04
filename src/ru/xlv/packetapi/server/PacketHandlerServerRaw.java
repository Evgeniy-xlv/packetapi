package ru.xlv.packetapi.server;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.capability.PacketAPI;
import ru.xlv.packetapi.common.PacketHandler;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.forge.IPacketCallbackOnServerRaw;
import ru.xlv.packetapi.server.packet.forge.IPacketInOnServerRaw;
import ru.xlv.packetapi.server.packet.forge.IPacketOutServerRaw;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class PacketHandlerServerRaw<PLAYER> extends PacketHandler<PLAYER> implements IPacketHandlerServer<PLAYER, IPacketOutServerRaw<PLAYER>> {

    private final Map<Class<? extends IPacket>, PacketData> packetMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressWarnings("UnstableApiUsage")
    private final Class<? super PLAYER> playerTypeClass = new TypeToken<PLAYER>(getClass()){}.getRawType();

    public PacketHandlerServerRaw(PacketRegistry packetRegistry, String channelName) {
        super(packetRegistry, channelName);
        scanAnnotations(packetRegistry, packetMap);
    }

    @Override
    protected void onServerPacketReceived(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = getPacketById(pid);
        if (packet != null) {
            PacketData packetData = packetMap.get(packet.getClass());
            Runnable runnable = () -> handlePacket(packet, packetData, pid, entityPlayer, bbis);
            if(packetData != null && packetData.isAsync) {
                executorService.submit(runnable);
            } else {
                if(PacketAPI.INSTANCE.getCapabilityAdapter().isServerThread(Thread.currentThread())) {
                    runnable.run();
                } else {
                    PacketAPI.INSTANCE.getCapabilityAdapter().scheduleServerTaskSync(runnable);
                }
            }
        }
    }

    private void handlePacket(IPacket packet, PacketData packetData, int pid, PLAYER entityPlayer, ByteBufInputStream byteBufInputStream) {
        boolean rejected = false;
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(PacketAPI.INSTANCE.getCapabilityAdapter().getPlayerEntityUniqueId(entityPlayer))) {
                rejected = true;
            }
        }
        try {
            if (packet instanceof IPacketCallback) {
                processPacketCallbackOnServer((IPacketCallback) packet, pid, rejected && packetData.callWriteAnyway, entityPlayer, byteBufInputStream);
            } else if (packet instanceof IPacketInOnServerRaw) {
                //noinspection unchecked
                ((IPacketInOnServerRaw<PLAYER>) packet).read(entityPlayer, byteBufInputStream);
            } else if (packet instanceof IPacketIn) {
                ((IPacketIn) packet).read(byteBufInputStream);
            }
        } catch (Exception e) {
            getLogger().warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + PacketAPI.INSTANCE.getCapabilityAdapter().getPlayerEntityName(entityPlayer));
            e.printStackTrace();
        }
    }

    private void processPacketCallbackOnServer(IPacketCallback packet, int pid, boolean callWriteOnly, PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        if(!callWriteOnly) {
            if (packet instanceof IPacketCallbackOnServerRaw) {
                //noinspection unchecked
                ((IPacketCallbackOnServerRaw<PLAYER>) packet).read(entityPlayer, bbis, () -> {
                    //noinspection unchecked
                    if (((IPacketCallbackOnServerRaw<PLAYER>) packet).handleCallback()) {
                        try {
                            sendPacketCallbackToClient(packet, pid, callbackId, entityPlayer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                packet.read(bbis);
            }
        }
        //noinspection unchecked
        if(!(packet instanceof IPacketCallbackOnServerRaw) || !((IPacketCallbackOnServerRaw<PLAYER>) packet).handleCallback()) {
            sendPacketCallbackToClient(packet, pid, callbackId, entityPlayer);
        }
    }

    private void sendPacketCallbackToClient(IPacketCallback packet, int pid, int callbackId, PLAYER entityPlayer) throws IOException {
        ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
        bbos.writeInt(pid);
        bbos.writeInt(callbackId);
        if(packet instanceof IPacketCallbackOnServerRaw) {
            //noinspection unchecked
            ((IPacketCallbackOnServerRaw<PLAYER>) packet).write(entityPlayer, bbos);
        } else {
            packet.write(bbos);
        }
        sendPacketToPlayer(entityPlayer, bbos);
    }

    @Override
    public void sendPacketToPlayer(PLAYER entityPlayer, IPacketOutServerRaw<PLAYER> packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(packet));
            packet.write(entityPlayer, byteBufOutputStream);
            sendPacketToPlayer(entityPlayer, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Stream<PLAYER> getOnlinePlayers() {
        return PacketAPI.INSTANCE.getCapabilityAdapter().getOnlinePlayersStream(playerTypeClass);
    }
}
