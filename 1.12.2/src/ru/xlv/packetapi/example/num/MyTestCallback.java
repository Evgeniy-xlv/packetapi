package ru.xlv.packetapi.example.num;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;

public class MyTestCallback implements IPacketCallbackEffective<MyTestCallback.Result> {

    private final Result result = new Result();

    private int value;

    public MyTestCallback() {}

    public MyTestCallback(int value) {
        this.value = value;
    }

    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        bbos.writeInt(value);
    }

    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        result.success = bbis.readBoolean();
        result.responseMessage = bbis.readUTF();
    }

    @Nullable
    @Override
    public Result getResult() {
        return result;
    }

    public static class Result {

        private boolean success;
        private String responseMessage;

        public boolean isSuccess() {
            return success;
        }

        public String getResponseMessage() {
            return responseMessage;
        }
    }
}
