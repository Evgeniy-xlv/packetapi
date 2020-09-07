package ru.xlv.packetapi.client;

class CallbackResponseResult<T> {

    private final T result;
    private final State state;

    CallbackResponseResult(T result, State state) {
        this.result = result;
        this.state = state;
    }

    State getState() {
        return state;
    }

    T getResult() {
        return result;
    }

    enum State {
        CONSTRUCTED,
        TIME_OUT,
        EXCEPTION
    }
}
