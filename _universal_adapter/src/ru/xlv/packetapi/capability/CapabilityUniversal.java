package ru.xlv.packetapi.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import ru.xlv.packetapi.PacketAPI;

import java.util.*;
import java.util.stream.Stream;

@Mod(modid = PacketAPI.NAME)
@VersionCheckerImpl(CapabilityUniversal.VersionChecker.class)
public class CapabilityUniversal implements ICapabilityAdapter, TypeLocator {

    private Queue<Runnable> RUNNABLE_CLIENT_QUEUE, RUNNABLE_SERVER_QUEUE;

    private final Map<String, EntityPlayer> onlinePlayerMap = new HashMap<>();

    @Mod.EventHandler
    public void event(FMLPreInitializationEvent event) {
        preConstruct(event.getSide() == Side.SERVER,
                Loader.instance().getActiveModList()
                        .stream()
                        .filter(modContainer -> Objects.nonNull(modContainer.getMod()))
                        .map(modContainer -> modContainer.getMod().getClass())
        );
    }

    @SubscribeEvent
    public void event(PlayerEvent.PlayerLoggedInEvent event) {
        onlinePlayerMap.put(event.player.getName(), event.player);
    }

    @SubscribeEvent
    public void event(PlayerEvent.PlayerLoggedOutEvent event) {
        onlinePlayerMap.remove(event.player.getName());
    }

    @SubscribeEvent
    public void event(TickEvent.ClientTickEvent event) {
        while(!RUNNABLE_CLIENT_QUEUE.isEmpty()) {
            RUNNABLE_CLIENT_QUEUE.poll().run();
        }
    }

    @SubscribeEvent
    public void event(TickEvent.ServerTickEvent event) {
        while(!RUNNABLE_SERVER_QUEUE.isEmpty()) {
            RUNNABLE_SERVER_QUEUE.poll().run();
        }
    }

    @Override
    public void scheduleTaskSync(Runnable runnable) {
        if(RUNNABLE_CLIENT_QUEUE == null) {
            RUNNABLE_CLIENT_QUEUE = new LinkedList<>();
            FMLCommonHandler.instance().bus().register(this);
        }
        //noinspection SynchronizeOnNonFinalField
        synchronized (RUNNABLE_CLIENT_QUEUE) {
            RUNNABLE_CLIENT_QUEUE.add(runnable);
        }
    }

    @Override
    public void scheduleServerTaskSync(Runnable runnable) {
        if(RUNNABLE_SERVER_QUEUE == null) {
            RUNNABLE_SERVER_QUEUE = new LinkedList<>();
            FMLCommonHandler.instance().bus().register(this);
        }
        //noinspection SynchronizeOnNonFinalField
        synchronized (RUNNABLE_SERVER_QUEUE) {
            RUNNABLE_SERVER_QUEUE.add(runnable);
        }
    }

    @Override
    public boolean isServerThread(Thread thread) {
        return false;
    }

    @Override
    public UUID getPlayerEntityUniqueId(Object playerEntity) {
        return null;
    }

    @Override
    public String getPlayerEntityName(Object playerEntity) {
        return null;
    }

    @Override
    public double getDistanceBetween(Object entity, Object entity1) {
        return 0;
    }

    @Override
    public double getDistanceBetween(Object entity, double x, double y, double z) {
        return 0;
    }

    @Override
    public int getPlayerDimension(Object playerEntity) {
        return 0;
    }

    @Override
    public <PLAYER> Stream<PLAYER> getOnlinePlayersStream(Class<? super PLAYER> aClass) {
        return null;
    }

    @Override
    public <PLAYER> AbstractNetworkAdapter<PLAYER> newNetworkAdapter(Class<? super PLAYER> aClass, String channelName) {
        return null;
    }

    public static class VersionChecker implements IVersionChecker {
        @Override
        public boolean check() {
            return true;
        }
    }
}
