package ru.xlv.packetapi.server;

import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.forge.packet.IPacketInServer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Eliminates unnecessary writing a {@link RequestController} in each packet.
 * Instead, it is enough to mark the class with an annotation and a new controller with the specified parameters will be automatically generated for it.
 *
 * It is important to understand that if the processing of a packet was rejected by such a controller,
 * the method {@link IPacketInServer#read(net.minecraft.entity.player.EntityPlayer, ByteBufInputStream)} will not be called.
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllablePacket {

    /**
     * Permissible period for {@link RequestController.Periodic}
     * */
    long period() default -1;

    /**
     * Permissible limit for {@link RequestController.Limited}
     * */
    int limit() default -1;

    /**
     * Allows to call the write method of packet, even if the request was denied by the controller.
     * */
    boolean callWriteAnyway() default false;
}
