package ru.xlv.packetapi.common.packet.registration;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.logging.Logger;

public abstract class PacketRegistrationRouter {

    protected static final Logger LOGGER = Logger.getLogger(PacketRegistrationRouter.class.getSimpleName());

    public void scanThenRegister(@Nonnull Class<?> modClass) {
        PacketSubscriber[] annotations = getAnnotations(modClass);
        if (annotations == null) {
            LOGGER.warning("No AutoRegPacketSubscriber annotations found for " + modClass.getName() + "! Skipping...");
            return;
        }
        for (PacketSubscriber annotation : annotations) {
            if(annotation.enableReflectionsScanner()) {
                List<String> list = annotation.packages().length > 0 ? Arrays.asList(annotation.packages()) : Collections.singletonList(modClass.getPackage().getName());
                for (String s : list) {
                    scanThenRegister(annotation.channelName(), s);
                }
            }
            List<Class<?>> list = annotation.packets().length > 0 ? Arrays.asList(annotation.packets()) : Collections.emptyList();
            for (Class<?> aClass : list) {
                register(annotation.channelName(), aClass);
            }
        }
    }

    private PacketSubscriber[] getAnnotations(@Nonnull Class<?> modClass) {
        if (modClass.isAnnotationPresent(PacketSubscriber.class)) {
            return new PacketSubscriber[] { modClass.getAnnotation(PacketSubscriber.class) };
        } else if(modClass.isAnnotationPresent(PacketSubscriberContainer.class)) {
            return modClass.getAnnotation(PacketSubscriberContainer.class).value();
        }
        return null;
    }

    protected abstract void register(@Nonnull String channelName, @Nonnull Class<?> packetClass);

    protected void scanThenRegister(@Nonnull String channelName, @Nonnull String path) {
        LOGGER.info("Scanning " + path + " for packets...");
        scanForPacketClasses(path)
                .stream()
                .sorted((o1, o2) -> {
                    Packet annotation = o1.getAnnotation(Packet.class);
                    Packet annotation1 = o2.getAnnotation(Packet.class);
                    String a = !annotation.registryName().equals("") ? annotation.registryName() : o1.getName();
                    String b = !annotation1.registryName().equals("") ? annotation1.registryName() : o2.getName();
                    return a.compareTo(b);
                })
                .forEach(packetClass -> register(channelName, packetClass));
    }

    protected Set<Class<?>> scanForPacketClasses(String path) {
        try {
            Class.forName("org.reflections.Reflections");
            return new ReflectionsAnnotationScanner().scanPacketClasses(path);
        } catch (ClassNotFoundException ignored) {
        }
        return new HashSet<>();
    }
}
