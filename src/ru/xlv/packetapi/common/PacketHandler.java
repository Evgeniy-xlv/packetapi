package ru.xlv.packetapi.common;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import ru.xlv.packetapi.common.packet.IPacketOut;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.util.logging.Logger;

@Getter
public abstract class PacketHandler implements IPacketHandler {

    protected final Logger logger;

    private final FMLEventChannel channel;

    private final PacketRegistry packetRegistry;

    private final String channelName;

    public PacketHandler(@Nonnull PacketRegistry packetRegistry, @Nonnull String channelName) {
        this.packetRegistry = packetRegistry;
        this.channelName = channelName;
        this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        this.channel.register(this);
        MinecraftForge.EVENT_BUS.register(this);
        this.logger = Logger.getLogger(this.getClass().getSimpleName() + ":" + channelName);
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    @SubscribeEvent
    public void onClientPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event) {
        if(event.getPacket().channel().equals(getChannelName())) {
            ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPacket().payload());
            onClientPacketReceived(byteBufInputStream);
            byteBufInputStream.close();
        }
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    @SubscribeEvent
    public void onServerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event) {
        if(event.getPacket().channel().equals(getChannelName())) {
            ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPacket().payload());
            onServerPacketReceived(((NetHandlerPlayServer) event.getHandler()).player, byteBufInputStream);
            byteBufInputStream.close();
        }
    }

    protected void onClientPacketReceived(ByteBufInputStream bbis) throws IOException {}

    protected void onServerPacketReceived(EntityPlayerMP entityPlayer, ByteBufInputStream bbis) throws IOException {}

    @SneakyThrows
    public void sendPacketToServer(@Nonnull IPacketOut packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        byteBufOutputStream.writeInt(getPacketId(packet));
        packet.write(byteBufOutputStream);
        sendPacketToServer(byteBufOutputStream);
    }

    public void sendPacketToServer(@Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        FMLProxyPacket proxyPacket = new FMLProxyPacket(new PacketBuffer(bbos.buffer()), getChannelName());
        getChannel().sendToServer(proxyPacket);
        bbos.close();
    }

    @SneakyThrows
    public void sendPacketToPlayer(@Nullable EntityPlayer entityPlayer, @Nonnull IPacketOut packet) {
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
        byteBufOutputStream.writeInt(getPacketId(packet));
        packet.write(byteBufOutputStream);
        sendPacketToPlayer(entityPlayer, byteBufOutputStream);
    }

    public void sendPacketToPlayer(@Nullable EntityPlayer entityPlayer, @Nonnull @WillClose ByteBufOutputStream bbos) throws IOException {
        if(entityPlayer == null) {
            getLogger().warning("Unable to send a packet to null player!");
            return;
        }
        if(!(entityPlayer instanceof EntityPlayerMP)) {
            getLogger().warning("Unable to send a packet! The player must be an EntityPlayerMP!");
            return;
        }
        FMLProxyPacket proxyPacket = new FMLProxyPacket(new PacketBuffer(bbos.buffer()), getChannelName());
        getChannel().sendTo(proxyPacket, (EntityPlayerMP) entityPlayer);
        bbos.close();
    }
}
