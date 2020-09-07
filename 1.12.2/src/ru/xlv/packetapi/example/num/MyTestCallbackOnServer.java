package ru.xlv.packetapi.example.num;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.ControllablePacket;
import ru.xlv.packetapi.server.packet.PacketCallbackSender;
import ru.xlv.packetapi.server.packet.forge.IPacketCallbackOnServer;

import java.io.IOException;

@ControllablePacket(period = 200L, callWriteAnyway = true)
public class MyTestCallbackOnServer implements IPacketCallbackOnServer {

    private Integer value;

    @Override
    public void read(EntityPlayerMP entityPlayer, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException {
        System.out.println("Incoming request from " + entityPlayer.getName());
        value = bbis.readInt();
    }

    @Override
    public void write(EntityPlayerMP entityPlayer, ByteBufOutputStream bbos) throws IOException {
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
