package ru.xlv.packetapi.example.num;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.PacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.num.MyTestMod.MODID;

@Mod(
        modid = MODID
)
public class MyTestMod {

    static final String MODID = "packetapiexample";

    private PacketHandlerClient packetHandlerClient;
    private PacketHandlerServer packetHandlerServer;

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide() == Side.CLIENT) {
            PacketRegistry packetRegistry = new PacketRegistry()
                    .register(MODID, new MyTestCallback())
                    .applyRegistration();
            packetHandlerClient = new PacketHandlerClient(packetRegistry, "packetapi");

            MinecraftForge.EVENT_BUS.register(this);
        } else {
            PacketRegistry packetRegistry = new PacketRegistry()
                    .register(MODID, new MyTestCallbackOnServer())
                    .applyRegistration();
            packetHandlerServer = new PacketHandlerServer(packetRegistry, "packetapi");
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_P)) {
            int v = new Random().nextInt(10);
            System.out.println(String.format("Отправляю число %s на проверку...", v));
            packetHandlerClient.sendPacketEffectiveCallback(new MyTestCallback(v))
                    .thenAcceptSync(result -> {
                        if (result.isSuccess()) {
                            System.out.println("Успех!");
                        } else {
                            System.out.println("Неудача!");
                        }
                        System.out.println(result.getResponseMessage());
                    });
        }
    }
}
