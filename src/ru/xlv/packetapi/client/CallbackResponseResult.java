package ru.xlv.packetapi.client;

public class CallbackResponseResult<T> {

    private final T result;
    private final State state;

    CallbackResponseResult(T result, State state) {
        this.result = result;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public T getResult() {
        return result;
    }

    public enum State {
        CONSTRUCTED,
        TIME_OUT,
        EXCEPTION
    }
}
