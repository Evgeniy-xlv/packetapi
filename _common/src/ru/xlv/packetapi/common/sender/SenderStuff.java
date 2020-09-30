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
import ru.xlv.packetapi.common.packet.PacketRegistrationException;
import ru.xlv.packetapi.server.IPacketOutServerRaw;
import ru.xlv.packetapi.server.bukkit.PacketAPIBukkitPlugin;
import ru.xlv.packetapi.server.bukkit.PacketHandlerBukkit;
import ru.xlv.packetapi.server.bukkit.packet.IPacketOutBukkit;
import ru.xlv.packetapi.server.forge.PacketHandlerServer;
import ru.xlv.packetapi.server.forge.packet.IPacketOutServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SenderStuff {

    private SenderStuff() {}

    public static class ComposableSide<T extends Composable> {

        private final T composable;

        public ComposableSide(T composable) {
            this.composable = composable;
        }

        public ComposableClientSender<T> fromClient() {
            return new ComposableClientSender<>(composable);
        }

        public ComposableTarget<T, EntityPlayer, ComposableServerSender<T>> fromServer() {
            return new ComposableTarget<>(new ComposableServerSender<>(composable), () -> PacketAPI.getCapabilityAdapter().getOnlinePlayersStream(EntityPlayer.class).collect(Collectors.toList()));
        }

        public ComposableTarget<T, Player, ComposableBukkitSender<T>> fromBukkit() {
            return new ComposableTarget<>(new ComposableBukkitSender<>(composable), PacketAPIBukkitPlugin::getOnlinePlayers);
        }
    }

    public static abstract class ComposableSender<T extends Composable> {

        protected final T composable;

        private ComposableSender(T composable) {
            this.composable = composable;
        }

        public abstract void send();
    }

    public static class ComposableClientSender<T extends Composable> extends ComposableSender<T> {

        private ComposableClientSender(T composable) {
            super(composable);
        }

        @Override
        public void send() {
            PacketHandlerClient.getInstance().sendComposable(composable);
        }
    }

    public abstract static class ComposableBaseServerSender<T extends Composable, PLAYER, SENDER extends ComposableBaseServerSender<T, PLAYER, ?>> extends ComposableSender<T> {

        protected final List<PLAYER> playerList = new ArrayList<>();

        private ComposableBaseServerSender(T composable) {
            super(composable);
        }

        public SENDER except(@Nonnull PLAYER player) {
            playerList.remove(player);
            //noinspection unchecked
            return (SENDER) this;
        }

        public SENDER except(@Nonnull PLAYER... players) {
            playerList.removeAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) this;
        }

        public SENDER except(@Nonnull Collection<PLAYER> players) {
            playerList.removeAll(players);
            //noinspection unchecked
            return (SENDER) this;
        }

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

        public SENDER toAll() {
            baseServerSender.playerList.addAll(getOnlinePlayers.get());
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

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

        public SENDER to(@Nonnull PLAYER player) {
            baseServerSender.playerList.add(player);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull PLAYER... players) {
            baseServerSender.playerList.addAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull Collection<PLAYER> players) {
            baseServerSender.playerList.addAll(players);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull Stream<PLAYER> players) {
            players.forEach(baseServerSender.playerList::add);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }
    }

    public static class ComposableServerSender<T extends Composable> extends ComposableBaseServerSender<T, EntityPlayer, ComposableServerSender<T>> {

        private ComposableServerSender(T composable) {
            super(composable);
        }

        @Override
        public void send() {
            for (EntityPlayer player : playerList) {
                PacketHandlerServer.getInstance().sendComposable(player, composable);
            }
        }
    }

    public static class ComposableBukkitSender<T extends Composable> extends ComposableBaseServerSender<T, Player, ComposableBukkitSender<T>> {

        private ComposableBukkitSender(T composable) {
            super(composable);
        }

        @Override
        public void send() {
            for (Player player : playerList) {
                PacketHandlerBukkit.getInstance().sendComposable(player, composable);
            }
        }
    }

    public static class CallbackSender<PACKET extends ICallbackOut<RESULT>, RESULT> extends Sender<PACKET, CallbackResponseHandler<RESULT>> {

        private long timeout = 2000L;
        private long resultCheckPeriod = 0L;
        private boolean checkNonnullResult = true;

        CallbackSender(PACKET packet) {
            super(packet);
        }

        public CallbackSender<PACKET, RESULT> timeout(long millis) {
            this.timeout = millis;
            return this;
        }

        public CallbackSender<PACKET, RESULT> resultCheckPeriod(long millis) {
            this.resultCheckPeriod = millis;
            return this;
        }

        public CallbackSender<PACKET, RESULT> checkNonnullResult(boolean checkNonnullResult) {
            this.checkNonnullResult = checkNonnullResult;
            return this;
        }

        @Override
        public CallbackResponseHandler<RESULT> send() {
            String channelNameByPacket = PacketHandlerClient.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
            if (channelNameByPacket != null) {
                return PacketHandlerClient.getInstance().sendCallback(channelNameByPacket, packet, timeout, resultCheckPeriod, checkNonnullResult);
            }
            return CallbackResponseHandler.wrap(null, CallbackResponseResult.State.ERROR, new PacketRegistrationException());
        }
    }

    public static class ClientSender<PACKET extends IPacketOutClient> extends Sender<PACKET, Void> {

        ClientSender(PACKET packet) {
            super(packet);
        }

        @Nullable
        @Override
        public Void send() {
            String channelNameByPacket = PacketHandlerClient.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
            if (channelNameByPacket != null) {
                PacketHandlerClient.getInstance().sendPacketToServer(channelNameByPacket, packet);
            }
            return null;
        }
    }

    public abstract static class BaseServerSender<PACKET extends IPacketOutServerRaw<PLAYER>, PLAYER, SENDER extends BaseServerSender<PACKET, PLAYER, ?>> extends Sender<PACKET, Void> {

        protected final List<PLAYER> playerList = new ArrayList<>();

        private BaseServerSender(PACKET packet) {
            super(packet);
        }

        public SENDER except(@Nonnull PLAYER player) {
            playerList.remove(player);
            //noinspection unchecked
            return (SENDER) this;
        }

        public SENDER except(@Nonnull PLAYER... players) {
            playerList.removeAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) this;
        }

        public SENDER except(@Nonnull Collection<PLAYER> players) {
            playerList.removeAll(players);
            //noinspection unchecked
            return (SENDER) this;
        }

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

        public SENDER toAll() {
            baseServerSender.playerList.addAll(getOnlinePlayers.get());
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

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

        public SENDER to(@Nonnull PLAYER player) {
            baseServerSender.playerList.add(player);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull PLAYER... players) {
            baseServerSender.playerList.addAll(Arrays.asList(players));
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull Collection<PLAYER> players) {
            baseServerSender.playerList.addAll(players);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }

        public SENDER to(@Nonnull Stream<PLAYER> players) {
            players.forEach(baseServerSender.playerList::add);
            //noinspection unchecked
            return (SENDER) baseServerSender;
        }
    }

    public static class ServerSender<PACKET extends IPacketOutServer> extends BaseServerSender<PACKET, EntityPlayer, ServerSender<PACKET>> {

        ServerSender(PACKET packet) {
            super(packet);
        }

        @Nullable
        @Override
        public Void send() {
            String channelNameByPacket = PacketHandlerServer.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
            if (channelNameByPacket != null) {
                for (EntityPlayer player : playerList) {
                    PacketHandlerServer.getInstance().sendPacketToPlayer(channelNameByPacket, player, packet);
                }
            }
            return null;
        }
    }

    public static class BukkitSender<PACKET extends IPacketOutBukkit> extends BaseServerSender<PACKET, Player, BukkitSender<PACKET>> {

        BukkitSender(PACKET packet) {
            super(packet);
        }

        @Nullable
        @Override
        public Void send() {
            String channelNameByPacket = PacketHandlerBukkit.getInstance().getPacketRegistry().findChannelNameByPacket(packet);
            if (channelNameByPacket != null) {
                for (Player player : playerList) {
                    PacketHandlerBukkit.getInstance().sendPacketToPlayer(channelNameByPacket, player, packet);
                }
            }
            return null;
        }
    }
}
