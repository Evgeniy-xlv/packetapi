package ru.xlv.packetapi.common.sender;

import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.CallbackResponseHandler;
import ru.xlv.packetapi.client.CallbackResponseResult;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.client.packet.ICallbackOut;
import ru.xlv.packetapi.client.packet.IPacketOutClient;
import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.server.IPacketOutServerRaw;
import ru.xlv.packetapi.server.bukkit.PacketAPIBukkitPlugin;
import ru.xlv.packetapi.server.bukkit.PacketHandlerBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;
import ru.xlv.packetapi.server.forge.PacketHandlerServer;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SenderStuff {

    private SenderStuff() {}

    public static class ComposableSide<T extends Composable> {

        private final Stream<T> composableStream;

        public ComposableSide(Stream<T> stream) {
            this.composableStream = stream;
        }

        /**
         * @return Sender intended for the client side
         * */
        public ComposableClientSender<T> fromClient() {
            return new ComposableClientSender<>(composableStream);
        }

        /**
         * @return Sender intended for the forge-server side
         * */
        public ComposableTarget<T, EntityPlayer, ComposableServerSender<T>> fromServer() {
            return new ComposableTarget<>(new ComposableServerSender<>(composableStream), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
        }

        /**
         * @return Sender intended for the bukkit side
         * */
        public ComposableTarget<T, Player, ComposableBukkitSender<T>> fromBukkit() {
            return new ComposableTarget<>(new ComposableBukkitSender<>(composableStream), PacketAPIBukkitPlugin::getOnlinePlayers);
        }
    }

    public static abstract class ComposableSender<T extends Composable> {

        protected final Stream<T> composableStream;

        private ComposableSender(Stream<T> composableStream) {
            this.composableStream = composableStream;
        }

        public abstract void send();
    }

    public static class ComposableClientSender<T extends Composable> extends ComposableSender<T> {

        private ComposableClientSender(Stream<T> composable) {
            super(composable);
        }

        /**
         * Sends composables to the server
         * */
        @Override
        public void send() {
            composableStream.forEach(t -> PacketHandlerClient.getInstance().sendComposable(t));
        }
    }

    public abstract static class ComposableBaseServerSender<T extends Composable, PLAYER, SENDER extends ComposableBaseServerSender<T, PLAYER, ?>> extends ComposableSender<T> {

        protected final List<PLAYER> playerList = new ArrayList<>();

        private ComposableBaseServerSender(Stream<T> composable) {
            super(composable);
        }

        /**
         * Excepts the player from the recipient list
         * */
        public SENDER except(@Nonnull PLAYER player) {
            playerList.remove(player);
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        @SafeVarargs
        public final SENDER except(@Nonnull PLAYER... players) {
            playerList.removeAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        public SENDER except(@Nonnull Collection<PLAYER> players) {
            playerList.removeAll(players);
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        public SENDER except(@Nonnull Predicate<PLAYER> predicate) {
            playerList.removeIf(predicate);
            //noinspection unchecked
            return (SENDER) this;
        }
    }

    public static class ComposableTarget<T extends Composable, PLAYER, SENDER extends ComposableBaseServerSender<T, PLAYER, ?>>  {

        private final ComposableBaseServerSender<T, PLAYER, ?> baseServerSender;
        private final Supplier<Collection<PLAYER>> getOnlinePlayers;

        private ComposableTarget(ComposableBaseServerSender<T, PLAYER, ?> baseServerSender, Supplier<Collection<PLAYER>> getOnlinePlayers) {
            this.baseServerSender = baseServerSender;
            this.getOnlinePlayers = getOnlinePlayers;
        }

        /**
         * Adds all players in the dimension to the recipient list
         * */
        public SENDER toDimension(int dimension) {
            baseServerSender.playerList.addAll(
                    getOnlinePlayers.get()
                            .stream()
                            .filter(player -> PacketAPI.getCapabilityAdapter().getPlayerDimension(player) == dimension)
                            .collect(Collectors.toList())
            );
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds all online players to the recipient list
         * */
        public SENDER toAll() {
            baseServerSender.playerList.addAll(getOnlinePlayers.get());
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds all filtered online players to the recipient list
         * @param predicate filter
         * */
        public SENDER toAll(@Nonnull Predicate<PLAYER> predicate) {
            baseServerSender.playerList.addAll(
                    getOnlinePlayers.get()
                            .stream()
                            .filter(predicate)
                            .collect(Collectors.toList())
            );
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds the player to the recipient list
         * */
        public SENDER to(@Nonnull PLAYER player) {
            baseServerSender.playerList.add(player);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        @SafeVarargs
        public final SENDER to(@Nonnull PLAYER... players) {
            baseServerSender.playerList.addAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        public SENDER to(@Nonnull Collection<PLAYER> players) {
            baseServerSender.playerList.addAll(players);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        public SENDER to(@Nonnull Stream<PLAYER> players) {
            players.forEach(baseServerSender.playerList::add);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }
    }

    public static class ComposableServerSender<T extends Composable> extends ComposableBaseServerSender<T, EntityPlayer, ComposableServerSender<T>> {

        private ComposableServerSender(Stream<T> composable) {
            super(composable);
        }

        /**
         * Sends composables to recipients
         * */
        @Override
        public void send() {
            for (EntityPlayer player : playerList) {
                composableStream.forEach(t -> PacketHandlerServer.getInstance().sendComposable(player, t));
            }
        }
    }

    public static class ComposableBukkitSender<T extends Composable> extends ComposableBaseServerSender<T, Player, ComposableBukkitSender<T>> {

        private ComposableBukkitSender(Stream<T> composable) {
            super(composable);
        }

        /**
         * Sends composables to recipients
         * */
        @Override
        public void send() {
            for (Player player : playerList) {
                composableStream.forEach(t -> PacketHandlerBukkit.getInstance().sendComposable(player, t));
            }
        }
    }

    public static class CallbackSender<PACKET extends ICallbackOut<RESULT>, RESULT> extends PacketSender<PACKET, CallbackResponseHandler<RESULT>> {

        private long timeout = 2000L;
        private long resultCheckPeriod = 0L;
        private boolean checkNonnullResult = true;

        CallbackSender(Stream<PACKET> packetStream) {
            super(packetStream);
        }

        /**
         * Sets the timeout to wait the callback result.
         * @see PacketHandlerClient#sendCallback(String, ICallbackOut, long, long, boolean)
         * */
        public CallbackSender<PACKET, RESULT> timeout(long millis) {
            this.timeout = millis;
            return this;
        }

        /**
         * Sets the period for checking the callback result. By default, it is 0.
         * @see PacketHandlerClient#sendCallback(String, ICallbackOut, long, long, boolean)
         * */
        public CallbackSender<PACKET, RESULT> resultCheckPeriod(long millis) {
            this.resultCheckPeriod = millis;
            return this;
        }

        /**
         * Sets the checkNonnullResult param.
         * @see PacketHandlerClient#sendCallback(String, ICallbackOut, long, long, boolean)
         * */
        public CallbackSender<PACKET, RESULT> checkNonnullResult(boolean checkNonnullResult) {
            this.checkNonnullResult = checkNonnullResult;
            return this;
        }

        /**
         * Sends the callback to the server
         * */
        @Override
        public CallbackResponseHandler<RESULT> send() {
            Optional<PACKET> first = packetStream.findFirst();
            if (first.isPresent()) {
                PACKET packet = first.get();
                String channelNameByPacket = PacketHandlerClient.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
                if (channelNameByPacket != null) {
                    return PacketHandlerClient.getInstance().sendCallback(channelNameByPacket, packet, timeout, resultCheckPeriod, checkNonnullResult);
                }
                return CallbackResponseHandler.wrap(null, CallbackResponseResult.State.ERROR, new PacketRegistrationException());
            }
            throw new RuntimeException("Unexpected error.");
        }
    }

    public static abstract class PacketSender<PACKET extends IPacket, T> {

        protected final Stream<PACKET> packetStream;

        PacketSender(Stream<PACKET> packetStream) {
            this.packetStream = packetStream;
        }

        abstract T send();
    }

    public static class ClientSender<PACKET extends IPacketOutClient> extends PacketSender<PACKET, Void> {

        ClientSender(Stream<PACKET> packetStream) {
            super(packetStream);
        }

        /**
         * Sends packets to the server
         * */
        @Nullable
        @Override
        public Void send() {
            packetStream.forEach(packet -> {
                String channelNameByPacket = PacketHandlerClient.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
                if (channelNameByPacket != null) {
                    PacketHandlerClient.getInstance().sendPacketToServer(channelNameByPacket, packet);
                }
            });
            return null;
        }
    }

    public abstract static class BaseServerSender<PACKET extends IPacketOutServerRaw<PLAYER>, PLAYER, SENDER extends BaseServerSender<PACKET, PLAYER, ?>> extends PacketSender<PACKET, Void> {

        protected final List<PLAYER> playerList = new ArrayList<>();

        private BaseServerSender(Stream<PACKET> packetStream) {
            super(packetStream);
        }

        /**
         * Excepts the player from the recipient list
         * */
        public SENDER except(@Nonnull PLAYER player) {
            playerList.remove(player);
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        @SafeVarargs
        public final SENDER except(@Nonnull PLAYER... players) {
            playerList.removeAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        public SENDER except(@Nonnull Collection<PLAYER> players) {
            playerList.removeAll(players);
            //noinspection unchecked
            return (SENDER) this;
        }

        /**
         * Excepts players from the recipient list
         * */
        public SENDER except(@Nonnull Predicate<PLAYER> predicate) {
            playerList.removeIf(predicate);
            //noinspection unchecked
            return (SENDER) this;
        }
    }

    public static class Target<PACKET extends IPacketOutServerRaw<PLAYER>, PLAYER, SENDER extends BaseServerSender<PACKET, PLAYER, ?>> {

        private final BaseServerSender<PACKET, PLAYER, ?> baseServerSender;
        private final Supplier<Collection<PLAYER>> getOnlinePlayers;

        Target(BaseServerSender<PACKET, PLAYER, ?> baseServerSender, Supplier<Collection<PLAYER>> getOnlinePlayers) {
            this.baseServerSender = baseServerSender;
            this.getOnlinePlayers = getOnlinePlayers;
        }

        /**
         * Adds all players in the dimension to the recipient list
         * */
        public SENDER toDimension(int dimension) {
            baseServerSender.playerList.addAll(
                    getOnlinePlayers.get()
                            .stream()
                            .filter(player -> PacketAPI.getCapabilityAdapter().getPlayerDimension(player) == dimension)
                            .collect(Collectors.toList())
            );
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds all online players to the recipient list
         * */
        public SENDER toAll() {
            baseServerSender.playerList.addAll(getOnlinePlayers.get());
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds all filtered online players to the recipient list
         * @param predicate filter
         * */
        public SENDER toAll(@Nonnull Predicate<PLAYER> predicate) {
            baseServerSender.playerList.addAll(
                    getOnlinePlayers.get()
                            .stream()
                            .filter(predicate)
                            .collect(Collectors.toList())
            );
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds the player to the recipient list
         * */
        public SENDER to(@Nonnull PLAYER player) {
            baseServerSender.playerList.add(player);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        @SafeVarargs
        public final SENDER to(@Nonnull PLAYER... players) {
            baseServerSender.playerList.addAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        public SENDER to(@Nonnull Collection<PLAYER> players) {
            baseServerSender.playerList.addAll(players);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        /**
         * Adds players to the recipient list
         * */
        public SENDER to(@Nonnull Stream<PLAYER> players) {
            players.forEach(baseServerSender.playerList::add);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }
    }

    public static class ServerSender<PACKET extends IPacketOutServer> extends BaseServerSender<PACKET, EntityPlayer, ServerSender<PACKET>> {

        ServerSender(Stream<PACKET> packetStream) {
            super(packetStream);
        }

        /**
         * Sends packets to recipients
         * */
        @Nullable
        @Override
        public Void send() {
            packetStream.forEach(packet -> {
                String channelNameByPacket = PacketHandlerServer.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
                if (channelNameByPacket != null) {
                    for (EntityPlayer player : playerList) {
                        PacketHandlerServer.getInstance().sendPacketToPlayer(channelNameByPacket, player, packet);
                    }
                }
            });
            return null;
        }
    }

    public static class BukkitSender<PACKET extends IPacketOutBukkit> extends BaseServerSender<PACKET, Player, BukkitSender<PACKET>> {

        BukkitSender(Stream<PACKET> packetStream) {
            super(packetStream);
        }

        /**
         * Sends packets to recipients
         * */
        @Nullable
        @Override
        public Void send() {
            packetStream.forEach(packet -> {
                String channelNameByPacket = PacketHandlerBukkit.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
                if (channelNameByPacket != null) {
                    for (Player player : playerList) {
                        PacketHandlerBukkit.getInstance().sendPacketToPlayer(channelNameByPacket, player, packet);
                    }
                }
            });
            return null;
        }
    }
}
