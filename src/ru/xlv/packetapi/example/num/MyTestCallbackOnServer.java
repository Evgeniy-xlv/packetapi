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
        System.out.println("Входящий запрос от " + entityPlayer.getName());
        value = bbis.readInt();
    }

    @Override
    public void write(EntityPlayerMP entityPlayer, ByteBufOutputStream bbos) throws IOException {
        System.out.println("Отсылаю результат...");
        boolean success = false;
        String message = "Вы слишком часто отсылали запрос. Запрос отклонен!";
        if (value != null) {
            message = "Вы не угадали секретное число, это точно не 4!";
            if(value == 4) {
                success = true;
                message = "Вы угадали секретное число, это действительно 4!";
            }
        }
        bbos.writeBoolean(success);
        bbos.writeUTF(message);
        System.out.println("Результат отослан.");
    }
}
