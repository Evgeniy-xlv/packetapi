package ru.xlv.packetapi.example.num;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.num.MyTestMod.MODID;

@Mod(
        modid = MODID
)
public class MyTestMod {

    static final String MODID = "packetapiexample";

    private final PacketHandlerClient packetHandlerClient = new PacketHandlerClient(new SimplePacketRegistry().register(0, new MyTestCallback()));
    private final PacketHandlerServer packetHandlerServer = new PacketHandlerServer(new SimplePacketRegistry().register(0, new MyTestCallbackOnServer()));

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_P)) {
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
