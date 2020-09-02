package ru.xlv.flex.thr;

import java.io.IOException;

public interface ThrCallable<T> {

    T call() throws IOException;
}
