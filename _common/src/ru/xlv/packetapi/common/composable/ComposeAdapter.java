package ru.xlv.packetapi.common.composable;

public class ComposeAdapter<T> {

    private final IComposition<T> composition;
    private final IDecomposition<T> decomposition;

    public ComposeAdapter(IComposition<T> composition, IDecomposition<T> decomposition) {
        this.composition = composition;
        this.decomposition = decomposition;
    }

    protected IComposition<T> getComposition() {
        return composition;
    }

    protected IDecomposition<T> getDecomposition() {
        return decomposition;
    }
}
