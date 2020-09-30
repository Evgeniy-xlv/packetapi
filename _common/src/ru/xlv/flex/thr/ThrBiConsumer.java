package ru.xlv.flex.thr;

import java.io.IOException;

public interface ThrBiConsumer<T, V> {

    void accept(T t, V v) throws IOException;
}
