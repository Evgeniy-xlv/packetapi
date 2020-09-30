package ru.xlv.packetapi.capability;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@Mod(
        modid = PacketAPI.NAME,
        name = PacketAPI.NAME,
        version = PacketAPI.VERSION
)
public class CapabilityAdapter1_7_10 implements ICapabilityAdapter {

    @Mod.EventHandler
    public void event(FMLPreInitializationEvent event) {
        preConstruct(event.getSide() == Side.SERVER,
                Loader.instance().getActiveModList()
                        .stream()
                        .filter(modContainer -> Objects.nonNull(modContainer.getMod()))
                        .map(modContainer -> modContainer.getMod().getClass())
        );
    }

    private Queue<Runnable> RUNNABLE_QUEUE;

    @SubscribeEvent
    public void event(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.END) {
            while(!RUNNABLE_QUEUE.isEmpty()) {
                RUNNABLE_QUEUE.poll().run();
            }
        }
    }

    @Override
    public void scheduleTaskSync(Runnable runnable) {
        Minecraft.getMinecraft().func_152344_a(runnable);
    }

    @Override
    public void scheduleServerTaskSync(Runnable runnable) {
        if(RUNNABLE_QUEUE == null) {
            RUNNABLE_QUEUE = new LinkedList<>();
            FMLCommonHandler.instance().bus().register(this);
        }
        //noinspection SynchronizeOnNonFinalField
        synchronized (RUNNABLE_QUEUE) {
            RUNNABLE_QUEUE.add(runnable);
        }
    }

    @Override
    public boolean isServerThread(Thread thread) {
        return false;
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
        return ((EntityPlayer) playerEntity).getCommandSenderName();
    }

    @Override
    public double getDistanceBetween(Object entity, Object entity1) {
        if(entity instanceof Entity && entity1 instanceof Entity) {
            return ((Entity) entity).getDistanceToEntity((Entity) entity1);
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
        if(playerEntity instanceof EntityPlayer) {
            return ((EntityPlayer) playerEntity).dimension;
        }
        return 0;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass) {
        return (Stream<PLAYER>) FMLCommonHandler.instance().getMinecraftServerInstance().getConfigurationManager().playerEntityList.stream();
    }

    @Override
    public <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        return new NetworkAdapter<>(channelName, clientPacketReceived, serverPacketReceived);
    }

    @SuppressWarnings({"unchecked"})
    public static class NetworkAdapter<PLAYER> extends AbstractNetworkAdapter<PLAYER> {

        private final FMLEventChannel channel;

        public NetworkAdapter(String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
            super(channelName, clientPacketReceived, serverPacketReceived);
            this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
            this.channel.register(this);
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public void onClientPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event) {
            if (event.packet.channel().equals(channelName)) {
                try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.packet.payload())) {
                    onClientPacketReceived(byteBufInputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressWarnings("unused")
        @SubscribeEvent
        public void onServerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event) {
            if (event.packet.channel().equals(channelName)) {
                try (ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.packet.payload())) {
                    onServerPacketReceived((PLAYER) ((NetHandlerPlayServer) event.handler).playerEntity, byteBufInputStream);
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
    }
}
