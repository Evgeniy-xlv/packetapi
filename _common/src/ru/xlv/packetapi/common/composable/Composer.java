package ru.xlv.packetapi.common.composable;

import com.google.common.primitives.Primitives;
import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * This tool provides the composition process of {@link Composable}.
 * <p>
 * The process is that we ignore data that both logical sides already know about.
 * For example, both sides have instances of classes, which means that there is no need to transfer
 * metadata of classes. Cutting off all unnecessary, Composer leaves only the paths to the classes and the
 * values of its fields. It is this data that is packed into the buffer.
 * <p>
 * Composer also slightly optimizes the amount of data for some types. So, for example, only its ordinal
 * remains from the enum.
 * <p>
 * By default, Composer uses processes similar to Java serialization with some optimizations.
 * However, you can make your objects very lightweight by using the {@link Lightweight} annotation.
 *
 * @see Composable
 * @see Lightweight
 * */
public class Composer {

    private final Map<String, TypeComposeMetadata> typeComposeMetadataMap = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, ComposeAdapter> typeComposeAdapterMap = new HashMap<>();

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
     * @throws NullPointerException if one of the arguments is null.
     * */
    public <T> void registerComposeAdapter(Class<T> type, IComposition<T> composition, IDecomposition<T> decomposition) {
        registerComposeAdapter(type, new ComposeAdapter<>(composition, decomposition));
    }

    /**
     * Registers new {@link ComposeAdapter}.
     * @param type is the type whose composition process you want to implement for the Composable system.
     * @throws UnsupportedOperationException if the target type is Composable; or if the target type is primitive; or if {@link ComposeAdapter} for the target type is already registered.
     * @throws NullPointerException if one of the arguments is null.
     * */
    public <T> void registerComposeAdapter(Class<T> type, ComposeAdapter<T> composeAdapter) {
        if(type == null || composeAdapter == null)
            throw new NullPointerException();
        if(type.isPrimitive() || Primitives.isWrapperType(type) || (type.isArray() && (type.getComponentType().isPrimitive() || Primitives.isWrapperType(type.getComponentType()))))
            throw new UnsupportedOperationException("ComposeAdapter isn't designed to override the composition process for primitive types.");
        if(Composable.class.isAssignableFrom(type))
            throw new UnsupportedOperationException("ComposeAdapter is designed to extend the list of data types supported by the Composable system. To change the composition process for Composable, override the corresponding method inside the Composable class.");
        if (typeComposeAdapterMap.containsKey(type))
            throw new UnsupportedOperationException("ComposeAdapter for this type is already registered!");
        typeComposeAdapterMap.put(type, composeAdapter);
    }

    /**
     * Packs {@link Composable} to the buffer.
     * @see Composer#writeComposable(Composable, ByteBufOutputStream, boolean, LightweightDeep)
     * @see Composer#getClassComposeData(Class)
     * @throws IOException if any exception was thrown during the composition process.
     * */
    public <T extends Composable> void compose(T composable, ByteBufOutputStream byteBufOutputStream) throws IOException {
        if(composable == null) throw new IOException("Composable is null!");
        try {
            writeComposable(composable, byteBufOutputStream, false, null);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new IOException(e);
        }
    }

    private void writeComposable(Composable object, ByteBufOutputStream byteBufOutputStream, boolean isRecursiveCall, LightweightDeep lightweightDeep) throws IOException, IllegalAccessException, NoSuchMethodException {
        if(!isRecursiveCall || !lightweightDeep.isLightweight())
            byteBufOutputStream.writeUTF(object.getClass().getName());
        TypeComposeMetadata typeComposeMetadata = getClassComposeData(object.getClass());
        if(typeComposeMetadata.overrideCompose) {
            object.compose(byteBufOutputStream);
            return;
        }
        Lightweight classLightweight = object.getClass().getAnnotation(Lightweight.class);
        for (int i = 0; i < typeComposeMetadata.fields.size(); i++) {
            Field field = typeComposeMetadata.fields.get(i);
            if (!Modifier.isPublic(field.getModifiers()))
                field.setAccessible(true);
            Object o = field.get(object);
            if(i >= typeComposeMetadata.primitives)
                byteBufOutputStream.writeBoolean(o != null);
            if(o == null)
                continue;
            if(isRecursiveCall) {
                lightweightDeep = lightweightDeep.next();
            } else if(!field.isAnnotationPresent(Lightweight.Exclude.class) && (classLightweight != null || field.isAnnotationPresent(Lightweight.class))) {
                Lightweight fieldAnnotation = field.getAnnotation(Lightweight.class);
                int fieldDeep = fieldAnnotation != null ? fieldAnnotation.deep() : 0;
                lightweightDeep = new LightweightDeep(classLightweight != null ? Math.max(classLightweight.deep(), fieldDeep) : fieldDeep, false);
            } else {
                lightweightDeep = new LightweightDeep(0, false);
            }
            try {
                writeObject(o, byteBufOutputStream, lightweightDeep);
            } catch (IOException e) {
                String s = o.getClass().getName();
                if(field.getGenericType() instanceof ParameterizedType && ((ParameterizedType) field.getGenericType()).getActualTypeArguments().length > 0)
                    s += Arrays.toString(((ParameterizedType) field.getGenericType()).getActualTypeArguments());
                throw new IOException(s + " isn't supported by the composition process!");
            }
        }
    }

