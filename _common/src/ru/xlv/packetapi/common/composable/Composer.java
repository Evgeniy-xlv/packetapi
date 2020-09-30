package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Composer {

    public <T extends Composable> void compose(T packetSerializable, ByteBufOutputStream byteBufOutputStream) throws IOException {
        byteBufOutputStream.writeUTF(packetSerializable.getClass().getName());
        try {
            if (packetSerializable.getClass().getMethod("serialize", ByteBufOutputStream.class).getDeclaringClass() == packetSerializable.getClass()) {
                packetSerializable.serialize(byteBufOutputStream);
                return;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteBufOutputStream);
        objectOutputStream.writeObject(packetSerializable);
        objectOutputStream.close();
    }

    @Nonnull
    public Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        try {
            String classpath = byteBufInputStream.readUTF();
            Class<?> aClass = Class.forName(classpath);
            if(Composable.class.isAssignableFrom(aClass)) {
                ObjectInputStream objectInputStream = new ObjectInputStream(byteBufInputStream);
                Composable composable = (Composable) objectInputStream.readObject();
                objectInputStream.close();
                if (aClass.getMethod("deserialize", ByteBufInputStream.class).getDeclaringClass() == aClass) {
                    composable.deserialize(byteBufInputStream);
                }
                return composable;
            }
        } catch (NoSuchMethodException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        throw new IOException();
    }
}
