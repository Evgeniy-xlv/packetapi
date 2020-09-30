package ru.xlv.packetapi.capability;

import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import ru.xlv.flex.thr.ThrBiConsumer;
import ru.xlv.flex.thr.ThrConsumer;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.util.ByteBufInputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class CapabilityAdapter1_15_2 implements ICapabilityAdapter {
    @Override
    public void scheduleTaskSync(Runnable runnable) {
        Minecraft.getInstance().deferTask(runnable);
    }

    @Override
    public void scheduleServerTaskSync(Runnable runnable) {
        ServerLifecycleHooks.getCurrentServer().deferTask(runnable);
    }

    @Override
    public boolean isServerThread(Thread thread) {
        return ServerLifecycleHooks.getCurrentServer().getExecutionThread() == thread;
    }

    @Override
    public UUID getPlayerEntityUniqueId(Object playerEntity) {
        if (!(playerEntity instanceof PlayerEntity)) {
            throw new RuntimeException();
        }
        return ((PlayerEntity) playerEntity).getUniqueID();
    }

    @Override
    public String getPlayerEntityName(Object playerEntity) {
        if (!(playerEntity instanceof PlayerEntity)) {
            throw new RuntimeException();
        }
        return ((PlayerEntity) playerEntity).getName().getFormattedText();
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
            return Math.sqrt(((Entity) entity).getDistanceSq(x, y, z));
        }
        return 0;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass) {
        return (Stream<PLAYER>) Arrays.stream(ServerLifecycleHooks.getCurrentServer().getPlayerList().getOnlinePlayerNames())
                .map(s -> ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUsername(s));
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName, ThrConsumer<ByteBufInputStream> clientPacketReceived, ThrBiConsumer<PLAYER, ByteBufInputStream> serverPacketReceived) {
        return new AbstractNetworkAdapter<PLAYER>(channelName, clientPacketReceived, serverPacketReceived) {
            private static final String PROTOCOL_VERSION = "1";
            private final SimpleChannel channel;
            {
                this.channel = NetworkRegistry.ChannelBuilder
                        .named(new ResourceLocation(PacketAPI.getApiDefaultChannelName(), channelName))
                        .networkProtocolVersion(() -> PROTOCOL_VERSION)
                        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                        .simpleChannel();
                this.channel.registerMessage(0, MessageWrap.class, MessageWrap::write, (buf) -> new MessageWrap().read(buf), (msg, fun) -> handle(msg, fun.get()));
            }

            public void handle(MessageWrap messageWrap, NetworkEvent.Context context) {
                if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    ByteBufInputStream byteBufInputStream = messageWrap.getByteBufInputStream();
                    try {
                        onClientPacketReceived(byteBufInputStream);
                        byteBufInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ByteBufInputStream byteBufInputStream = messageWrap.getByteBufInputStream();
                    try {
                        onServerPacketReceived((PLAYER) context.getSender(), byteBufInputStream);
                        byteBufInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

//            @SuppressWarnings("unused")
//            @SubscribeEvent
//            public void onClientPacketReceived(NetworkEvent.ClientCustomPayloadEvent event) {
//                ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPayload());
//                try {
//                    onClientPacketReceived(byteBufInputStream);
//                    byteBufInputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @SuppressWarnings("unused")
//            @SubscribeEvent
//            public void onServerPacketReceived(NetworkEvent.ServerCustomPayloadEvent event) {
//                ByteBufInputStream byteBufInputStream = new ByteBufInputStream(event.getPayload());
//                try {
//                    onServerPacketReceived((PLAYER) event.getSource().get().getSender(), byteBufInputStream);
//                    byteBufInputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            @Override
            public void sendTo(PLAYER player, ByteBufOutputStream byteBufOutputStream) {
                channel.sendTo(new MessageWrap(byteBufOutputStream), ((ServerPlayerEntity) player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
            }

            @Override
            public void sendToServer(ByteBufOutputStream byteBufOutputStream) {
                channel.sendToServer(new MessageWrap(byteBufOutputStream));
            }
        };
    }

    public static class MessageWrap {

        private ByteBufOutputStream byteBufOutputStream;
        private ByteBufInputStream byteBufInputStream;

        public MessageWrap() {}

        public MessageWrap(ByteBufOutputStream byteBufOutputStream) {
            this.byteBufOutputStream = byteBufOutputStream;
        }

        public void write(PacketBuffer packetBuffer) {
            packetBuffer.writeBytes(byteBufOutputStream.buffer());
        }

        public MessageWrap read(PacketBuffer packetBuffer) {
            byteBufInputStream = new ByteBufInputStream(packetBuffer.getBuffer());
            return this;
        }

        public ByteBufInputStream getByteBufInputStream() {
            return byteBufInputStream;
        }
    }
}
