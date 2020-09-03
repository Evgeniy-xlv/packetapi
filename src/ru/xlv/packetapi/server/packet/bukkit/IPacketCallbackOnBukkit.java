package ru.xlv.packetapi.server.packet.bukkit;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.IPacketCallback;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.PacketCallbackSender;

import java.io.IOException;

/**
 * Специализированный пакет для обработки запросов на серверной стороне.
 * <p>
 * Использовать только в паре с {@link IPacketCallback} и его потомками, тк эти пакеты дополнительно подписываются!
 *
 * Прим. использования:
 * <pre>
 * public class MyCallbackOnServer implements IPacketCallbackOnBukkit {
 *
 *    private int inputValue;
 *
 *    public void read(Player player, ByteBufInputStream bbis,
 *                      PacketCallbackSender packetCallbackSender) throws IOException {
 *      inputValue = bbis.readInt();
 *    }
 *
 *    public void write(Player player, ByteBufOutputStream bbos) throws IOException {
 *      boolean success = false;
 *      if(inputValue % 2 == 0) {
 *          success = true;
 *      }
 *      bbos.writeBoolean(success);
 *      bbos.writeUTF("Вау! Твое число способно делиться на 2 без остатка. Поздравляю!");
 *    }
 * }
 * <pre>
 * */
public interface IPacketCallbackOnBukkit extends IPacketCallback {

    /**
     * Здесь следует производить чтение данных и их обработку.
     * @param packetCallbackSender использовать только если {@link IPacketCallbackOnBukkit#handleCallback()} == true, иначе будет отослано два пакета.
     * */
    void read(Player player, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException;

    /**
     * Здесь следует производить конструкцию ответа и его записи в буфер. Будет вызван только в случае, если не возникло ошибок
     * при чтении запроса и у PacketCallbackSender в методе {@link IPacketCallbackOnBukkit#read(Player, ByteBufInputStream, PacketCallbackSender)}
     * был вызван метод {@link PacketCallbackSender#send()}.
     * <p>
     * Произойти это может только в двух случаях:
     * <p>
     *  1. Управление пакетом осталось за апи и метод write вызвался сразу за read.
     * <p>
     *  2. Управление было взято программистом, который вручную вызвал метод {@link PacketCallbackSender#send()} в методе read.
     * */
    void write(Player player, ByteBufOutputStream bbos) throws IOException;

    @Deprecated
    @Override
    default void read(ByteBufInputStream bbis) throws IOException {}

    @Deprecated
    @Override
    default void write(ByteBufOutputStream bbos) throws IOException {}

    /**
     * Следует возвращать true, чтобы взять конроль над отправкой колбека в свои руки.
     * */
    default boolean handleCallback() {
        return false;
    }
}