package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrCallable;
import ru.xlv.flex.thr.ThrFunction;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;

public interface IPacket {

    /**
     * Позволяет записать коллекцию в буфер, используя {@link ThrBiConsumer}.
     * */
    default <T> void writeCollection(@Nonnull ByteBufOutputStream byteBufOutputStream, Collection<T> collection, @Nonnull ThrBiConsumer<ByteBufOutputStream, T> consumer) throws IOException {
        if(collection == null) {
            byteBufOutputStream.writeInt(0);
            return;
        }
        byteBufOutputStream.writeInt(collection.size());
        for (T t : collection) {
            consumer.accept(byteBufOutputStream, t);
        }
    }

    /**
     * Позволяет прочитать данные и добавить их в {@link ArrayList}, используя {@link ThrFunction}.
     * */
    default <T> ArrayList<T> readList(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull ThrFunction<ByteBufInputStream, T> function) throws IOException {
        ArrayList<T> list = new ArrayList<>();
        int c = byteBufInputStream.readInt();
        for (int i = 0; i < c; i++) {
            list.add(function.apply(byteBufInputStream));
        }
        return list;
    }

    /**
     * Позволяет прочитать данные и добавить их в {@link ArrayList}, используя {@link ThrCallable}.
     * */
    default <T> ArrayList<T> readList(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull ThrCallable<T> callable) throws IOException {
        ArrayList<T> list = new ArrayList<>();
        int c = byteBufInputStream.readInt();
        for (int i = 0; i < c; i++) {
            list.add(callable.call());
        }
        return list;
    }

    /**
     * Позволяет записать {@link IPacketComposable} в буфер.
     * */
    default <T extends IPacketComposable> void writeComposable(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull T object) throws IOException {
        List<Object> list = new LinkedList<>();
        object.writeDataToPacket(list, byteBufOutputStream);
        for (Object object1 : list) {
            if(object1 instanceof Serializable) {
                writeObject(byteBufOutputStream, (Serializable) object1);
            } else {
                throw new IOException("Cannot write data into ByteBufOutputStream. " + object1 + " isn't serializable!");
            }
        }
        object.writeDataToPacketPost(byteBufOutputStream);
    }

    default <T extends IPacketComposable> T readComposable(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull T object) throws IOException {
        object.readDataFromPacket(byteBufInputStream);
        return object;
    }

    /**
     * @param tClass должен иметь пустой конструктор
     * */
    default <T extends IPacketComposable & Serializable> T readComposable(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull Class<T> tClass) throws IOException {
        return readObject(byteBufInputStream, tClass);
    }

    /**
     * Позволяет записать все {@link Serializable} из входящего {@link ArrayList} в буфер в качестве {@link ArrayList}.
     * */
    default <T extends Serializable> void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull ArrayList<T> arrayList) throws IOException {
        writeObject(byteBufOutputStream, arrayList);
    }

    /**
     * Позволяет записать все {@link Serializable} из входящей {@link Collection} в буфер в качестве {@link ArrayList}.
     * */
    default <T extends Serializable> void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull Collection<T> collection) throws IOException {
        writeObject(byteBufOutputStream, new ArrayList<>(collection));
    }

    /**
     * Позволяет записать все {@link Serializable} из входящего массива {@link Serializable]} в буфер в качестве {@link ArrayList}.
     * */
    default void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull Serializable... serializables) throws IOException {
        writeObject(byteBufOutputStream, (ArrayList<Serializable>) Arrays.asList(serializables));
    }

    /**
     * Пытается прочитать {@link ArrayList} из буфера, который должен содержать объекты типа tClass.
     * @return полученный {@link ArrayList}, либо пустой {@link ArrayList} при неудаче.
     * */
    @Nonnull
    @SuppressWarnings("CastCanBeRemovedNarrowingVariableType")
    default <T extends Serializable> ArrayList<T> readObjects(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull Class<T> tClass) throws IOException {
        ArrayList<?> arrayList = readObject(byteBufInputStream, ArrayList.class);
        if (!arrayList.isEmpty() && arrayList.get(0).getClass().isAssignableFrom(tClass)) {
            //noinspection unchecked
            return (ArrayList<T>) arrayList;
        }
        return new ArrayList<>(0);
    }

    /**
     * Позволяет записать {@link Serializable} в буфер.
     * <p>
     * Рекомендуется использовать {@link IPacket#writeObjects} для записи нескольких объектов сразу.
     * */
    default void writeObject(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull Serializable serializable) throws IOException {
        if(serializable instanceof String) {
            byteBufOutputStream.writeUTF((String) serializable);
        } else if(serializable instanceof Integer) {
            byteBufOutputStream.writeInt((Integer) serializable);
        } else if(serializable instanceof Byte) {
            byteBufOutputStream.writeByte((Byte) serializable);
        } else if(serializable instanceof Boolean) {
            byteBufOutputStream.writeBoolean((Boolean) serializable);
        } else if(serializable instanceof Long) {
            byteBufOutputStream.writeLong((Long) serializable);
        } else if(serializable instanceof Float) {
            byteBufOutputStream.writeFloat((Float) serializable);
        } else if(serializable instanceof Double) {
            byteBufOutputStream.writeDouble((Double) serializable);
        } else if(serializable instanceof Character) {
            byteBufOutputStream.writeChar((Character) serializable);
        } else if(serializable instanceof Short) {
            byteBufOutputStream.writeShort((Short) serializable);
        } else if(serializable instanceof byte[]) {
            byteBufOutputStream.writeBytes(new String((byte[]) serializable));
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);
            objectOutputStream.flush();
            byteBufOutputStream.write(byteArrayOutputStream.toByteArray());
            objectOutputStream.close();
        }
    }

    /**
     * Позволяет прочитать {@link Serializable} из буфера.
     * <p>
     * Используйте {@link IPacket#readObjects} для чтения нескольких объектов за раз, т.к. данный метод не изменяет {@link io.netty.buffer.ByteBuf#readerIndex} у входящего byteBufInputStream.
     * */
    @SuppressWarnings("unchecked")
    default  <T extends Serializable> T readObject(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull Class<T> tClass) throws IOException {
        if(tClass == String.class) {
            return (T) byteBufInputStream.readUTF();
        } else if(tClass == Integer.class || tClass == int.class) {
            return (T) new Integer(byteBufInputStream.readInt());
        } else if(tClass == Byte.class || tClass == byte.class) {
            return (T) new Byte(byteBufInputStream.readByte());
        } else if(tClass == Boolean.class || tClass == boolean.class) {
            return (T) Boolean.valueOf(byteBufInputStream.readBoolean());
        } else if(tClass == Long.class || tClass == long.class) {
            return (T) new Long(byteBufInputStream.readLong());
        } else if(tClass == Float.class || tClass == float.class) {
            return (T) new Float(byteBufInputStream.readFloat());
        } else if(tClass == Double.class || tClass == double.class) {
            return (T) new Double(byteBufInputStream.readDouble());
        } else if(tClass == Character.class || tClass == char.class) {
            return (T) new Character(byteBufInputStream.readChar());
        } else if(tClass == Short.class || tClass == short.class) {
            return (T) new Short(byteBufInputStream.readShort());
        } else if(tClass == byte[].class) {
            return (T) byteBufInputStream.readUTF().getBytes();
        } else {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Unpooled.copiedBuffer(byteBufInputStream.getBuffer()).array());
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                return (T) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }
}
