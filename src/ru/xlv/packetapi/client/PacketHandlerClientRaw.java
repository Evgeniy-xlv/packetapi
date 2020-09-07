package ru.xlv.packetapi.client;

import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.PacketHandler;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.packet.IPacketOut;
import ru.xlv.packetapi.common.registry.AbstractPacketRegistry;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The main client-side packet handler.
 * The handler deals with both receiving and sending packets. Using this handler, you can send both
 * simple packets(eg. {@link ru.xlv.packetapi.common.packet.IPacketOut}) and callbacks ({@link IPacketCallbackEffective}).
 * <p>
 * You should use an adapted version of this handler ({@link ru.xlv.packetapi.client.PacketHandlerClient}) for each version of the game.
 * */
public class PacketHandlerClientRaw<PLAYER> extends PacketHandler<PLAYER> {

    private final long defaultCheckResultPeriod;
    private final long callbackResultWaitTimeout;

    private final TIntObjectHashMap<IPacketCallback> callbackMap = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<Object> callbackResultMap = new TIntObjectHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public PacketHandlerClientRaw() {
        this(new SimplePacketRegistry(), PacketAPI.getApiDefaultChannelName());
    }

    public PacketHandlerClientRaw(@Nonnull String channelName) {
        this(new SimplePacketRegistry(), channelName);
    }

    public PacketHandlerClientRaw(@Nonnull AbstractPacketRegistry packetRegistry) {
        this(packetRegistry, PacketAPI.getApiDefaultChannelName());
    }

    public PacketHandlerClientRaw(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName) {
        this(packetRegistry, channelName, 2000L, 0L);
    }

    public PacketHandlerClientRaw(@Nonnull AbstractPacketRegistry packetRegistry, @Nonnull String channelName, long callbackResultWaitTimeout, long defaultCheckResultPeriod) {
        super(packetRegistry, channelName);
        this.defaultCheckResultPeriod = defaultCheckResultPeriod;
        this.callbackResultWaitTimeout = callbackResultWaitTimeout;
    }

    @Override
    protected void onClientPacketReceived(ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = findPacketById(pid);
        if (packet != null || pid == -1) {
            PacketAPI.getCapabilityAdapter().scheduleTaskSync(() -> {
                try {
                    if (pid == -1) {
                        PacketAPI.getComposableCatcherBus().post(getComposer().decompose(bbis));
                        return;
                    }
                    if (packet instanceof IPacketCallback) {
                        processPacketCallbackOnClient(bbis);
                    } else if (packet instanceof IPacketIn) {
                        ((IPacketIn) packet).read(bbis);
                    }
                } catch (Exception e) {
                    getLogger().warning("An error has occurred during executing a packet " + pid + "#" + packet);
                    e.printStackTrace();
                }
            });
        }
    }

    private void processPacketCallbackOnClient(ByteBufInputStream bbis) throws IOException {
        int callbackId = bbis.readInt();
        IPacketCallback packetCallback;
        synchronized (callbackMap) {
            packetCallback = callbackMap.remove(callbackId);
        }
        if (packetCallback != null) {
            try {
                packetCallback.read(bbis);
                if (packetCallback instanceof IPacketCallbackEffective) {
                    synchronized (callbackResultMap) {
                        // noinspection rawtypes
                        callbackResultMap.put(callbackId, ((IPacketCallbackEffective) packetCallback).getResult());
                    }
                }
            } catch (IOException e) {
                if (packetCallback instanceof IPacketCallbackEffective) {
                    synchronized (callbackResultMap) {
                        callbackResultMap.put(callbackId, e);
                    }
                }
                throw e;
            }
        }
    }

