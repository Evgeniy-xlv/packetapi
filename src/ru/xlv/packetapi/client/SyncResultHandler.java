package ru.xlv.packetapi.client;

import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Used in conjunction with {@link IPacketCallbackEffective} and serves to be able to process it in a synchronous order.
 * <p>
 * Usage example:
 * <pre>
 *      PacketHandlerClient.sendPacketEffectiveCallback(...)
 *                  .thenAcceptSync(result -> {...});
 * </pre>
 * */
public class SyncResultHandler<T> {

    private final CompletableFuture<T> completableFuture;
    private final boolean checkNonNullResult;

    public SyncResultHandler(CompletableFuture<T> completableFuture) {
        this(completableFuture, true);
    }

    public SyncResultHandler(CompletableFuture<T> completableFuture, boolean checkNonNullResult) {
        this.completableFuture = completableFuture;
        this.checkNonNullResult = checkNonNullResult;
    }

    /**
     * Handles asynchronous result in the main thread.
     * <p>
     * It will be called immediately after {@link IPacketCallbackEffective#read(ByteBufInputStream)} is executed.
     * */
    public void thenAcceptSync(Consumer<T> consumer) {
        completableFuture.thenAccept(result -> {
            if(checkNonNullResult && result == null) {
                return;
            }
            PacketAPI.getCapabilityAdapter().scheduleTaskSync(() -> consumer.accept(result));
        });
    }
}
