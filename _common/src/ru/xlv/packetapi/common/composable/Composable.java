package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A simple object based on {@link Serializable}. It serves for a convenient exchange of objects between the client and the server.
 * <p>
 * The serializing process of {@link Composable} is easy to understand. It is based on serialization of {@link Serializable} objects in java.
 * @see Serializable to undestand what the object should look like for successful serialization.
 * @see Composable to understand how it is serializing.
 * */
public interface Composable extends Serializable {

    static <T extends Composable> void compose(T composable, ByteBufOutputStream byteBufOutputStream) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteBufOutputStream);
        objectOutputStream.writeObject(composable);
        objectOutputStream.close();
    }

    @Nonnull
    static Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        ObjectInputStream objectInputStream = new ObjectInputStream(byteBufInputStream);
        try {
            Object o = objectInputStream.readObject();
            if (o instanceof Composable) {
                return (Composable) o;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        throw new IOException("Unexpected exception.");
    }
}
