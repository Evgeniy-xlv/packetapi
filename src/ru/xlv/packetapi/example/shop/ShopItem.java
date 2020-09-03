package ru.xlv.packetapi.example.shop;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.packet.IPacketComposable;

import java.io.Serializable;
import java.util.List;

public class ShopItem implements IPacketComposable, Serializable {

    private final int id;
    private final String name;
    private final int price;

    public ShopItem(int id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    @Override
    public void writeDataToPacket(List<Object> writableList, ByteBufOutputStream byteBufOutputStream) {
        writableList.add(this);
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getPrice() {
        return this.price;
    }

    public String toString() {
        return "ShopItem(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ")";
    }
}
