package ru.xlv.packetapi.common.composable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This bus lets you register your own catchers of input {@link Composable}.
 * It looks like {@link com.google.common.eventbus.EventBus}, but it is slightly modified.
 * An instance of this bus is located at {@link ru.xlv.packetapi.PacketAPI} class.
 * @see ComposableCatcher here is described the catching process of {@link Composable}.
 * */
public class ComposableCatcherBus {

    private final Logger logger = Logger.getLogger(ComposableCatcherBus.class.getSimpleName());

    private final Map<Class<? extends Serializable>, List<Data>> registry = new HashMap<>();

    /**
     * Posts the {@link Composable} object.
     * */
    public <T extends Composable> void post(@Nonnull T object) {
        post(object, null);
    }

    /**
     * Posts the {@link Composable} object.
     * @param player is a basic Minecraft's EntityPlayer or {@link org.bukkit.entity.Player} of BukkitAPI.
     * */
    public <T extends Composable> void post(@Nonnull T object, @Nullable Object player) {
        List<Data> data = registry.get(object.getClass());
        if (data != null) {
            for (Data aData : data) {
                for (Object o : aData.catcherMethods.keySet()) {
                    List<Method> methods = aData.catcherMethods.get(o);
                    for (Method method : methods) {
                        try {
                            Object[] params = null;
                            if(player != null && method.getParameterCount() > 1) {
                                if(method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                                    params = new Object[] {player, object};
                                } else if(method.getParameterTypes()[1].isAssignableFrom(player.getClass())) {
                                    params = new Object[] {object, player};
                                }
                            } else if(method.getParameterTypes()[0] == object.getClass()) {
                                params = new Object[] {object};
                            }
                            if(params == null) {
                                continue;
                            }
                            method.invoke(o, params);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Registers the catcher object.
     * @see ComposableCatcher to understand what it is.
     * */
    public void register(@Nonnull Object object) {
        register(object, null);
    }

    /**
     * Registers the catcher object.
     * @param aClass is a type of {@link Composable} you want to subscribe the catcher.
     * @see ComposableCatcher to understand what it is.
     * */
    public void register(@Nonnull Object catcher, @Nullable Class<? extends Serializable> aClass) {
        for (Method method : catcher.getClass().getMethods()) {
            ComposableCatcher annotation = method.getAnnotation(ComposableCatcher.class);
            if(annotation != null) {
                if(method.getParameterCount() != 1 && method.getParameterCount() != 2) {
                    logger.warning(String.format("Error! The found method %s has the wrong number of parameters. Please, specify parameters right. " +
                            "There might be only 1 parameter as composable or 2 parameters: composable and EntityPlayer.", method));
                    continue;
                }
                if(aClass == null) {
                    aClass = Composable.class;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if(!aClass.isAssignableFrom(parameterType)) {
                    Class<?> c = parameterType;
                    parameterType = method.getParameterTypes()[1];
                    if(method.getParameterCount() != 2 || aClass.isAssignableFrom(parameterType)) {
                        continue;
                    }
                    parameterType = c;
                }
                //noinspection unchecked
                aClass = (Class<? extends Serializable>) parameterType;
                if(Modifier.isAbstract(method.getModifiers())) {
                    logger.warning(String.format("Error! The found method %s is abstract.", method));
                    continue;
                }
                if(!Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }
                List<Data> data = registry.get(aClass);
                Data dataObj = null;
                if (data == null) {
                    registry.put(aClass, data = new ArrayList<>());
                    dataObj = new Data(parameterType.getName());
                } else {
                    for (Data aData : data) {
                        if(aData.classpath.equals(parameterType.getName())) {
                            dataObj = aData;
                            break;
                        }
                    }
                    if(dataObj == null) {
                        dataObj = new Data(parameterType.getName());
                    }
                }
                List<Method> list = dataObj.catcherMethods.get(catcher);
                if (list == null) {
                    list = new ArrayList<>();
                }
                if (list.contains(method)) {
                    logger.warning(String.format("Error! The found method %s is already registered.", method));
                    continue;
                }
                list.add(method);
                dataObj.catcherMethods.put(catcher, list);
                data.add(dataObj);
            }
        }
    }

    /**
     * Unregisters the catcher.
     * */
    public void unregister(@Nonnull Object catcher) {
        unregister(catcher, null);
    }

    /**
     * Unregisters the catcher.
     * @param aClass is a type of {@link Composable} you want to unsubscribe the catcher.
     * */
    public void unregister(@Nonnull Object catcher, @Nullable Class<? extends Serializable> aClass) {
        if(aClass != null) {
            unregister0(catcher, aClass);
        } else {
            for (Class<? extends Serializable> aClass1 : registry.keySet()) {
                unregister0(catcher, aClass1);
            }
        }
    }

    private void unregister0(@Nonnull Object catcher, @Nonnull Class<? extends Serializable> aClass) {
        List<Data> data = registry.get(aClass);
        if (data != null) {
            for (Data aData : data) {
                aData.catcherMethods.keySet().removeIf(o -> o.equals(catcher));
            }
            data.removeIf(aData -> aData.catcherMethods.isEmpty());
            registry.values().removeIf(List::isEmpty);
        }
    }

    private static class Data {

        private final String classpath;
        private final Map<Object, List<Method>> catcherMethods = new HashMap<>();

        public Data(String classpath) {
            this.classpath = classpath;
        }
    }
}
