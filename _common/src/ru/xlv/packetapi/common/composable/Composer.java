package ru.xlv.packetapi.common.composable;

import com.google.common.primitives.Primitives;
import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class Composer {

    private final Map<String, ClassComposeMetadata> classComposeMetadataMap = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, ComposeAdapter> classComposeAdapterMap = new HashMap<>();

    private final ReflectionFactory reflectionFactory = ReflectionFactory.getReflectionFactory();
    private Unsafe unsafe;

    public Composer() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers new {@link ComposeAdapter}.
     * @param type is the type whose composition process you want to implement for the Composable system.
     * @param composition is the composition process of the target class.
     * @param decomposition is the decomposition process of the target class.
     * @throws UnsupportedOperationException if the target type is Composable; or if the target type is primitive; or if {@link ComposeAdapter} for the target type is already registered.
     * */
    public <T> void registerComposeAdapter(@Nonnull Class<T> type, @Nonnull IComposition<T> composition, @Nonnull IDecomposition<T> decomposition) {
        if(type.isPrimitive() || Primitives.isWrapperType(type))
            throw new UnsupportedOperationException("ComposeAdapter isn't designed to override the composition process for primitive types.");
        if(Composable.class.isAssignableFrom(type))
            throw new UnsupportedOperationException("ComposeAdapter is designed to extend the list of data types supported by the Composable system. To change the composition process for Composable, override the corresponding method inside the Composable class.");
        if (classComposeAdapterMap.containsKey(type))
            throw new UnsupportedOperationException("ComposeAdapter for this type is already registered!");
        classComposeAdapterMap.put(type, new ComposeAdapter<>(composition, decomposition));
    }

    public <T extends Composable> void compose(T composable, ByteBufOutputStream byteBufOutputStream) throws IOException {
        if(composable == null) throw new IOException("Composable is null!");
        try {
            writeObject(composable, byteBufOutputStream);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    public Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        if (unsafe == null)
            throw new IOException("Unexpected error");
        try {
            return (Composable) readObject(byteBufInputStream);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    private Object readObject(ByteBufInputStream byteBufInputStream) throws IOException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String className = byteBufInputStream.readUTF();
        Class<?> aClass = Class.forName(className);
        if(!Composable.class.isAssignableFrom(aClass))
            throw new IOException(aClass.getName() + " isn't Composable!");
        Class<?> c = aClass;
        while(Serializable.class.isAssignableFrom(c)) {
            c = c.getSuperclass();
        }
        Constructor<?> constructor = c.getConstructor((Class<?>[]) null);
        constructor = reflectionFactory.newConstructorForSerialization(aClass, constructor);
        Object o = constructor.newInstance();
        ClassComposeMetadata classComposeMetadata = getClassComposeData(aClass);
        if(classComposeMetadata.overrideDecompose) {
            Composable decompose = ((Composable) o).decompose(byteBufInputStream);
            if (decompose == null)
                throw new IOException(aClass.getName() + " overrides the decomposition process, but returns null!");
            return decompose;
        }
        int primitives = classComposeMetadata.primitives;
        for (int i = 0; i < classComposeMetadata.fields.size(); i++) {
            Field declaredField = classComposeMetadata.fields.get(i);
            Class<?> type = declaredField.getType();
            if(primitives > 0 && type.isPrimitive())
                primitives--;
            boolean skip = !type.isPrimitive() && !byteBufInputStream.readBoolean();
            if(skip)
                continue;
            long l = unsafe.objectFieldOffset(declaredField);
            if(type == Integer.class || type == int.class) {
                unsafe.putInt(o, l, byteBufInputStream.readInt());
            } else if(type == Byte.class || type == byte.class) {
                unsafe.putByte(o, l, byteBufInputStream.readByte());
            } else if(type == Boolean.class || type == boolean.class) {
                unsafe.putBoolean(o, l, byteBufInputStream.readBoolean());
            } else if(type == Long.class || type == long.class) {
                unsafe.putLong(o, l, byteBufInputStream.readLong());
            } else if(type == Float.class || type == float.class) {
                unsafe.putFloat(o, l, byteBufInputStream.readFloat());
            } else if(type == Double.class || type == double.class) {
                unsafe.putDouble(o, l, byteBufInputStream.readDouble());
            } else if(type == Character.class || type == char.class) {
                unsafe.putChar(o, l, byteBufInputStream.readChar());
            } else if(type == Short.class || type == short.class) {
                unsafe.putShort(o, l, byteBufInputStream.readShort());
            } else {
                Object o1 = null;
                if(type == String.class) {
                    o1 = byteBufInputStream.readUTF();
                } else if(Enum.class.isAssignableFrom(type)) {
                    Object[] values = (Object[]) type.getDeclaredMethod("values").invoke(null);
                    int ordinal = byteBufInputStream.readInt();
                    o1 = values[ordinal];
                } else if(Collection.class.isAssignableFrom(type)) {
                    Collection<Object> collection;
                    if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                        collection = new ArrayList<>();
                    } else {
                        //noinspection unchecked
                        collection = (Collection<Object>) type.newInstance();
                    }
                    int i1 = byteBufInputStream.readInt();
                    for (int j = 0; j < i1; j++) {
                        collection.add(readObject(byteBufInputStream));
                    }
                    o1 = collection;
                } else if(Map.class.isAssignableFrom(type)) {
                    Map<Object, Object> map;
                    if(type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                        map = new HashMap<>();
                    } else {
                        //noinspection unchecked
                        map = (Map<Object, Object>) type.newInstance();
                    }
                    int i1 = byteBufInputStream.readInt();
                    for (int j = 0; j < i1; j++) {
                        map.put(readObject(byteBufInputStream), readObject(byteBufInputStream));
                    }
                    o1 = map;
                } else if(type.isArray()) {
                    int length = byteBufInputStream.readInt();
                    Object[] array = new Object[length];
                    for (int j = 0; j < length; j++) {
                        Object o2 = readObject(byteBufInputStream);
                        array[j] = o2;
                    }
                    o1 = array;
                } else if(Composable.class.isAssignableFrom(type)) {
                    o1 = readObject(byteBufInputStream);
                } else if(classComposeAdapterMap.containsKey(type)) {
                    try {
                        o1 = classComposeAdapterMap.get(type).decomposition.decompose(byteBufInputStream);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
                unsafe.putObject(o, l, o1);
            }
        }
        return o;
    }

    private void writeObject(Composable object, ByteBufOutputStream byteBufOutputStream) throws IOException, IllegalAccessException, NoSuchMethodException {
        byteBufOutputStream.writeUTF(object.getClass().getName());
        ClassComposeMetadata classComposeMetadata = getClassComposeData(object.getClass());
        if(classComposeMetadata.overrideCompose) {
            object.compose(byteBufOutputStream);
            return;
        }
        for (int i = 0; i < classComposeMetadata.fields.size(); i++) {
            Field field = classComposeMetadata.fields.get(i);
            Class<?> type = field.getType();
            if (!Modifier.isPublic(field.getModifiers())) {
                field.setAccessible(true);
            }
            Object o = field.get(object);
            if(i >= classComposeMetadata.primitives) {
                byteBufOutputStream.writeBoolean(o != null);
            }
            if(o == null)
                continue;
            if(type == Integer.class || type == int.class) {
                byteBufOutputStream.writeInt((Integer) o);
                continue;
            } else if(type == Byte.class || type == byte.class) {
                byteBufOutputStream.writeByte((Byte) o);
                continue;
            } else if(type == Boolean.class || type == boolean.class) {
                byteBufOutputStream.writeBoolean((Boolean) o);
                continue;
            } else if(type == Long.class || type == long.class) {
                byteBufOutputStream.writeLong((Long) o);
                continue;
            } else if(type == Float.class || type == float.class) {
                byteBufOutputStream.writeFloat((Float) o);
                continue;
            } else if(type == Double.class || type == double.class) {
                byteBufOutputStream.writeDouble((Double) o);
                continue;
            } else if(type == Character.class || type == char.class) {
                byteBufOutputStream.writeChar((Character) o);
                continue;
            } else if(type == Short.class || type == short.class) {
                byteBufOutputStream.writeShort((Short) o);
                continue;
            } else if(type == String.class) {
                byteBufOutputStream.writeUTF((String) o);
                continue;
            } else if(Enum.class.isAssignableFrom(type)) {
                byteBufOutputStream.writeInt(((Enum<?>) o).ordinal());
                continue;
            } else if(Collection.class.isAssignableFrom(type)) {
                Collection<?> collection = (Collection<?>) o;
                byteBufOutputStream.writeInt(collection.size());
                boolean flag = false;
                for (Object o1 : collection) {
                    if(!Composable.class.isAssignableFrom(o1.getClass())) {
                        flag = true;
                        break;
                    }
                    writeObject((Composable) o1, byteBufOutputStream);
                }
                if(!flag)
                    continue;
            } else if(Map.class.isAssignableFrom(type)) {
                Map<?, ?> map = (Map<?, ?>) o;
                byteBufOutputStream.writeInt(map.size());
                boolean flag = false;
                for (Object o1 : map.keySet()) {
                    Object o2 = map.get(o1);
                    if(!Composable.class.isAssignableFrom(o1.getClass()) || !Composable.class.isAssignableFrom(o2.getClass())) {
                        flag = true;
                        break;
                    }
                    writeObject((Composable) o1, byteBufOutputStream);
                    writeObject((Composable) o2, byteBufOutputStream);
                }
                if(!flag)
                    continue;
            } else if(type.isArray()) {
                Object[] o1 = (Object[]) o;
                byteBufOutputStream.writeInt(o1.length);
                boolean flag = false;
                for (Object o2 : o1) {
                    if(!Composable.class.isAssignableFrom(o2.getClass())) {
                        flag = true;
                        break;
                    }
                    writeObject((Composable) o2, byteBufOutputStream);
                }
                if(!flag)
                    continue;
            } else if(Composable.class.isAssignableFrom(type)) {
                writeObject((Composable) o, byteBufOutputStream);
                continue;
            } else if(classComposeAdapterMap.containsKey(type)) {
                try {
                    //noinspection unchecked
                    classComposeAdapterMap.get(type).composition.compose(o, byteBufOutputStream);
                } catch (Exception e) {
                    throw new IOException(e);
                }
                continue;
            }
            throw new IOException(type.getName() + " isn't supported by the composition process!");
        }
    }

    private ClassComposeMetadata getClassComposeData(Class<?> aClass) throws NoSuchMethodException {
        String className = aClass.getName();
        ClassComposeMetadata classComposeMetadata = classComposeMetadataMap.get(className);
        if (classComposeMetadata != null) {
            return classComposeMetadata;
        }
        int primitives = 0;
        List<Field> list = new ArrayList<>();
        for (Field declaredField : aClass.getDeclaredFields()) {
            if(Modifier.isTransient(declaredField.getModifiers()) || Modifier.isStatic(declaredField.getModifiers()))
                continue;
            if(declaredField.getType().isPrimitive())
                primitives++;
            list.add(declaredField);
        }
        list.sort((a, b) -> {
            boolean a1 = a.getType().isPrimitive();
            boolean b1 = b.getType().isPrimitive();
            if (a1 == b1) return 0;
            else if (a1) return -1;
            else return 1;
        });
        boolean overrideDecompose = aClass.getMethod("decompose", ByteBufInputStream.class).getDeclaringClass() == aClass;
        boolean overrideCompose = aClass.getMethod("compose", ByteBufOutputStream.class).getDeclaringClass() == aClass;
        classComposeMetadata = new ClassComposeMetadata(list, primitives, overrideDecompose, overrideCompose);
        classComposeMetadataMap.put(className, classComposeMetadata);
        return classComposeMetadata;
    }

    public interface IComposition<T> {
        void compose(T t, ByteBufOutputStream byteBufOutputStream) throws IOException;
    }

    public interface IDecomposition<T> {
        T decompose(ByteBufInputStream byteBufInputStream) throws IOException;
    }

    private static class ComposeAdapter<T> {

        private final IComposition<T> composition;
        private final IDecomposition<T> decomposition;

        public ComposeAdapter(IComposition<T> composition, IDecomposition<T> decomposition) {
            this.composition = composition;
            this.decomposition = decomposition;
        }
    }

    private static class ClassComposeMetadata {

        private final List<Field> fields;
        private final int primitives;
        private final boolean overrideDecompose;
        private final boolean overrideCompose;

        private ClassComposeMetadata(List<Field> fields, int primitives, boolean overrideDecompose, boolean overrideCompose) {
            this.fields = fields;
            this.primitives = primitives;
            this.overrideDecompose = overrideDecompose;
            this.overrideCompose = overrideCompose;
        }
    }
}
