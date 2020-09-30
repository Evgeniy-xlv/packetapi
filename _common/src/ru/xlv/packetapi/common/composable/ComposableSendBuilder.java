package ru.xlv.packetapi.common.composable;

import ru.xlv.packetapi.common.packet.NoPacketDestinationException;

import javax.annotation.Nonnull;

public class ComposableSendBuilder<T extends Composable, PLAYER> {

    protected final T composable;
    protected final PacketSender<T, PLAYER> packetSender;
    protected Boolean toServer;
    protected PLAYER player;

    private ComposableSendBuilder(T composable, PacketSender<T, PLAYER> packetSender) {
        this.composable = composable;
        this.packetSender = packetSender;
    }

    public void send() throws NoPacketDestinationException {
        if (toServer == null) {
            throw new NoPacketDestinationException("You are trying to send a composable, but you didn't specify a destination.");
        }
        packetSender.send(player, composable);
    }

    public static <T extends Composable, PLAYER> Target<T, PLAYER> of(@Nonnull Class<PLAYER> playerClass, T composable, @Nonnull PacketSender<T, PLAYER> packetSender) {
        return new Target<>(composable, packetSender);
    }

    public static class Target<T extends Composable, PLAYER> extends ComposableSendBuilder<T, PLAYER> {

        private Target(T composable, PacketSender<T, PLAYER> packetSender) {
            super(composable, packetSender);
        }

        public ComposableSendBuilder<T, PLAYER> toServer() {
            toServer = true;
            return this;
        }

        public ComposableSendBuilder<T, PLAYER> toPlayer(@Nonnull PLAYER player) {
            toServer = false;
            this.player = player;
            return this;
        }
    }

    public interface PacketSender<T extends Composable, PLAYER> {
        void send(PLAYER player, T composable);
    }
}
