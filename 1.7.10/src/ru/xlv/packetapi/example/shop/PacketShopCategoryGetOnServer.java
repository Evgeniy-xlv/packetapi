package ru.xlv.packetapi.example.shop;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayerMP;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.RequestController;
import ru.xlv.packetapi.server.packet.PacketCallbackSender;
import ru.xlv.packetapi.server.packet.forge.IPacketCallbackOnServer;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PacketShopCategoryGetOnServer implements IPacketCallbackOnServer {

    private static final RequestController<UUID> REQUEST_CONTROLLER = new RequestController.Periodic<>(1000L);

    private List<ShopItem> shopItems;
    private long period;

    @Override
    public void read(EntityPlayerMP entityPlayer, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException {
        String category = bbis.readUTF();
        System.out.println("Incoming request shop items by category: " + category);
        long l = System.currentTimeMillis();
        REQUEST_CONTROLLER.doCompletedRequestAsync(entityPlayer.getUniqueID(), () -> {
            shopItems = ShopMod.INSTANCE.getShopItemManager().getItemListByCategory(category);
            period = System.currentTimeMillis() - l;
            System.out.println("Request processed. Sending the result back to the client side...");
            packetCallbackSender.send();
        });
    }

    @Override
    public void write(EntityPlayerMP entityPlayer, ByteBufOutputStream bbos) throws IOException {
        writeObjects(bbos, shopItems);
        bbos.writeUTF("The processing of request took " + period + " millis");
    }

    @Override
    public boolean handleCallback() {
        return true;
    }
}
