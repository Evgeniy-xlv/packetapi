package ru.xlv.packetapi.client;

public class CallbackResponseResult<T> {

    private final T result;
    private final State state;
    private final Object[] metadata;

    public CallbackResponseResult(T result, State state, Object... metadata) {
        this.result = result;
        this.state = state;
        this.metadata = metadata;
    }

    public State getState() {
        return state;
    }

    public T getResult() {
        return result;
    }

    public Object[] getMetadata() {
        return metadata;
    }

    public enum State {
        CONSTRUCTED,
        TIME_OUT,
        ERROR
    }
}
