package ru.xlv.packetapi.example.composable;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.composable.ComposableCatcher;
import ru.xlv.packetapi.server.PacketHandlerServer;

import static ru.xlv.packetapi.example.composable.ComposableMod.MODID;

@Mod(MODID)
public class ComposableMod {

    static final String MODID = "catcherexample";

    private final PacketHandlerClient packetHandlerClient = DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> new PacketHandlerClient(MODID));
    private final PacketHandlerServer packetHandlerServer = DistExecutor.callWhenOn(Dist.DEDICATED_SERVER, () -> () -> new PacketHandlerServer(MODID));

    @SubscribeEvent
    public void event(FMLCommonSetupEvent event) {
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> {
            PacketAPI.getComposableCatcherBus().register(this, TestKeyComposable.class);
            return null;
        });
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        // it happens on the client side
        packetHandlerClient.sendComposable(new TestKeyComposable(event.getKey(), event.getAction() == 0));
    }

    @ComposableCatcher
    public void catcher(TestKeyComposable testKeyComposable, ServerPlayerEntity entityPlayer) {
        // it happens on the server side
        System.out.println(testKeyComposable);
    }
}
