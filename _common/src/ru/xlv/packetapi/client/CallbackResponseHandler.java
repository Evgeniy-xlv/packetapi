package ru.xlv.packetapi.client;

import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Used in conjunction with {@link ICallbackOut} and serves to be able to process it in a synchronous order.
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
     * It will be called when the result is successfully constructed.
     * <p>
     * It will be called immediately after {@link ICallbackOut#read(ByteBufInputStream)} is executed.
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
     * It will be called immediately after {@link ICallbackOut#read(ByteBufInputStream)} is executed.
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
     * It will be called immediately after {@link ICallbackOut#read(ByteBufInputStream)} is executed.
     * */
    public CallbackResponseHandler<T> onError(Consumer<Exception> consumer) {
        completableFuture.thenAccept(result -> {
            if(result.getState() == CallbackResponseResult.State.ERROR) {
                Exception exception = result.getMetadata().length > 0 && result.getMetadata()[0] instanceof Exception ? (Exception) result.getMetadata()[0] : null;
                PacketAPI.getCapabilityAdapter().scheduleTaskSync(() -> consumer.accept(exception));
            }
        });
        return this;
    }

    public static <T> CallbackResponseHandler<T> wrap(T object, CallbackResponseResult.State state, Object... metadata) {
        return new CallbackResponseHandler<>(CompletableFuture.completedFuture(new CallbackResponseResult<>(object, state, metadata)));
    }
}
