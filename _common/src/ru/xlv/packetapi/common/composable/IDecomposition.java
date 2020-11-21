package ru.xlv.packetapi.common.composable;

import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

public interface IDecomposition<T> {
    T decompose(ByteBufInputStream byteBufInputStream) throws IOException;
}
