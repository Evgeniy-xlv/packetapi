package ru.xlv.packetapi.example.a1_12_2.num;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import ru.xlv.packetapi.common.packet.registration.Packet;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.ControllablePacket;
import ru.xlv.packetapi.server.forge.packet.ICallbackInServer;
import ru.xlv.packetapi.server.forge.packet.PacketCallbackSender;

import java.io.IOException;

@Packet
@ControllablePacket(period = 200L, callWriteAnyway = true)
public class MyTestCallbackOnServer implements ICallbackInServer {

    private Integer value;

    @Override
    public void read(EntityPlayer entityPlayer, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException {
        System.out.println("Incoming request from " + entityPlayer.getName());
        value = bbis.readInt();
    }

    @Override
    public void write(EntityPlayer entityPlayer, ByteBufOutputStream bbos) throws IOException {
        System.out.println("Sending the result back to the client side...");
        boolean success = false;
        String message = "You sent a request too often. Request rejected!";
        if (value != null) {
            message = "You haven't guessed the secret number, it's definitely not 4!";
            if(value == 4) {
                success = true;
                message = "You guessed the secret number, it really is 4!";
            }
        }
        bbos.writeBoolean(success);
        bbos.writeUTF(message);
        System.out.println("The result was sent.");
    }
}
