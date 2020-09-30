package ru.xlv.packetapi.common.packet.autoreg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegPacket {

    /**
     * This name is used by {@link AutoRegPacketScanner}.
     * In case you want your packets to have different names, you should set the same registry name for them.
     * */
    String registryName() default "";

    /**
     *
     * */
    String channelName() default "packetapi";
}
