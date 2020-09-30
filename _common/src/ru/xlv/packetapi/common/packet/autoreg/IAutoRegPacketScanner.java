package ru.xlv.packetapi.common.packet.autoreg;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public interface IAutoRegPacketScanner {

    Logger LOGGER = Logger.getLogger(IAutoRegPacketScanner.class.getSimpleName());

    default void scanThenRegister(@Nonnull Class<?> modClass) {
        AutoRegPacketSubscriber annotation = modClass.getAnnotation(AutoRegPacketSubscriber.class);
        List<String> list = annotation.packages().length > 0 ? Arrays.asList(annotation.packages()) : Collections.singletonList(modClass.getPackage().getName());
        for (String s : list) {
            scanThenRegister(s);
        }
    }

    void scanThenRegister(@Nonnull String path);

    default Set<Class<?>> scanPacketClasses(String path) {
        try {
            Class.forName("org.reflections.Reflections");
            return new ReflectionsAnnotationScanner().scanPacketClasses(path);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("UnexpectedError caught! Couldn't scan classes for annotations. Please, install the Reflections library.");
    }
}
