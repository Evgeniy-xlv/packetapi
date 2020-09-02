package ru.xlv.packetapi.client;

import net.minecraft.client.Minecraft;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Используется в паре с {@link ru.xlv.packetapi.client.packet.IPacketCallbackEffective} и служит для возможности его обработки в синхронном порядке.
 * <p>
 * Прим. использования:
 * <pre>
 *      PacketHandlerClient.sendPacketEffectiveCallback(...)
 *                  .thenAcceptSync(result -> {...});
 * </pre>
 * */
public class SyncResultHandler<T> {

    private static final Consumer<Runnable> mainThreadExecutor = Minecraft.getMinecraft()::addScheduledTask;
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
     * Позволяет обработать асинхронный результат в основном потоке.
     * <p>
     * Будет вызван сразу же после того, как выполнится {@link ru.xlv.packetapi.client.packet.IPacketCallbackEffective#read(ByteBufInputStream)}
     * */
    public void thenAcceptSync(Consumer<T> consumer) {
        completableFuture.thenAccept(result -> {
            if(checkNonNullResult && result == null) {
                return;
            }
            mainThreadExecutor.accept(() -> consumer.accept(result));
        });
    }
}
