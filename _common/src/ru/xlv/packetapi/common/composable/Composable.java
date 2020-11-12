package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.io.Serializable;

/**
 * A simple object based on {@link Serializable}. It serves for a convenient exchange of objects between the client and the server.
 * <p>
 * The serializing process of {@link Composable} is easy to understand. It is based on serialization of {@link Serializable} objects in java.
 * @see Serializable to undestand what the object should look like for successful serialization.
 * @see Composer to understand how the composition process works.
 * */
public interface Composable extends Serializable {

    default void compose(ByteBufOutputStream byteBufOutputStream) throws IOException {}

    default Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        return null;
    }
}
