package ru.xlv.packetapi.common.packet.registration;

import ru.xlv.packetapi.PacketAPI;

import java.lang.annotation.*;

/**
 * Indicates the class as a subscriber to the packet system. This annotation should only be declared above
 * the main class of your mod, i.e. class annotated with @Mod.
 * <pre>
 * This way allows you to:
 *  - define the name of the network channel
 *  - define the packet classes for this channel
 *  - use auto scanning utils </pre>
 *
 * @see PacketSubscriber
 * */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PacketSubscriberContainer.class)
public @interface PacketSubscriber {

    /**
     * The name of the channel where you want to register your packets
     * */
    String channelName() default PacketAPI.DEFAULT_NET_CHANNEL_NAME;

    /**
     * You can define the classes annotated by {@link Packet} manually, using this method.
     * */
    Class<?>[] packets() default {};

    /**
     * By default, the annotation scanner only works in the package that contains the AutoRegPacketSubscriber
     * annotated class, but you can define more packages to scan using this method.
     * */
    String[] packages() default {};

    /**
     * Enables packet scanning using the Reflections library
     * */
    boolean enableReflectionsScanner() default false;
}
