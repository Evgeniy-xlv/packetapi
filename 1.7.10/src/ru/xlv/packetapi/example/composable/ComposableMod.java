package ru.xlv.packetapi.example.composable;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.composable.ComposableCatcher;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static ru.xlv.packetapi.example.composable.ComposableMod.MODID;

@Mod(modid = MODID)
public class ComposableMod {

    static final String MODID = "catcherexample";

    private final PacketHandlerClient packetHandlerClient = new PacketHandlerClient();
    private final PacketHandlerServer packetHandlerServer = new PacketHandlerServer();

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            PacketAPI.getComposableCatcherBus().register(this, TestKeyComposable.class);
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        // it happens on the client side
        packetHandlerClient.sendComposable(new TestKeyComposable(Keyboard.getEventKey(), Keyboard.getEventKeyState()));
    }

    @ComposableCatcher
    public void catcher(TestKeyComposable testKeyComposable, EntityPlayer entityPlayer) {
        // it happens on the server side
        Arrays.stream(Keyboard.class.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType() == int.class)
                .filter(field -> {
                    Object o = null;
                    try {
                        o = field.get(null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return ((int) o) == testKeyComposable.getKeyCode();
                })
                .findFirst()
                .ifPresent(field -> System.out.println("The player " + entityPlayer.getCommandSenderName() + (testKeyComposable.isPressed() ? " " : " un") + "pressed the key " + field.getName().replace("KEY_", "")));
    }
}
