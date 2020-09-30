package ru.xlv.packetapi.common.packet;

/**
 * This packet differs from all packets in that it is additionally signing and entered into the cache until it waits
 * for a response from the server signed by the same pid, or receives a timeout.
 * <p>
 * It is important to understand that this packet can only be sent from client to server and not otherwise. The server packet must also inherit this interface.
 * */
public interface ICallback extends IPacket {
}
