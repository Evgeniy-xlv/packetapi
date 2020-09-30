package ru.xlv.packetapi.common.composable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation lets you to mark the method as catcher method. Such methods are listeners and will be
 * automatically (if they are registered) called by {@link ComposableCatcherBus} when a new object came from the another side.
 * All registered packet handlers support the catching process. They have a special method to send {@link Composable} objects
 * and they are posting every {@link Composable} objects they read to the {@link ComposableCatcherBus}.
 * This is an easiest way to exchanging data between sides at the moment :)
 * <p>
 * You can use two options for forming your catcher methods.
 *  1. Only one parameter: {@link Composable}.
 *  2. Two parameters in any order: {@link Composable} and the player(EntityPlayer, if you are working with Forge or Player if it is BukkitAPI)
 * */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ComposableCatcher {
}
