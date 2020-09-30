package ru.xlv.packetapi.common.packet.autoreg;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Set;

public class ReflectionsAnnotationScanner {

    public Set<Class<?>> scanPacketClasses() {
        Reflections reflections = new Reflections(new TypeAnnotationsScanner(), new SubTypesScanner());
        return reflections.getTypesAnnotatedWith(AutoRegPacket.class);
    }

    public Set<Class<?>> scanPacketClasses(String path) {
        Reflections reflections = new Reflections(path, new TypeAnnotationsScanner(), new SubTypesScanner());
        return reflections.getTypesAnnotatedWith(AutoRegPacket.class);
    }
}
