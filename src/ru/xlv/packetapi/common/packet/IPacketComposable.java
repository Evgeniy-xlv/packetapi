package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * A parody of {@link java.io.Externalizable}. Serves for convenient arrangement of an object into a packet.
 * @deprecated use {@link ru.xlv.packetapi.common.composable.Composable}
 * */
@Deprecated
public interface IPacketComposable {

    void writeDataToPacket(List<Object> writableList, ByteBufOutputStream byteBufOutputStream);

    default void readDataFromPacket(ByteBufInputStream byteBufInputStream) throws IOException {}

    default void writeDataToPacketPost(ByteBufOutputStream byteBufOutputStream) {}

    default void writeComposableCollection(@Nullable Collection<? extends IPacketComposable> collection, List<Object> writableList, ByteBufOutputStream byteBufOutputStream) {
        if(collection == null) {
            writableList.add(0);
            return;
        }
        writableList.add(collection.size());
        collection.forEach(o -> o.writeDataToPacket(writableList, byteBufOutputStream));
    }

    default void writeSerializableCollection(@Nullable Collection<? extends Serializable> collection, List<Object> writableList) {
        if(collection == null) {
            writableList.add(0);
            return;
        }
        writableList.add(collection.size());
        writableList.addAll(collection);
    }
}
