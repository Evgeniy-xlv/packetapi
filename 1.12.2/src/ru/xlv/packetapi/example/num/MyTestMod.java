package ru.xlv.packetapi.example.num;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.num.MyTestMod.MODID;

@Mod(modid = MODID)
public class MyTestMod {

    static final String MODID = "packetapiexample";

    private PacketHandlerClient packetHandlerClient;
    private PacketHandlerServer packetHandlerServer;

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide() == Side.CLIENT) {
            packetHandlerClient = new PacketHandlerClient(new SimplePacketRegistry().register(0, new MyTestCallback()), MODID);
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            packetHandlerServer = new PacketHandlerServer(new SimplePacketRegistry().register(0, new MyTestCallbackOnServer()), MODID);
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_P)) {
            int v = new Random().nextInt(10);
            System.out.println(String.format("Sending a number о %s for verification to the server side...", v));
            packetHandlerClient.sendPacketEffectiveCallback(new MyTestCallback(v))
                    .thenAcceptSync(result -> {
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
