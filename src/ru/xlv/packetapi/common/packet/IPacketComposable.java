package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Пародия на {@link java.io.Externalizable}. Служит для удобной компоновки объекта в пакет.
 * */
public interface IPacketComposable {

    /**
     * Метод, занимающийся упаковкой данных в буфер.
     * @param writableList может конвертировать {@link Serializable} в байты(см. {@link IPacket#writeObject(ByteBufOutputStream, Serializable)}).
     *                     После конвертации байты попадут в буфер.
     * */
    void writeDataToPacket(List<Object> writableList, ByteBufOutputStream byteBufOutputStream);

    default void readDataFromPacket(ByteBufInputStream byteBufInputStream) throws IOException {}

    /**
     * Используется как дополнительный метод для компоновки данных после вызова {@link IPacketComposable#writeDataToPacket}.
     * */
    default void writeDataToPacketPost(ByteBufOutputStream byteBufOutputStream) {}

    /**
     * Позволяет упаковать все {@link IPacketComposable} объекты из коллекции.
     * */
    default void writeComposableCollection(@Nullable Collection<? extends IPacketComposable> collection, List<Object> writableList, ByteBufOutputStream byteBufOutputStream) {
        if(collection == null) {
            writableList.add(0);
            return;
        }
        writableList.add(collection.size());
        collection.forEach(o -> o.writeDataToPacket(writableList, byteBufOutputStream));
    }

    /**
     * Позволяет упаковать все {@link Serializable} объекты из коллекции.
     * */
    default void writeSerializableCollection(@Nullable Collection<? extends Serializable> collection, List<Object> writableList) {
        if(collection == null) {
            writableList.add(0);
            return;
        }
        writableList.add(collection.size());
        writableList.addAll(collection);
    }
}
