package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrCallable;
import ru.xlv.flex.thr.ThrFunction;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public interface IPacket {

    /**
     * Writes a collection to the buffer, using {@link ThrBiConsumer}.
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
     * Reads the data and add it to {@link ArrayList}, using {@link ThrFunction}.
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
     * Reads the data and add it to {@link ArrayList}, using {@link ThrCallable}.
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
     * Writes all {@link Serializable} from input {@link ArrayList} to the buffer as {@link ArrayList}.
     * @deprecated use {@link IPacket#writeObjects(ByteBufOutputStream, Collection)} directly.
     * */
    @Deprecated
    default <T extends Serializable> void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull ArrayList<T> arrayList) throws IOException {
        writeObjects(byteBufOutputStream, (Collection<T>) arrayList);
    }

    /**
     * Writes all {@link Serializable} from input {@link Collection} to the buffer as {@link ArrayList}.
     * */
    default <T extends Serializable> void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull Collection<T> collection) throws IOException {
        byteBufOutputStream.writeInt(collection.size());
        for (T t : collection) {
            writeObject(byteBufOutputStream, t);
        }
    }

    /**
     * Writes all {@link Serializable} from input array of {@link Serializable} to the buffer as {@link ArrayList}.
     * */
    default void writeObjects(@Nonnull ByteBufOutputStream byteBufOutputStream, @Nonnull Serializable... serializables) throws IOException {
        byteBufOutputStream.writeInt(serializables.length);
        for (Serializable serializable : serializables) {
            writeObject(byteBufOutputStream, serializable);
        }
    }

    /**
     * Attempts to read {@link ArrayList} from the buffer that should contain objects of type tClass.
     * By default, all elements will be added to the new {@link ArrayList}.
     * @param byteBufInputStream is the buffer
     * @param tClass is the class extending Serializable, whose elements will be read from the buffer.
     * @return read {@link ArrayList} or empty {@link ArrayList} on failure.
     * */
    @Nonnull
    default <T extends Serializable> ArrayList<T> readObjects(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull Class<T> tClass) throws IOException {
        return readObjects(byteBufInputStream, tClass, new ArrayList<>());
    }

    /**
     * Attempts to read {@link ArrayList} from the buffer that should contain objects of type tClass.
     * @param byteBufInputStream is the buffer
     * @param tClass is the class extending Serializable, whose elements will be read from the buffer.
     * @param collection is the collection to which all read elements will be added.
     * @return read {@link ArrayList} or empty {@link ArrayList} on failure.
     * */
    @Nonnull
    default <T extends Serializable, V extends Collection<T>> V readObjects(@Nonnull ByteBufInputStream byteBufInputStream, @Nonnull Class<T> tClass, @Nonnull V collection) throws IOException {
        int length = byteBufInputStream.readInt();
        for (int i = 0; i < length; i++) {
            T t = readObject(byteBufInputStream, tClass);
            collection.add(t);
        }
        return collection;
    }

    /**
     * Writes {@link Serializable} to the buffer.
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
        } else if(serializable instanceof Composable) {
            PacketAPI.getComposer().compose((Composable) serializable, byteBufOutputStream);
        } else {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteBufOutputStream);
            objectOutputStream.writeObject(serializable);
        }
    }

    /**
     * Reads {@link Serializable} from the buffer.
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
        } else if(Composable.class.isAssignableFrom(tClass)) {
            return (T) PacketAPI.getComposer().decompose(byteBufInputStream);
        } else {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(byteBufInputStream);
                Object o = objectInputStream.readObject();
                if(tClass == o) {
                    return (T) o;
                } else {
                    throw new IOException(tClass + " != " + o.getClass());
                }
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }
}
