package ru.xlv.packetapi.example.shop;

import io.netty.buffer.ByteBufOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.xlv.packetapi.client.packet.IPacketCallbackEffective;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class PacketShopCategoryGet implements IPacketCallbackEffective<PacketShopCategoryGet.Result> {

    @Getter
    private final Result result = new Result();

    private String category;

    @Override
    public void write(ByteBufOutputStream bbos) throws IOException {
        bbos.writeUTF(category);
    }

    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        result.shopItemList = readObjects(bbis, ShopItem.class);
        result.responseMessage = bbis.readUTF();
    }

    @Getter
    public static class Result {
        private String responseMessage;
        private List<ShopItem> shopItemList;
    }
}
