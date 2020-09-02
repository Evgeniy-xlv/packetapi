package ru.xlv.packetapi.common.packet;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;

/**
 * Отличается от всех пакетов тем, что дополнительно подписывается и заносится в кеш, пока не дождется ответа от сервера, подписанного тем же pid, либо не получит таймаут.
 * Когда(если) придет ответ от сервера, будет вызван {@link IPacketCallback#read(ByteBufInputStream)}.
 * <p>
 * Важно понимать, что пакет может быть отправлен только от клиента к серверу и никак иначе. Серверный пакет тоже должен наследовать данный интерфейс.
 * */
public interface IPacketCallback extends IPacket {

    /**
     * Здесь следует производить запись в пакет.
     * */
    void write(ByteBufOutputStream bbos) throws IOException;

    /**
     * Вызывается в случае получения ответа от сервера.
     * */
    void read(ByteBufInputStream bbis) throws IOException;
}
