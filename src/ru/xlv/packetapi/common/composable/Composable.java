package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.Serializable;

/**
 * A simple object based on {@link Serializable}. It serves for a convenient exchange of objects between the client and the server.
 * <p>
 * The serializing process of {@link Composable} is easy to understand. It is based on serialization of {@link java.io.Serializable} objects in java.
 * @see Serializable to undestand what the object should look like for successful serialization.
 * @see Composer to understand how it is serializing.
 * */
public interface Composable extends Serializable {

    /**
     * Override this method if you want to change the serialization process of your object.
     * */
    default void serialize(ByteBufOutputStream byteBufOutputStream) {}

    /**
     * Override this method if you want to change the deserialization process of your object.
     * */
    default void deserialize(ByteBufInputStream byteBufInputStream) {}
}
