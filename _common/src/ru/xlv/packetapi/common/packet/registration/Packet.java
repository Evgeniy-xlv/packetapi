package ru.xlv.packetapi.common.packet.registration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be used to indicate the class as packet class.
 *
 * @see PacketSubscriber
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Packet {

    /**
     * This name is used by {@link PacketRegistrationRouterForge}.
     * In case you want your packets to have different names, you should set the same registry name for them.
     * */
    String registryName() default "";
}