    /**
     * Sends the composable object to the server side.
     * @see Composable
     * */
    public <T extends Composable> void sendComposable(@Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            getComposer().compose(composable, byteBufOutputStream);
            sendPacketToServer(byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    protected void sendPacketToServer(@Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        getNetworkAdapter().sendToServer(bbos);
        bbos.close();
    }

    /**
     * Sends a packet, that calls the {@link IPacketCallback#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return callbackId
     * */
    public int sendPacketCallback(@Nonnull IPacketCallback packet) {
        int id = genCallbackId();
        synchronized (callbackMap) {
            callbackMap.put(id, packet);
        }
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(packet));
            byteBufOutputStream.writeInt(id);
            packet.write(byteBufOutputStream);
            sendPacketToServer(byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * @deprecated it will be removed in the next versions. Use {@link PacketHandlerClientRaw#sendCallback(IPacketCallbackEffective)}.
     * */
    @Deprecated
    public <T> CallbackResponseHandler<T> sendPacketEffectiveCallback(@Nonnull IPacketCallbackEffective<T> packet) {
        return sendCallback(packet);
    }

    /**
     * @deprecated it will be removed in the next versions. Use {@link PacketHandlerClientRaw#sendCallback(IPacketCallbackEffective, boolean)}.
     * */
    @Deprecated
    public <T> CallbackResponseHandler<T> sendPacketEffectiveCallback(@Nonnull IPacketCallbackEffective<T> packet, boolean checkNonNullResult) {
        return sendCallback(packet, checkNonNullResult);
    }

    /**
     * @deprecated it will be removed in the next versions. Use {@link PacketHandlerClientRaw#sendCallbackAsync(IPacketCallbackEffective)}.
     * */
    @Deprecated
    public <T> CompletableFuture<CallbackResponseResult<T>> sendPacketCallbackAsync(@Nonnull IPacketCallbackEffective<T> packet) {
        return sendCallbackAsync(packet);
    }

    /**
     * @deprecated it will be removed in the next versions. Use {@link PacketHandlerClientRaw#sendCallbackAsync(IPacketCallbackEffective)}.
     * */
    @Deprecated
    public <T> CompletableFuture<CallbackResponseResult<T>> sendPacketCallbackAsync(@Nonnull IPacketCallbackEffective<T> packet, long checkResultPeriod) {
        return sendCallbackAsync(packet, checkResultPeriod);
    }

    /**
     * Sends a packet, that calls the {@link IPacketCallbackEffective#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * The returned {@link CallbackResponseHandler} contains a not null result.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull IPacketCallbackEffective<T> packet) {
        return new CallbackResponseHandler<>(sendCallbackAsync(packet));
    }

    /**
     * Sends a packet, that calls the {@link IPacketCallbackEffective#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkNonNullResult lets you check if the result is null
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull IPacketCallbackEffective<T> packet, boolean checkNonNullResult) {
        return new CallbackResponseHandler<>(sendCallbackAsync(packet), checkNonNullResult);
    }

    /**
     * Sends a packet, that calls the {@link IPacketCallbackEffective#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CompletableFuture}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CompletableFuture<CallbackResponseResult<T>> sendCallbackAsync(@Nonnull IPacketCallbackEffective<T> packet) {
        return sendCallbackAsync(packet, defaultCheckResultPeriod);
    }

    /**
     * Sends a packet, that calls the {@link IPacketCallbackEffective#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkResultPeriod is the period in millis for checking the prepared response.
     *
     * @return {@link CompletableFuture}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CompletableFuture<CallbackResponseResult<T>> sendCallbackAsync(@Nonnull IPacketCallbackEffective<T> packet, long checkResultPeriod) {
        final int id = sendPacketCallback(packet);
        return CompletableFuture.supplyAsync(() -> {
            long l = System.currentTimeMillis() + callbackResultWaitTimeout;
            while (true) {
                synchronized (callbackResultMap) {
                    if (callbackResultMap.containsKey(id)) {
                        Object remove = callbackResultMap.remove(id);
                        if(remove instanceof Exception) {
                            return new CallbackResponseResult<>(null, CallbackResponseResult.State.EXCEPTION);
                        } else {
                            //noinspection unchecked
                            return new CallbackResponseResult<>((T) remove, CallbackResponseResult.State.CONSTRUCTED);
                        }
                    }
                }
                if (System.currentTimeMillis() >= l) {
                    break;
                }
                if(checkResultPeriod <= 0) continue;
                try {
                    TimeUnit.MILLISECONDS.sleep(checkResultPeriod);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return new CallbackResponseResult<>(null, CallbackResponseResult.State.TIME_OUT);
        }, executorService);
    }

    private int genCallbackId() {
        int i = 1;
        while(callbackMap.containsKey(i)) i++;
        return i;
    }
}
