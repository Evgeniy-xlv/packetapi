package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;

/**
 * A simple object based on {@link Serializable} idea. It serves for a convenient exchange of objects between
 * the client and the server.
 * <p>
 * The serializing process of {@link Composable} is easy to understand. It is based on serialization idea
 * of {@link Serializable} objects in Java, but much more compact and faster.
 * <p>
 * Important:
 * <p>
 * - Composable, the order of its fields and their data types must be identical on both logical sides, otherwise
 *      something and somewhere will flare up and explode.
 * <p>
 * - Composable does not use the usual Java serialization. Only the values of its fields are packed, which makes it
 *      much lighter. In addition, packing some data types in Composable is simplified as much as possible.
 *      For example, enum is passed as an integer using its ordinal. Keep this in mind when working with Composable.
 *      If you want to rewrite the process of packing any data type (the same enum, for example), then pay attention
 *      to the text below.
 * <p>
 * - The Composable packer natively does not support anything other than primitives, strings, enums, collections,
 *      arrays, maps, and Composable themselves. However, you can expand this list without any problems by registering
 *      your adapter using {@link Composer#registerComposeAdapter(Class, IComposition, IDecomposition)}.
 *      It is important to understand that this method is not intended for the Composables themselves. If you suddenly
 *      decide to rewrite the packaging logic of your Composable from A to Z, you just need to override the
 *      corresponding methods inside your Composable.
 * @see Composer to understand how the composition process works.
 * */
public interface Composable extends Serializable {

    /**
     * Override this method if you want to change the composing process of your Composable.
     * */
    default void compose(ByteBufOutputStream byteBufOutputStream) throws IOException {}

    /**
     * Override this method if you want to change the decomposing process of your Composable.
     * */
    @Nonnull
    default Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        //noinspection ConstantConditions
        return null;
    }
}
