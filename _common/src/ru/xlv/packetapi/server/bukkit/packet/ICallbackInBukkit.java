package ru.xlv.packetapi.server.bukkit.packet;

import io.netty.buffer.ByteBufOutputStream;
import org.bukkit.entity.Player;
import ru.xlv.packetapi.common.packet.ICallback;
import ru.xlv.packetapi.common.util.ByteBufInputStream;
import ru.xlv.packetapi.server.forge.packet.PacketCallbackSender;

import java.io.IOException;

/**
 * A special packet for processing requests on the server side.
 * <p>
 * Use only in conjunction with {@link ICallback} and its childes, as these packets are additionally signing!
 *
 * Usage example:
 * <pre>
 * public class MyCallbackOnServer implements ICallbackInBukkit {
 *
 *    private int inputValue;
 *
 *    public void read(Player player, ByteBufInputStream bbis,
 *                      PacketCallbackSender packetCallbackSender) throws IOException {
 *      inputValue = bbis.readInt();
 *    }
 *
 *    public void write(Player player, ByteBufOutputStream bbos) throws IOException {
 *      boolean success = false;
 *      if(inputValue % 2 == 0) {
 *          success = true;
 *      }
 *      bbos.writeBoolean(success);
 *      bbos.writeUTF("Wow! Your number can be divisible by 2 without a remainder. Congratulations!");
 *    }
 * }
 * <pre>
 * */
public interface ICallbackInBukkit extends ICallback {

    /**
     * Here you should read the data and process them.
     * @param packetCallbackSender use if {@link ICallbackInBukkit#handleCallback()} == true, otherwise two packets will be sent.
     * */
    void read(Player player, ByteBufInputStream bbis, PacketCallbackSender packetCallbackSender) throws IOException;

    /**
     * Here you should construct the response and write it to the buffer. Will be called only if no errors occurred
     * while reading the request and the {@link PacketCallbackSender#send()} method was called in the
     * {@link ICallbackInBukkit#read(Player, ByteBufInputStream, PacketCallbackSender)} method for the PacketCallbackSender.
     * <p>
     * This can happen only in two cases:
     * <p>
     *  1. Control was left to the api and the write method was called immediately after read.
     * <p>
     *  2. Control was taken over by a programmer who manually called the {@link PacketCallbackSender#send()} method in the read method.
     * */
    void write(Player player, ByteBufOutputStream bbos) throws IOException;

    /**
     * Return true to take control of the dispatch of the callback.
     * */
    default boolean handleCallback() {
        return false;
    }
}