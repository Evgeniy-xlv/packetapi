package ru.xlv.packetapi.capability;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Mod(
        modid = PacketAPI.NAME,
        name = PacketAPI.NAME,
        version = PacketAPI.VERSION
)
public class CapabilityAdapter1_12_2 implements ICapabilityAdapter {

    @Mod.EventHandler
    public void event(FMLPreInitializationEvent event) {
        preConstruct(event.getSide() == Side.SERVER,
                Loader.instance().getActiveModList()
                        .stream()
                        .filter(modContainer -> Objects.nonNull(modContainer.getMod()))
                        .map(modContainer -> modContainer.getMod().getClass())
        );
    }

    @Override
    public void scheduleTaskSync(Runnable runnable) {
        Minecraft.getMinecraft().addScheduledTask(runnable);
    }

    @Override
    public void scheduleServerTaskSync(Runnable runnable) {
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(runnable);
    }

    @Override
    public boolean isServerThread(Thread thread) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer() ? Minecraft.getMinecraft().isCallingFromMinecraftThread() : FMLCommonHandler.instance().getMinecraftServerInstance().isCallingFromMinecraftThread();
    }

    @Override
    public UUID getPlayerEntityUniqueId(Object playerEntity) {
        if (!(playerEntity instanceof EntityPlayer)) {
            throw new RuntimeException();
        }
        return ((EntityPlayer) playerEntity).getUniqueID();
    }

    @Override
    public String getPlayerEntityName(Object playerEntity) {
        if (!(playerEntity instanceof EntityPlayer)) {
            throw new RuntimeException();
        }
        return ((EntityPlayer) playerEntity).getName();
    }

    @Override
    public double getDistanceBetween(Object entity, Object entity1) {
        if(entity instanceof Entity && entity1 instanceof Entity) {
            return ((Entity) entity).getDistance((Entity) entity1);
        }
        return 0;
    }

    @Override
    public double getDistanceBetween(Object entity, double x, double y, double z) {
        if(entity instanceof Entity) {
            return ((Entity) entity).getDistance(x, y, z);
        }
        return 0;
    }

    @Override
    public int getPlayerDimension(Object playerEntity) {
        if(playerEntity instanceof Entity) {
            return ((Entity) playerEntity).dimension;
        }
        return 0;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass) {
        return (Stream<PLAYER>) Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getOnlinePlayerNames())
                .map(s -> FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(s));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        return new AbstractNetworkAdapter<PLAYER>(channelName, clientPacketReceived, serverPacketReceived) {
            private final FMLEventChannel channel;
            {
                this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
                this.channel.register(this);
                MinecraftForge.EVENT_BUS.register(this);
            }

            @SuppressWarnings("unused")
            @SubscribeEvent
            public void onClientPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event) {
                if (event.getPacket().channel().equals(channelName)) {
                    try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPacket().payload())) {
                        onClientPacketReceived(byteBufInputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @SuppressWarnings("unused")
            @SubscribeEvent
            public void onServerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event) {
                if (event.getPacket().channel().equals(channelName)) {
                    try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPacket().payload())) {
                        onServerPacketReceived((PLAYER) ((NetHandlerPlayServer) event.getHandler()).player, byteBufInputStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void sendTo(PLAYER player, ByteBufOutputStream byteBufOutputStream) {
                FMLProxyPacket proxyPacket = new FMLProxyPacket(new PacketBuffer(byteBufOutputStream.buffer()), channelName);
                channel.sendTo(proxyPacket, (EntityPlayerMP) player);
            }

            @Override
            public void sendToServer(ByteBufOutputStream byteBufOutputStream) {
                FMLProxyPacket proxyPacket = new FMLProxyPacket(new PacketBuffer(byteBufOutputStream.buffer()), channelName);
                channel.sendToServer(proxyPacket);
            }
        };
    }
}
