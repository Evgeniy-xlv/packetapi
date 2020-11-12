package ru.xlv.packetapi.capability;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public interface TypeLocator {

    default Method findMethod(Class<?> aClass, MethodData... methodData) {
        for (MethodData md : methodData) {
            try {
                return md.get(aClass);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new RuntimeException("Method not found: " + Arrays.toString(methodData));
    }

    default Method findMethod(ClassNames classNames, MethodData... methodData) {
        for (MethodData md : methodData) {
            for (String name : classNames.names) {
                try {
                    return md.get(Class.forName(name));
                } catch (NoSuchMethodException | ClassNotFoundException ignored) {}
            }
        }
        throw new RuntimeException("Method not found: " + Arrays.toString(methodData));
    }

    default Class<?> findClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new RuntimeException("Class not found: " + Arrays.toString(names));
    }

    default ConstructorContainer<?> findConstructor(ClassNames classNames, ParamsData paramsData) {
        for (String name : classNames.names) {
            try {
                return new ConstructorContainer<>(Class.forName(name).getConstructor(paramsData.params));
            } catch (NoSuchMethodException | ClassNotFoundException ignored) {}
        }
        throw new RuntimeException("Class not found: " + classNames + " " + paramsData);
    }

    class ClassNames {
        protected final String[] names;

        public ClassNames(String... names) {
            this.names = names;
        }

        @Override
        public String toString() {
            return "ClassNames{" +
                    "names=" + Arrays.toString(names) +
                    '}';
        }
    }

    class MethodData extends ParamsData {
        protected final String name;
        protected final boolean isDeclared;

        public MethodData(String name, Class<?>... params) {
            this(false, name, params);
        }

        public MethodData(boolean isDeclared, String name, Class<?>... params) {
            super(params);
            this.name = name;
            this.isDeclared = isDeclared;
        }

        public Method get(Class<?> aClass) throws NoSuchMethodException {
            return isDeclared ? aClass.getDeclaredMethod(name, params) : aClass.getMethod(name, params);
        }

        @Override
        public String toString() {
            return "MethodData{" +
                    "name='" + name + '\'' +
                    ", isDeclared=" + isDeclared +
                    ", params=" + Arrays.toString(params) +
                    '}';
        }
    }

    class ParamsData {
        protected final Class<?>[] params;

        public ParamsData(Class<?>... params) {
            this.params = params;
        }

        @Override
        public String toString() {
            return "ParamsData{" +
                    "params=" + Arrays.toString(params) +
                    '}';
        }
    }

    class ConstructorContainer<T> {
        private final Constructor<T> constructor;

        public ConstructorContainer(Constructor<T> constructor) {
            this.constructor = constructor;
        }

        public T newInstance(Object... objects) {
            try {
                return constructor.newInstance(objects);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public Constructor<T> getConstructor() {
            return constructor;
        }
    }
}
