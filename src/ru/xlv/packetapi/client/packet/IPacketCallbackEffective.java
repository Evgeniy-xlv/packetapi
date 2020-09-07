package ru.xlv.packetapi.client.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.CallbackResponseHandler;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * The main type of request that returns a result as a specified data type.
 * <p>
 * Should be used in conjunction with {@link CallbackResponseHandler}.
 *
 * Usage example:
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
 *        // packing the request to the server
 *        bbos.writeInt(2);
 *    }
 *
 *    public void read(ByteBufInputStream bbis) throws IOException {
 *        // constructing of a response from the server in case a response was received
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
 * @param <RESULT> the type of response from the server.
 * */
public interface IPacketCallbackEffective<RESULT> extends IPacketCallback {

    @Override
    void write(ByteBufOutputStream bbos) throws IOException;

    @Override
    void read(ByteBufInputStream bbis) throws IOException;

    /**
     * @return a constructed response from the server. The response should be constructed in {@link IPacketCallbackEffective#read(ByteBufInputStream)}
     * */
    @Nullable
    RESULT getResult();
}
