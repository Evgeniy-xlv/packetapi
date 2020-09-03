package ru.xlv.packetapi.server;

import ru.xlv.packetapi.common.IPacketHandler;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.common.packet.IPacket;
import ru.xlv.packetapi.common.packet.IPacketOut;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public interface IPacketHandlerServer<PLAYER, PACKET_OUT extends IPacketOut> extends IPacketHandler {

    default void scanAnnotations(PacketRegistry packetRegistry, Map<Class<? extends IPacket>, PacketData> packetMap) {
        for (Class<? extends IPacket> aClass : packetRegistry.getClassRegistry().keySet()) {
            PacketData packetData = new PacketData();
            {
                AsyncPacket annotation = aClass.getAnnotation(AsyncPacket.class);
                if (annotation != null) {
                    packetData.isAsync = true;
                }
            }
            {
                ControllablePacket annotation = aClass.getAnnotation(ControllablePacket.class);
                if (annotation != null) {
                    packetData.callWriteAnyway = annotation.callWriteAnyway();
                    packetData.requestLimit = annotation.limit();
                    packetData.requestPeriod = annotation.period();
                }
            }
            if(packetData.requestPeriod != -1) {
                packetData.requestController = new RequestController.Periodic<>(packetData.requestPeriod);
            } else if(packetData.requestLimit != -1) {
                packetData.requestController = new RequestController.Limited<>(packetData.requestLimit);
            }
            packetMap.put(aClass, packetData);
        }
    }

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

    class PacketData {
        protected RequestController<UUID> requestController;
        protected boolean isAsync;
        protected long requestPeriod;
        protected int requestLimit;
        protected boolean callWriteAnyway;
    }
}
