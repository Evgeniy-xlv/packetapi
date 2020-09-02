package ru.xlv.packetapi.server.packet;

/**
 * Служит как инструмент для ручного управления отправкой пакета обратно на клиент.
 * <p>
 * Применим при асинхронной работе с пакетом, либо при использовании {@link ru.xlv.packetapi.server.RequestController}.
 * */
public interface PacketCallbackSender {

    /**
     * Позволяет отослать пакет в тот же момент.
     * Советую всегда лишний раз убеждаться, что вы не вызвали метод дважды.
     * */
    void send();
}
