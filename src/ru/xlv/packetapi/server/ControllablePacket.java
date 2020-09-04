package ru.xlv.packetapi.server;

import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.packet.forge.IPacketInOnServerRaw;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Избавляет от лишнего написания {@link RequestController} в каждом пакете.
 * Вместо этого достаточно пометить класс аннотацией и для нее автоматически сгенерируется новый контроллер с заданными параметрами.
 *
 * Важно понимать, что если обработка пакета была отклонена подобным контроллером,
 * метод {@link IPacketInOnServerRaw#read(Object, ByteBufInputStream)} не будет вызван.
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllablePacket {

    /**
     * Допустимый период для {@link RequestController.Periodic}
     * */
    long period() default -1;

    /**
     * Допустимый лимит для {@link RequestController.Limited}
     * */
    int limit() default -1;

    /**
     * Позволяет вызывать метод записи у пакета, даже если запрос был отклонен контроллером.
     * */
    boolean callWriteAnyway() default false;
}
