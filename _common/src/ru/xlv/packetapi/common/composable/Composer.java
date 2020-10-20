package ru.xlv.packetapi.common.composable;

import io.netty.buffer.ByteBufOutputStream;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Composer {

    public <T extends Composable> void compose(T composable, ByteBufOutputStream byteBufOutputStream) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteBufOutputStream);
        objectOutputStream.writeObject(composable);
        objectOutputStream.close();
    }

    @Nonnull
    public Composable decompose(ByteBufInputStream byteBufInputStream) throws IOException {
        ObjectInputStream objectInputStream = new ObjectInputStream(byteBufInputStream);
        try {
            Object o = objectInputStream.readObject();
            if (o instanceof Composable) {
                return (Composable) o;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        throw new IOException("Unexpected exception.");
    }
}
