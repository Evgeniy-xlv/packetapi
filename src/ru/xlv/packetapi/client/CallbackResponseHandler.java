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
public class CallbackResponseHandler<T> {

    private final CompletableFuture<CallbackResponseResult<T>> completableFuture;
    private final boolean checkNonNullResult;

    public CallbackResponseHandler(CompletableFuture<CallbackResponseResult<T>> completableFuture) {
        this(completableFuture, true);
    }

    public CallbackResponseHandler(CompletableFuture<CallbackResponseResult<T>> completableFuture, boolean checkNonNullResult) {
        this.completableFuture = completableFuture;
        this.checkNonNullResult = checkNonNullResult;
    }

    /**
     * @deprecated it will be removed in the next versions. Use {@link CallbackResponseHandler#onResult(Consumer)}.
     * */
    @Deprecated
    public CallbackResponseHandler<T> thenAcceptSync(Consumer<T> consumer) {
        return onResult(consumer);
    }

    /**
     * It will be called when the result is successfully constructed.
     * <p>
     * It will be called immediately after {@link IPacketCallbackEffective#read(ByteBufInputStream)} is executed.
     * */
    public CallbackResponseHandler<T> onResult(Consumer<T> consumer) {
        completableFuture.thenAccept(result -> {
            if(result.getState() == CallbackResponseResult.State.CONSTRUCTED) {
                if (checkNonNullResult && result.getResult() == null) {
                    return;
                }
                PacketAPI.getCapabilityAdapter().scheduleTaskSync(() -> consumer.accept(result.getResult()));
            }
        });
        return this;
    }

    /**
     * Execute a task if callback fails due to timeout.
     * <p>
     * It will be called immediately after {@link IPacketCallbackEffective#read(ByteBufInputStream)} is executed.
     * */
    public CallbackResponseHandler<T> onTimeout(Runnable runnable) {
        completableFuture.thenAccept(result -> {
            if(result.getState() == CallbackResponseResult.State.TIME_OUT) {
                PacketAPI.getCapabilityAdapter().scheduleTaskSync(runnable);
            }
        });
        return this;
    }

    /**
     * Execute a task if callback fails due to exception.
     * <p>
     * It will be called immediately after {@link IPacketCallbackEffective#read(ByteBufInputStream)} is executed.
     * */
    public CallbackResponseHandler<T> onException(Runnable runnable) {
        completableFuture.thenAccept(result -> {
            if(result.getState() == CallbackResponseResult.State.EXCEPTION) {
                PacketAPI.getCapabilityAdapter().scheduleTaskSync(runnable);
            }
        });
        return this;
    }
}
