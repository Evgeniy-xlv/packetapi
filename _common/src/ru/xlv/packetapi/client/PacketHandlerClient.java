package ru.xlv.packetapi.client;

import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.client.packet.IPacketInClient;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.PacketHandlerForge;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * The main client-side packet handler.
 * The handler deals with both receiving and sending packets. Using this handler, you can send both
 * simple packets and callbacks.
 * <p>
 * Supported packet types:
 * @see IPacketOutClient
 * @see IPacketInClient
 * @see ICallbackOut
 * */
public class PacketHandlerClient extends PacketHandlerForge {

    private static PacketHandlerClient INSTANCE;

    private final long callbackDefaultCheckResultPeriod;
    private final long callbackResultWaitTimeout;

    private final TIntObjectHashMap<ICallbackOut<?>> callbackMap = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<Object> callbackResultMap = new TIntObjectHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private PacketHandlerClient(long callbackResultWaitTimeout, long callbackDefaultCheckResultPeriod) {
        this.callbackDefaultCheckResultPeriod = callbackDefaultCheckResultPeriod;
        this.callbackResultWaitTimeout = callbackResultWaitTimeout;
    }

    protected void onClientPacketReceived(String channelName, ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = createPacketById(channelName, pid);
        if (packet != null || pid == -1) {
            PacketAPI.getCapabilityAdapter().scheduleTaskSync(() -> {
                try {
                    if (pid == -1) {
                        PacketAPI.getComposableCatcherBus().post(Composable.decompose(bbis));
                        return;
                    }
                    if (packet instanceof ICallbackOut) {
                        processPacketCallbackOnClient(bbis);
                    } else if (packet instanceof IPacketInClient) {
                        ((IPacketInClient) packet).read(bbis);
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
        ICallbackOut<?> packetCallback;
        synchronized (callbackMap) {
            packetCallback = callbackMap.remove(callbackId);
        }
        if (packetCallback != null) {
            try {
                packetCallback.read(bbis);
                synchronized (callbackResultMap) {
                    // noinspection rawtypes
                    callbackResultMap.put(callbackId, ((ICallbackOut) packetCallback).getResult());
                }
            } catch (IOException e) {
                synchronized (callbackResultMap) {
                    callbackResultMap.put(callbackId, e);
                }
                throw e;
            }
        }
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends IPacketOutClient> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends IPacketInClient> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
        return registerPackets(packet -> registerPacket(channelName, packet), packets);
    }

    /**
     * Registers packets for the specified channel name.
     * Their id will be automatically generated.
     * */
    @SafeVarargs
    public final <T extends ICallbackOut<T>> int[] registerPackets(@Nonnull String channelName, @Nonnull T... packets) {
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
    public <T extends IPacketOutClient> int registerPacket(@Nonnull String channelName, @Nonnull T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T extends IPacketInClient> int registerPacket(@Nonnull String channelName, @Nonnull T packet) {
        return registerPacketSilently(channelName, packet);
    }

    /**
     * Registers a packet for the specified channel name.
     * His id will be automatically generated.
     * */
    public <T, V extends ICallbackOut<T>> int registerPacket(@Nonnull String channelName, @Nonnull V packet) {
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
    public void setupNetworkChannel(String channelName) {
        createNetworkAdapter(channelName, byteBufInputStream -> onClientPacketReceived(channelName, byteBufInputStream), null);
    }

    /**
     * Sends a composable object to the server side.
     * @see Composable
     * */
    public <T extends Composable> void sendComposable(@Nonnull T composable) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(-1);
            Composable.compose(composable, byteBufOutputStream);
            sendPacketToServer(PacketAPI.DEFAULT_NET_CHANNEL_NAME, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacketToServer(@Nonnull String channelName, @Nonnull IPacketOutClient packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(channelName, packet));
            packet.write(byteBufOutputStream);
            sendPacketToServer(channelName, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendPacketToServer(@Nonnull String channelName, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        getNetworkAdapter(channelName).sendToServer(bbos);
        bbos.close();
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return callbackId
     * */
    public <T> int sendPacketCallback(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet) {
        int id = genCallbackId();
        synchronized (callbackMap) {
            callbackMap.put(id, packet);
        }
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        try {
            byteBufOutputStream.writeInt(getPacketId(channelName, packet));
            byteBufOutputStream.writeInt(id);
            packet.write(byteBufOutputStream);
            sendPacketToServer(channelName, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * The returned {@link CallbackResponseHandler} contains a not null result.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet) {
        return new CallbackResponseHandler<>(sendCallbackAsync(channelName, packet));
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkNonNullResult lets you check if the result is null
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet, boolean checkNonNullResult) {
        return new CallbackResponseHandler<>(sendCallbackAsync(channelName, packet), checkNonNullResult);
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * The returned {@link CallbackResponseHandler} contains a not null result.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet, long resultWaitTimeout, long checkResultPeriod) {
        return new CallbackResponseHandler<>(sendCallbackAsync(channelName, packet, resultWaitTimeout, checkResultPeriod));
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkNonNullResult lets you check if the result is null
     * @return {@link CallbackResponseHandler}, which lets the response to be processed synchronously on the main thread.
     * */
    public <T> CallbackResponseHandler<T> sendCallback(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet, long resultWaitTimeout, long checkResultPeriod, boolean checkNonNullResult) {
        return new CallbackResponseHandler<>(sendCallbackAsync(channelName, packet, resultWaitTimeout, checkResultPeriod), checkNonNullResult);
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CompletableFuture}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CompletableFuture<CallbackResponseResult<T>> sendCallbackAsync(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet) {
        return sendCallbackAsync(channelName, packet, callbackResultWaitTimeout, callbackDefaultCheckResultPeriod);
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkResultPeriod is the period in millis for checking the prepared response.
     *
     * @return {@link CompletableFuture}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CompletableFuture<CallbackResponseResult<T>> sendCallbackAsync(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet, long resultWaitTimeout, long checkResultPeriod) {
        final int id = sendPacketCallback(channelName, packet);
        return CompletableFuture.supplyAsync(() -> waitForCallbackResponse(id, resultWaitTimeout, checkResultPeriod), executorService);
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @return {@link CallbackResponseResult}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CallbackResponseResult<T> sendCallbackSync(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet) {
        return sendCallbackSync(channelName, packet, callbackResultWaitTimeout, callbackDefaultCheckResultPeriod);
    }

    /**
     * Sends a packet, that calls the {@link ICallbackOut#read(ByteBufInputStream)} method,
     * when(if) the server sends a response packet with the same packetId and callbackId back to the client.
     *
     * @param checkResultPeriod is the period in millis for checking the prepared response.
     *
     * @return {@link CallbackResponseResult}, which will be contain the response from the server, or null if no response
     * was received or was improperly constructed.
     * */
    public <T> CallbackResponseResult<T> sendCallbackSync(@Nonnull String channelName, @Nonnull ICallbackOut<T> packet, long resultWaitTimeout, long checkResultPeriod) {
        final int id = sendPacketCallback(channelName, packet);
        return waitForCallbackResponse(id, resultWaitTimeout, checkResultPeriod);
    }

    private <T> CallbackResponseResult<T> waitForCallbackResponse(int id, long resultWaitTimeout, long checkResultPeriod) {
        long l = System.currentTimeMillis() + resultWaitTimeout;
        while (true) {
            synchronized (callbackResultMap) {
                if (callbackResultMap.containsKey(id)) {
                    Object remove = callbackResultMap.remove(id);
                    if(remove instanceof Exception) {
                        return new CallbackResponseResult<>(null, CallbackResponseResult.State.ERROR, remove);
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
    }

    private int genCallbackId() {
        int i = 1;
        while(callbackMap.containsKey(i)) i++;
        return i;
    }

    @Override
    public <T extends IPacket> boolean isSupportedPacket(Class<T> aClass) {
        return IPacketOutClient.class.isAssignableFrom(aClass) || IPacketInClient.class.isAssignableFrom(aClass) || ICallbackOut.class.isAssignableFrom(aClass);
    }

    public static PacketHandlerClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PacketHandlerClient(2000L, 0L);
            INSTANCE.setupNetworkChannel(PacketAPI.DEFAULT_NET_CHANNEL_NAME);
        }
        return INSTANCE;
    }
}
