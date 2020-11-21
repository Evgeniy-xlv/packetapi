package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;

public interface IComposition<T> {
    void compose(T t, ByteBufOutputStream byteBufOutputStream) throws IOException;
}
