package ru.xlv.packetapi.client;

import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.packetapi.capability.PacketAPI;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.PacketHandler;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.packet.IPacketIn;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PacketHandlerClientRaw<PLAYER> extends PacketHandler<PLAYER> {

    private final long defaultCheckResultPeriod;
    private final long callbackResultWaitTimeout;

    private final TIntObjectHashMap<IPacketCallback> callbackMap = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<Object> callbackResultMap = new TIntObjectHashMap<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public PacketHandlerClientRaw(PacketRegistry packetRegistry, String channelName) {
        this(packetRegistry, channelName, 2000L, 0L);
    }

    public PacketHandlerClientRaw(PacketRegistry packetRegistry, String channelName, long callbackResultWaitTimeout, long defaultCheckResultPeriod) {
        super(packetRegistry, channelName);
        this.defaultCheckResultPeriod = defaultCheckResultPeriod;
        this.callbackResultWaitTimeout = callbackResultWaitTimeout;
    }

    @Override
    protected void onClientPacketReceived(ByteBufInputStream bbis) throws IOException {
        int pid = bbis.readInt();
        IPacket packet = getPacketById(pid);
        if (packet != null) {
            PacketAPI.INSTANCE.getCapabilityAdapter().scheduleTaskSync(() -> {
                try {
                    if (packet instanceof IPacketCallback) {
                        processPacketCallbackOnClient(bbis);
                    } else if (packet instanceof IPacketIn) {
                        ((IPacketIn) packet).read(bbis);
                    }
                } catch (Exception e) {
                    logger.warning("An error has occurred during executing a packet " + pid + "#" + packet);
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
            packetCallback.read(bbis);
            if(packetCallback instanceof IPacketCallbackEffective) {
                synchronized (callbackResultMap) {
                    //noinspection rawtypes
                    callbackResultMap.put(callbackId, ((IPacketCallbackEffective) packetCallback).getResult());
                }
            }
        }
    }

    /**
     * Позволяет отсылать пакет, у объекта которого вызовется метод {@link IPacketCallback#read(ByteBufInputStream)},
     * когда(если) сервер отошлет ответный пакет с тем же packetId и callbackId обратно клиенту.
     *
     * @return callbackId
     * */
    public int sendPacketCallback(IPacketCallback packet) {
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
     * @return {@link SyncResultHandler}, который позволяет выполнить обработку ответа синхронно в основном потоке.
     * Возвращаемый {@link SyncResultHandler} содержит not null результат.
     * */
    public <T> SyncResultHandler<T> sendPacketEffectiveCallback(IPacketCallbackEffective<T> packet) {
        return new SyncResultHandler<>(sendPacketCallbackAsync(packet));
    }

    /**
     * @param checkNonNullResult позволяет проверить результат на null
     * @return {@link SyncResultHandler}, который позволяет выполнить обработку ответа синхронно в основном потоке.
     * */
    public <T> SyncResultHandler<T> sendPacketEffectiveCallback(IPacketCallbackEffective<T> packet, boolean checkNonNullResult) {
        return new SyncResultHandler<>(sendPacketCallbackAsync(packet), checkNonNullResult);
    }

    /**
     * Позволяет отсылать пакет, у объекта которого вызовется метод {@link IPacketCallbackEffective#read(ByteBufInputStream)},
     * когда(если) сервер отошлет ответный пакет с тем же packetId и callbackId обратно клиенту.
     *
     * @return {@link CompletableFuture}, который будет содержать ответ от сервера или NULL, если ответ не был получен
     * или был неверно сконструирован.
     * */
    public <T> CompletableFuture<T> sendPacketCallbackAsync(IPacketCallbackEffective<T> packet) {
        return sendPacketCallbackAsync(packet, defaultCheckResultPeriod);
    }

    /**
     * Позволяет отсылать пакет, у объекта которого вызовется метод {@link IPacketCallbackEffective#read(ByteBufInputStream)},
     * когда(если) сервер отошлет ответный пакет с тем же packetId и callbackId обратно клиенту.
     *
     * @param checkResultPeriod период проверки подготовленного ответа.
     *
     * @return {@link CompletableFuture}, который будет содержать ответ от сервера или NULL, если ответ не был получен
     * или был неверно сконструирован.
     * */
    public <T> CompletableFuture<T> sendPacketCallbackAsync(IPacketCallbackEffective<T> packet, long checkResultPeriod) {
        final int id = sendPacketCallback(packet);
        return CompletableFuture.supplyAsync(() -> {
            long l = System.currentTimeMillis() + callbackResultWaitTimeout;
            while (true) {
                synchronized (callbackResultMap) {
                    if (callbackResultMap.containsKey(id)) {
                        //noinspection unchecked
                        return (T) callbackResultMap.remove(id);
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
            return null;
        }, executorService);
    }

    private int genCallbackId() {
        int i = 1;
        while(callbackMap.containsKey(i)) i++;
        return i;
    }
}
