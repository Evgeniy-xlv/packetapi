package ru.xlv.packetapi.example.a1_7_10.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.common.packet.registration.Packet;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;

@Packet(registryName = "FirstPacketExample")
public class FirstPacketClientExample implements ICallbackOut<String> {

    private String result;

    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        writeObject(bbos, "Ping.");
    }

    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        result = readObject(bbis, String.class);
    }

    @Nullable
    @Override
    public String getResult() {
        return result;
    }
}
