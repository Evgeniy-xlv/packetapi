package ru.xlv.packetapi.example.a1_12_2.composable;

import ru.xlv.packetapi.common.composable.Composable;

public class TestKeyComposable implements Composable {

    private final int keyCode;
    private final boolean isPressed;

    public TestKeyComposable(int keyCode, boolean isPressed) {
        this.keyCode = keyCode;
        this.isPressed = isPressed;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public boolean isPressed() {
        return isPressed;
    }

    @Override
    public String toString() {
        return "TestKeyComposable{" +
                "keyCode=" + keyCode +
                ", isPressed=" + isPressed +
                '}';
    }
}
