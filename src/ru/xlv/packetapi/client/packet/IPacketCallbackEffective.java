package ru.xlv.packetapi.client.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Основной тип запроса, возвращающий результат в качестве установленного типа данных.
 * <p>
 * Следует использовать вместе с {@link ru.xlv.packetapi.client.SyncResultHandler}.
 *
 * Прим. использования:
 * <pre>
 *
 * //...
 *
 *      PacketHandlerClient.sendPacketEffectiveCallback(new MyCallback())
 *                  .thenAcceptSync(result -> {
 *                      if(result.success) {
 *                          System.out.println(result.responseMessage);
 *                      }
 *                  });
 *
 * //...
 *
 * public class MyCallback implements IPacketCallbackEffective<MyCallback.Result> {
 *
 *    private final Result result = new Result();
 *
 *    public void write(ByteBufOutputStream bbos) throws IOException {
 *        // упаковка запроса на сервер
 *        bbos.writeInt(2);
 *    }
 *
 *    public void read(ByteBufInputStream bbis) throws IOException {
 *        // конструкция ответа от сервера в случае, если ответ был получен
 *        result.success = bbis.readBoolean();
 *        result.responseMessage = bbis.readUTF();
 *    }
 *
 *    public Result getResult() {
 *        return result;
 *    }
 *
 *    public static class Result {
 *        public boolean success;
 *        public String responseMessage;
 *    }
 * }
 * <pre>
 *
 * @param <T> тип ответа от сервера.
 * */
public interface IPacketCallbackEffective<T> extends IPacketCallback {

    @Override
    void write(ByteBufOutputStream bbos) throws IOException;

    @Override
    void read(ByteBufInputStream bbis) throws IOException;

    /**
     * @return сконструированный ответ от сервера. Конструкцию ответа следует проводить в {@link IPacketCallbackEffective#read(ByteBufInputStream)}
     * */
    @Nullable
    T getResult();
}
