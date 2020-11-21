package ru.xlv.packetapi.common.composable;

class LightweightDeep {

    private final int deep;
    private final boolean increasing;

    LightweightDeep(int deep, boolean increasing) {
        this(deep, increasing, false);
    }

    private LightweightDeep(int deep, boolean increasing, boolean selfConstruction) {
        this.deep = selfConstruction ? deep : increasing ? Integer.MAX_VALUE - deep : deep;
        this.increasing = increasing;
    }

    LightweightDeep next() {
        return new LightweightDeep(deep + (increasing ? 1 : -1), increasing, true);
    }

    boolean isLightweight() {
        return increasing ? Integer.MAX_VALUE - deep > 0 : deep > 0;
    }
}
