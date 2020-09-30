package ru.xlv.packetapi.example.a1_12_2.autoreg.packets;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacket;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod;

import javax.annotation.Nullable;
import java.io.IOException;

@AutoRegPacket(channelName = AutoRegMod.MODID, registryName = "FirstPacketExample")
public class FirstPacketClientExample implements ICallbackOut<String> {

    private String result;

    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        String s = "Ping.";
        writeObject(bbos, s);
        System.out.println(s);
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
