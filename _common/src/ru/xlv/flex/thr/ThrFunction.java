package ru.xlv.flex.thr;

import java.io.IOException;

public interface ThrFunction<T, R> {

    R apply(T t) throws IOException;
}
