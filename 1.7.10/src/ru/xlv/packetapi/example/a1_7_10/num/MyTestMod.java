package ru.xlv.packetapi.example.a1_7_10.num;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;

import java.util.Random;

import static ru.xlv.packetapi.example.a1_7_10.num.MyTestMod.MODID;

@Mod(
        modid = MODID
)
@PacketSubscriber(channelName = MODID, packets = {
        MyTestCallback.class,
        MyTestCallbackOnServer.class
})
public class MyTestMod {

    static final String MODID = "packetapiexample";

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
            System.out.println(String.format("Sending a number %s for verification to the server side...", v));
            Sender.callback(new MyTestCallback(v))
                    .send()
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
