package ru.xlv.packetapi.common.packet.autoreg;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Logger;

public interface IAutoRegPacketScanner {

    Logger LOGGER = Logger.getLogger(IAutoRegPacketScanner.class.getSimpleName());

    default void scanThenRegister(@Nonnull Class<?> modClass) {
        AutoRegPacketSubscriber annotation = modClass.getAnnotation(AutoRegPacketSubscriber.class);
        List<String> list = annotation.packages().length > 0 ? Arrays.asList(annotation.packages()) : Collections.singletonList(modClass.getPackage().getName());
        for (String s : list) {
            scanThenRegister(s);
        }
        List<Class<?>> list1 = annotation.classes().length > 0 ? Arrays.asList(annotation.classes()) : Collections.emptyList();
        for (Class<?> aClass : list1) {
            register(aClass);
        }
    }

    void register(Class<?> packetClass);

    default void scanThenRegister(@Nonnull String path) {
        LOGGER.info("Scanning " + path + " for packets...");
        scanForPacketClasses(path)
                .stream()
                .sorted((o1, o2) -> {
                    AutoRegPacket annotation = o1.getAnnotation(AutoRegPacket.class);
                    AutoRegPacket annotation1 = o2.getAnnotation(AutoRegPacket.class);
                    String a = !annotation.registryName().equals("") ? annotation.registryName() : o1.getName();
                    String b = !annotation1.registryName().equals("") ? annotation1.registryName() : o2.getName();
                    return a.compareTo(b);
                })
                .forEach(this::register);
    }

    default Set<Class<?>> scanForPacketClasses(String path) {
        try {
            Class.forName("org.reflections.Reflections");
            return new ReflectionsAnnotationScanner().scanPacketClasses(path);
        } catch (ClassNotFoundException ignored) {
        }
        return new HashSet<>();
    }
}
