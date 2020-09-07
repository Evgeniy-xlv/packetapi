package ru.xlv.packetapi.server;

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.PacketHandler;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.forge.IPacketCallbackOnServerRaw;
import ru.xlv.packetapi.server.packet.forge.IPacketInOnServerRaw;
import ru.xlv.packetapi.server.packet.forge.IPacketOutServerRaw;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * The main server-side packet handler.
 * The handler deals with both receiving and sending packets. Using this handler, you can send both
 * simple packets(eg. {@link ru.xlv.packetapi.common.packet.IPacketOut}) and callbacks ({@link IPacketCallbackEffective}).
 * <p>
 * You should use an adapted version of this handler ({@link ru.xlv.packetapi.client.PacketHandlerServer}) for each version of the game.
 * */
public class PacketHandlerServerRaw<PLAYER> extends PacketHandler<PLAYER> implements IPacketHandlerServer<PLAYER, IPacketOutServerRaw<PLAYER>> {

    private final Map<Class<? extends IPacket>, PacketData> packetMap = new HashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressWarnings("UnstableApiUsage")
    private final Class<? super PLAYER> playerTypeClass = new TypeToken<PLAYER>(getClass()){}.getRawType();

    public PacketHandlerServerRaw() {
        this(new SimplePacketRegistry(), PacketAPI.getApiDefaultChannelName());
    }

    public PacketHandlerServerRaw(@Nonnull String channelName) {
        this(new SimplePacketRegistry(), channelName);
    }

    public PacketHandlerServerRaw(@Nonnull AbstractPacketRegistry packetRegistry) {
        this(packetRegistry, PacketAPI.getApiDefaultChannelName());
    }

    public PacketHandlerServerRaw(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        super(packetRegistry, channelName);
        processServerAnnotations(packetRegistry, packetMap);
    }

    @Override
    protected void onServerPacketReceived(PLAYER entityPlayer, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = findPacketById(pid);
        if (packet != null) {
            PacketData packetData = packetMap.get(packet.getClass());
            Runnable runnable = () -> handlePacket(packet, packetData, pid, entityPlayer, bbis);
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
            Runnable runnable = () -> {
                try {
                    PacketAPI.getComposableCatcherBus().post(getComposer().decompose(bbis), entityPlayer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            if(PacketAPI.getCapabilityAdapter().isServerThread(Thread.currentThread())) {
                runnable.run();
            } else {
                PacketAPI.getCapabilityAdapter().scheduleServerTaskSync(runnable);
            }
        }
    }

    private void handlePacket(IPacket packet, PacketData packetData, int pid, PLAYER entityPlayer, ByteBufInputStream byteBufInputStream) {
        boolean rejected = false;
        if(packetData != null) {
            RequestController<UUID> requestController = packetData.requestController;
            if (requestController != null && !requestController.tryRequest(PacketAPI.getCapabilityAdapter().getPlayerEntityUniqueId(entityPlayer))) {
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
            getLogger().warning("An error has occurred during executing a packet " + pid + "#" + packet + ". Sender: " + PacketAPI.getCapabilityAdapter().getPlayerEntityName(entityPlayer));
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

    /**
     * Sends the composable object to the client side.
     * @see Composable
     * */
    public <T extends Composable> void sendComposable(@Nonnull PLAYER player, @Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            getComposer().compose(composable, byteBufOutputStream);
            sendPacketToPlayer(player, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendPacketToPlayer(@Nullable PLAYER player, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if (player == null) {
            getLogger().warning("Unable to send a packet to null player!");
            return;
        }
        getNetworkAdapter().sendTo(player, bbos);
        bbos.close();
    }

    @Override
    public void sendPacketToPlayer(@Nonnull PLAYER player, @Nonnull IPacketOutServerRaw<PLAYER> packet) {
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
    public Stream<PLAYER> getOnlinePlayers() {
        return PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(playerTypeClass);
    }
}