    private void writeObject(Object object, ByteBufOutputStream byteBufOutputStream, LightweightDeep lightweightDeep) throws IOException, NoSuchMethodException, IllegalAccessException {
        Class<?> type = object.getClass();
        if(type == Integer.class)
            byteBufOutputStream.writeInt((Integer) object);
        else if(type == Byte.class)
            byteBufOutputStream.writeByte((Byte) object);
        else if(type == Boolean.class)
            byteBufOutputStream.writeBoolean((Boolean) object);
        else if(type == Long.class)
            byteBufOutputStream.writeLong((Long) object);
        else if(type == Float.class)
            byteBufOutputStream.writeFloat((Float) object);
        else if(type == Double.class)
            byteBufOutputStream.writeDouble((Double) object);
        else if(type == Character.class)
            byteBufOutputStream.writeChar((Character) object);
        else if(type == Short.class)
            byteBufOutputStream.writeShort((Short) object);
        else if(type == String.class)
            byteBufOutputStream.writeUTF((String) object);
        else if(type.isArray()) {
            writeArray(type.getComponentType(), object, byteBufOutputStream, lightweightDeep);
        } else if(Composable.class.isAssignableFrom(type)) {
            writeComposable((Composable) object, byteBufOutputStream, true, lightweightDeep);
        } else if(typeComposeAdapterMap.containsKey(type)) {
            try {
                //noinspection unchecked
                typeComposeAdapterMap.get(type).getComposition().compose(object, byteBufOutputStream);
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else if(Enum.class.isAssignableFrom(type)) {
            byteBufOutputStream.writeInt(((Enum<?>) object).ordinal());
        } else if(Collection.class.isAssignableFrom(type)) {
            writeCollection(object, byteBufOutputStream, lightweightDeep);
        } else if(Map.class.isAssignableFrom(type)) {
            writeMap(object, byteBufOutputStream, lightweightDeep);
        }
    }

    private void writeCollection(Object object, ByteBufOutputStream byteBufOutputStream, LightweightDeep lightweightDeep) throws NoSuchMethodException, IllegalAccessException, IOException {
        Collection<?> collection = (Collection<?>) object;
        byteBufOutputStream.writeInt(collection.size());
        for (Object element : collection) {
            byteBufOutputStream.writeBoolean(element != null);
            if(element != null) {
                if(!lightweightDeep.isLightweight())
                    byteBufOutputStream.writeUTF(element.getClass().getName());
                writeObject(element, byteBufOutputStream, lightweightDeep.next());
            }
        }
    }

    private void writeMap(Object object, ByteBufOutputStream byteBufOutputStream, LightweightDeep lightweightDeep) throws IOException, NoSuchMethodException, IllegalAccessException {
        Map<?, ?> map = (Map<?, ?>) object;
        byteBufOutputStream.writeInt(map.size());
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if ((key != null && isUnsupportedType(key.getClass())))
                throw new IOException(key + " isn't supported by the composition process!");
            if ((value != null && isUnsupportedType(value.getClass())))
                throw new IOException(value + " isn't supported by the composition process!");
            byteBufOutputStream.writeByte(key != null && value != null ? 0 : key == null && value == null ? 1 : key == null ? 2 : 3);
            if (key != null) {
                if(!lightweightDeep.isLightweight())
                    byteBufOutputStream.writeUTF(key.getClass().getName());
                writeObject(key, byteBufOutputStream, lightweightDeep.next());
            }
            if (value != null) {
                if(!lightweightDeep.isLightweight())
                    byteBufOutputStream.writeUTF(value.getClass().getName());
                writeObject(value, byteBufOutputStream, lightweightDeep.next());
            }
        }
    }

    private void writeArray(Class<?> componentType, Object object, ByteBufOutputStream byteBufOutputStream, LightweightDeep lightweightDeep) throws IOException, NoSuchMethodException, IllegalAccessException {
        if(componentType == boolean.class) {
            boolean[] arr = (boolean[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (boolean element : arr) {
                byteBufOutputStream.writeBoolean(element);
            }
        } else if(componentType == byte.class) {
            byte[] arr = (byte[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (byte element : arr) {
                byteBufOutputStream.writeByte(element);
            }
        } else if(componentType == short.class) {
            short[] arr = (short[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (short element : arr) {
                byteBufOutputStream.writeShort(element);
            }
        } else if(componentType == int.class) {
            int[] arr = (int[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (int element : arr) {
                byteBufOutputStream.writeInt(element);
            }
        } else if(componentType == float.class) {
            float[] arr = (float[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (float element : arr) {
                byteBufOutputStream.writeFloat(element);
            }
        } else if(componentType == double.class) {
            double[] arr = (double[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (double element : arr) {
                byteBufOutputStream.writeDouble(element);
            }
        } else if(componentType == long.class) {
            long[] arr = (long[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (long element : arr) {
                byteBufOutputStream.writeLong(element);
            }
        } else if(componentType == char.class) {
            char[] arr = (char[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (char element : arr) {
                byteBufOutputStream.writeChar(element);
            }
        } else if(componentType == String.class) {
            String[] arr = (String[]) object;
            byteBufOutputStream.writeInt(arr.length);
            for (String element : arr) {
                byteBufOutputStream.writeUTF(element);
            }
        } else {
            Object[] o1 = (Object[]) object;
            byteBufOutputStream.writeInt(o1.length);
            for (Object element : o1) {
                if(!componentType.isPrimitive())
                    byteBufOutputStream.writeBoolean(element != null);
                if(element != null) {
                    if (isUnsupportedType(element.getClass()))
                        throw new IOException(element + " isn't supported by the composition process!");
                    writeObject(element, byteBufOutputStream, lightweightDeep);
                }
            }
        }
    }

    /**
     * Unpacks {@link Composable} from the buffer.
     * @see Composer#readComposable(Class, ByteBufInputStream, boolean, LightweightDeep)
     * @see Composer#getClassComposeData(Class)
     * @throws IOException if any exception was thrown during the decomposition process.
     * */
    @Nonnull
    public Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        if (unsafe == null)
            throw new IOException("Unexpected error");
        try {
            return (Composable) readComposable(null, byteBufInputStream, false, null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IOException(e);
        }
    }

    @Nonnull
    private Object readComposable(Class<?> aClass, ByteBufInputStream byteBufInputStream, boolean isRecursiveCall, LightweightDeep lightweightDeep) throws IOException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(aClass == null || !lightweightDeep.isLightweight()) {
            String className = byteBufInputStream.readUTF();
            aClass = Class.forName(className);
        }
        if(!Composable.class.isAssignableFrom(aClass))
            throw new IOException(aClass.getName() + " isn't Composable!");
        Class<?> c = aClass;
        while(Serializable.class.isAssignableFrom(c)) {
            c = c.getSuperclass();
        }
        Constructor<?> constructor = c.getConstructor((Class<?>[]) null);
        constructor = reflectionFactory.newConstructorForSerialization(aClass, constructor);
        Object object = constructor.newInstance();
        TypeComposeMetadata typeComposeMetadata = getClassComposeData(aClass);
        if(typeComposeMetadata.overrideDecompose) {
            Composable decompose = ((Composable) object).decompose(byteBufInputStream);
            //noinspection ConstantConditions
            if (decompose == null)
                throw new IOException(aClass.getName() + " overrides the decomposition process, but returns null!");
            return decompose;
        }
        Lightweight classLightweight = object.getClass().getAnnotation(Lightweight.class);
        int primitives = typeComposeMetadata.primitives;
        for (int i = 0; i < typeComposeMetadata.fields.size(); i++) {
            Field field = typeComposeMetadata.fields.get(i);
            Class<?> type = field.getType();
            if(primitives > 0 && type.isPrimitive())
                primitives--;
            boolean skip = !type.isPrimitive() && !byteBufInputStream.readBoolean();
            if(skip)
                continue;
            if(isRecursiveCall) {
                lightweightDeep = lightweightDeep.next();
            } else if(!field.isAnnotationPresent(Lightweight.Exclude.class) && (classLightweight != null || field.isAnnotationPresent(Lightweight.class))) {
                Lightweight fieldAnnotation = field.getAnnotation(Lightweight.class);
                int fieldDeep = fieldAnnotation != null ? fieldAnnotation.deep() : 0;
                lightweightDeep = new LightweightDeep(classLightweight != null ? Math.max(classLightweight.deep(), fieldDeep) : fieldDeep, false);
            } else {
                lightweightDeep = new LightweightDeep(0, false);
            }
            long objectFieldOffset = unsafe.objectFieldOffset(field);
            if(type == Integer.class || type == int.class)
                unsafe.putInt(object, objectFieldOffset, byteBufInputStream.readInt());
            else if(type == Byte.class || type == byte.class)
                unsafe.putByte(object, objectFieldOffset, byteBufInputStream.readByte());
            else if(type == Boolean.class || type == boolean.class)
                unsafe.putBoolean(object, objectFieldOffset, byteBufInputStream.readBoolean());
            else if(type == Long.class || type == long.class)
                unsafe.putLong(object, objectFieldOffset, byteBufInputStream.readLong());
            else if(type == Float.class || type == float.class)
                unsafe.putFloat(object, objectFieldOffset, byteBufInputStream.readFloat());
            else if(type == Double.class || type == double.class)
                unsafe.putDouble(object, objectFieldOffset, byteBufInputStream.readDouble());
            else if(type == Character.class || type == char.class)
                unsafe.putChar(object, objectFieldOffset, byteBufInputStream.readChar());
            else if(type == Short.class || type == short.class)
                unsafe.putShort(object, objectFieldOffset, byteBufInputStream.readShort());
            else {
                Object value = null;
                if(type == String.class) {
                    value = byteBufInputStream.readUTF();
                } else if(type.isArray()) {
                    value = readArray(type.getComponentType(), byteBufInputStream, true, lightweightDeep);
                } else if(Composable.class.isAssignableFrom(type)) {
                    value = readComposable(type, byteBufInputStream, true, lightweightDeep);
                } else if(typeComposeAdapterMap.containsKey(type)) {
                    try {
                        value = typeComposeAdapterMap.get(type).getDecomposition().decompose(byteBufInputStream);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                } else if(Enum.class.isAssignableFrom(type)) {
                    Object[] values = (Object[]) type.getDeclaredMethod("values").invoke(null);
                    int ordinal = byteBufInputStream.readInt();
                    value = values[ordinal];
                } else if(Collection.class.isAssignableFrom(type)) {
                    value = readCollection(type, ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], byteBufInputStream, lightweightDeep);
                } else if(Map.class.isAssignableFrom(type)) {
                    value = readMap(type, ((ParameterizedType) field.getGenericType()).getActualTypeArguments(), byteBufInputStream, lightweightDeep);
                }
                unsafe.putObject(object, objectFieldOffset, value);
            }
        }
        return object;
    }

    private Object readObject(Class<?> aClass, Type[] types, ByteBufInputStream byteBufInputStream, boolean isCalledFromStart, LightweightDeep lightweightDeep) throws NoSuchMethodException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        if(aClass == Integer.class || aClass == int.class)
            return byteBufInputStream.readInt();
        else if(aClass == Byte.class || aClass == byte.class)
            return byteBufInputStream.readByte();
        else if(aClass == Boolean.class || aClass == boolean.class)
            return byteBufInputStream.readBoolean();
        else if(aClass == Long.class || aClass == long.class)
            return byteBufInputStream.readLong();
        else if(aClass == Float.class || aClass == float.class)
            return byteBufInputStream.readFloat();
        else if(aClass == Double.class || aClass == double.class)
            return byteBufInputStream.readDouble();
        else if(aClass == Character.class || aClass == char.class)
            return byteBufInputStream.readChar();
        else if(aClass == Short.class || aClass == short.class)
            return byteBufInputStream.readShort();
        else if(aClass == String.class)
            return byteBufInputStream.readUTF();
        else {
            Object value = null;
            if(aClass.isArray()) {
                value = readArray(aClass.getComponentType(), byteBufInputStream, false, lightweightDeep.next());
            } else if(Composable.class.isAssignableFrom(aClass)) {
                if(!isCalledFromStart)
                    lightweightDeep = lightweightDeep.next();
                value = readComposable(aClass, byteBufInputStream, true, lightweightDeep);
            } else if(typeComposeAdapterMap.containsKey(aClass)) {
                try {
                    value = typeComposeAdapterMap.get(aClass).getDecomposition().decompose(byteBufInputStream);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else if(Enum.class.isAssignableFrom(aClass)) {
                Object[] values = (Object[]) aClass.getDeclaredMethod("values").invoke(null);
                int ordinal = byteBufInputStream.readInt();
                value = values[ordinal];
            } else if(Collection.class.isAssignableFrom(aClass)) {
                value = readCollection(aClass, types != null ? types[0] : null, byteBufInputStream, lightweightDeep.next());
            } else if(Map.class.isAssignableFrom(aClass)) {
                value = readMap(aClass, types, byteBufInputStream, lightweightDeep.next());
            }
            if(value == null)
                throw new IOException("Unexpected error. Failed to read an object!");
            return value;
        }
    }

    private Collection<Object> readCollection(Class<?> aClass, Type parameterType, ByteBufInputStream byteBufInputStream, LightweightDeep lightweightDeep) throws NoSuchMethodException, IOException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Collection<Object> collection;
        if(aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())) {
            collection = new ArrayList<>();
        } else {
            //noinspection unchecked
            collection = (Collection<Object>) aClass.newInstance();
        }
        int i1 = byteBufInputStream.readInt();
        if(lightweightDeep.isLightweight()) {
            Class<?> aClass1;
            Type[] types;
            if (parameterType instanceof ParameterizedType) {
                aClass1 = (Class<?>) ((ParameterizedType) parameterType).getRawType();
                types = ((ParameterizedType) parameterType).getActualTypeArguments();
            } else {
                aClass1 = (Class<?>) parameterType;
                types = null;
            }
            for (int j = 0; j < i1; j++) {
                if (byteBufInputStream.readBoolean()) {
                    Object o = readObject(aClass1, types, byteBufInputStream, false, lightweightDeep);
                    collection.add(o);
                }
            }
        } else {
            for (int j = 0; j < i1; j++) {
                if (byteBufInputStream.readBoolean()) {
                    Class<?> aClass2 = Class.forName(byteBufInputStream.readUTF());
                    Object o = readObject(aClass2, null, byteBufInputStream, false, lightweightDeep);
                    collection.add(o);
                }
            }
        }
        return collection;
    }

    private Map<Object, Object> readMap(Class<?> aClass, Type[] parameterTypes, ByteBufInputStream byteBufInputStream, LightweightDeep lightweightDeep) throws IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        Map<Object, Object> map;
        if(aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())) {
            map = new HashMap<>();
        } else {
            //noinspection unchecked
            map = (Map<Object, Object>) aClass.newInstance();
        }
        int size = byteBufInputStream.readInt();
        if(lightweightDeep.isLightweight()) {
            Class<?>[] classes = new Class[2];
            Type[][] types = new Type[2][2];
            for (int i = 0; i < 2; i++) {
                if (parameterTypes[i] instanceof ParameterizedType) {
                    classes[i] = (Class<?>) ((ParameterizedType) parameterTypes[i]).getRawType();
                    types[i] = ((ParameterizedType) parameterTypes[i]).getActualTypeArguments();
                } else {
                    classes[i] = (Class<?>) parameterTypes[i];
                    types[i] = null;
                }
            }
            for (int j = 0; j < size; j++) {
                Object o = null;
                Object o1 = null;
                byte b = byteBufInputStream.readByte();
                boolean hasKey = b == 0 || b == 3;
                boolean hasValue = b == 0 || b == 2;
                if (hasKey) {
                    o = readObject(classes[0], types[0], byteBufInputStream, false, lightweightDeep);
                }
                if (hasValue) {
                    o1 = readObject(classes[1], types[1], byteBufInputStream, false, lightweightDeep);
                }
                map.put(o, o1);
            }
        } else {
            for (int j = 0; j < size; j++) {
                Object o = null;
                Object o1 = null;
                byte b = byteBufInputStream.readByte();
                boolean hasKey = b == 0 || b == 3;
                boolean hasValue = b == 0 || b == 2;
                if (hasKey) {
                    Class<?> aClass1 = Class.forName(byteBufInputStream.readUTF());
                    o = readObject(aClass1, null, byteBufInputStream, false, lightweightDeep);
                }
                if (hasValue) {
                    Class<?> aClass1 = Class.forName(byteBufInputStream.readUTF());
                    o1 = readObject(aClass1, null, byteBufInputStream, false, lightweightDeep);
                }
                map.put(o, o1);
            }
        }
        return map;
    }

    private Object readArray(Class<?> componentType, ByteBufInputStream byteBufInputStream, boolean isCalledFromStart, LightweightDeep lightweightDeep) throws IOException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        int length = byteBufInputStream.readInt();
        if (componentType == boolean.class) {
            boolean[] arr = new boolean[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readBoolean();
            }
            return arr;
        } else if(componentType == byte.class) {
            byte[] arr = new byte[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readByte();
            }
            return arr;
        } else if(componentType == short.class) {
            short[] arr = new short[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readShort();
            }
            return arr;
        } else if(componentType == int.class) {
            int[] arr = new int[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readInt();
            }
            return arr;
        } else if(componentType == float.class) {
            float[] arr = new float[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readFloat();
            }
            return arr;
        } else if(componentType == double.class) {
            double[] arr = new double[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readDouble();
            }
            return arr;
        } else if(componentType == long.class) {
            long[] arr = new long[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readLong();
            }
            return arr;
        } else if(componentType == char.class) {
            char[] arr = new char[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readChar();
            }
            return arr;
        } else if(componentType == String.class) {
            String[] arr = new String[length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = byteBufInputStream.readUTF();
            }
            return arr;
        } else {
            Object[] arr = (Object[]) Array.newInstance(componentType, length);
            for (int i = 0; i < arr.length; i++) {
                boolean notNull = byteBufInputStream.readBoolean();
                arr[i] = notNull ? readObject(componentType, null, byteBufInputStream, isCalledFromStart, lightweightDeep) : null;
            }
            return arr;
        }
    }

    private boolean isUnsupportedType(Class<?> aClass) {
        return !aClass.isPrimitive() && !Primitives.isWrapperType(aClass) && !aClass.isArray() && aClass != String.class && !Enum.class.isAssignableFrom(aClass)
                && !Collection.class.isAssignableFrom(aClass) && !Map.class.isAssignableFrom(aClass) && !Composable.class.isAssignableFrom(aClass);
    }

    /**
     * Forms and gives local meta information about the content of a particular class. This is necessary to optimize
     * the packing and unpacking processes of {@link Composable}.
     * @see TypeComposeMetadata
     * */
    @Nonnull
    private TypeComposeMetadata getClassComposeData(Class<?> aClass) throws NoSuchMethodException {
        String className = aClass.getName();
        TypeComposeMetadata typeComposeMetadata = typeComposeMetadataMap.get(className);
        if (typeComposeMetadata != null)
            return typeComposeMetadata;
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
            return a1 == b1 ? 0 : a1 ? -1 : 1;
        });
        boolean overrideDecompose = aClass.getMethod("decompose", ByteBufInputStream.class).getDeclaringClass() == aClass;
        boolean overrideCompose = aClass.getMethod("compose", ByteBufOutputStream.class).getDeclaringClass() == aClass;
        typeComposeMetadata = new TypeComposeMetadata(list, primitives, overrideDecompose, overrideCompose);
        typeComposeMetadataMap.put(className, typeComposeMetadata);
        return typeComposeMetadata;
    }

    private static class TypeComposeMetadata {

        /**
         * An ordered list of class fields. Primitives are brought to the top, and objects are brought down.
         * This approach allows to differentiate non-null objects from nullable objects.
         * All Nullable objects will be additionally signed with a single boolean prefix.
         * This boolean will signal if the further object is null or not.
         * This solves the problem of writing nulls to the buffer.
         * */
        private final List<Field> fields;
        /**
         * The number of fields of the primitive class types.
         * */
        private final int primitives;
        /**
         * true if the class overrides {@link Composable#decompose(ByteBufInputStream)} method.
         * */
        private final boolean overrideDecompose;
        /**
         * true if the class overrides {@link Composable#compose(ByteBufOutputStream)} method.
         * */
        private final boolean overrideCompose;

        private TypeComposeMetadata(List<Field> fields, int primitives, boolean overrideDecompose, boolean overrideCompose) {
            this.fields = fields;
            this.primitives = primitives;
            this.overrideDecompose = overrideDecompose;
            this.overrideCompose = overrideCompose;
        }
    }
}
