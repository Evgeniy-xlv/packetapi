package ru.xlv.packetapi.example.num;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.num.MyTestMod.MODID;

@Mod(MODID)
public class MyTestMod {

    static final String MODID = "packetapiexample";

    private PacketHandlerClient packetHandlerClient = DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> new PacketHandlerClient(new SimplePacketRegistry().register(0, new MyTestCallback()), MODID));
    private PacketHandlerServer packetHandlerServer = DistExecutor.callWhenOn(Dist.DEDICATED_SERVER, () -> () -> new PacketHandlerServer(new SimplePacketRegistry().register(0, new MyTestCallbackOnServer()), MODID));

    @SubscribeEvent
    public void event(FMLCommonSetupEvent event) {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(this));
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if(event.getAction() == 0 && event.getKey() == 0x19) {
            int v = new Random().nextInt(10);
            System.out.println(String.format("Sending a number о %s for verification to the server side...", v));
            packetHandlerClient.sendCallback(new MyTestCallback(v))
                    .onResult(result -> {
                        if (result.isSuccess()) {
                            System.out.println("Success!");
                        } else {
                            System.out.println("Failure!");
                        }
                        System.out.println(result.getResponseMessage());
                    });
        }
    }
}
