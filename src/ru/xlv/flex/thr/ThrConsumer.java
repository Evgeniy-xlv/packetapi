package ru.xlv.flex.thr;

import java.io.IOException;

public interface ThrConsumer<T> {

    void accept(T t) throws IOException;
}
