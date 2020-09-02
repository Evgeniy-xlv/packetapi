package ru.xlv.packetapi.example.shop;

import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.xlv.packetapi.common.packet.IPacketComposable;

import java.io.Serializable;
import java.util.List;

@ToString
@Getter
@RequiredArgsConstructor
public class ShopItem implements IPacketComposable, Serializable {

    private final int id;
    private final String name;
    private final int price;

    @Override
    public void writeDataToPacket(List<Object> writableList, ByteBufOutputStream byteBufOutputStream) {
        writableList.add(this);
    }
}
