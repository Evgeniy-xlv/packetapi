package ru.xlv.packetapi.server;

import ru.xlv.packetapi.common.IPacketHandler;
import ru.xlv.packetapi.common.packet.IPacketOut;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public interface IPacketHandlerServer<PLAYER, PACKET_OUT extends IPacketOut> extends IPacketHandler {

    void sendPacketToPlayer(PLAYER player, PACKET_OUT packet);

    default void sendPacketsToPlayer(PLAYER player, PACKET_OUT... packets) {
        for (PACKET_OUT packet : packets) {
            sendPacketToPlayer(player, packet);
        }
    }

    /**
     * Отправляет пакет всем на сервере.
     * */
    void sendPacketToAll(@Nonnull PACKET_OUT packetOut);

    /**
     * Отправляет пакет всем на сервере, исключая игрока.
     * */
    void sendPacketToAllExcept(@Nonnull PLAYER player, @Nonnull PACKET_OUT packetOut);

    /**
     * Отправляет пакет всем вокруг точки в радиусе.
     * */
    void sendPacketToAllAround(double x, double y, double z, double radius, @Nonnull PACKET_OUT packetOut);

    /**
     * Отправляет пакет всем вокруг существа в радиусе.
     * */
    void sendPacketToAllAround(@Nonnull PLAYER player, double radius, @Nonnull PACKET_OUT packetOut);

    /**
     * Отправляет пакет всем вокруг игрока в радиусе, исключая игрока.
     * */
    void sendPacketToAllAroundExcept(@Nonnull PLAYER player, double radius, @Nonnull PACKET_OUT packetOut);

    Stream<? extends PLAYER> getOnlinePlayers();
}
