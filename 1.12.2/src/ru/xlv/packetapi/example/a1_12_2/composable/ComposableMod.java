package ru.xlv.packetapi.example.a1_12_2.composable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.composable.ComposableCatcher;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;

import static ru.xlv.packetapi.example.a1_12_2.composable.ComposableMod.MODID;

@Mod(modid = MODID)
@PacketSubscriber
public class ComposableMod {

    static final String MODID = "catcherexample";

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(this);
        }
        PacketAPI.getComposableCatcherBus().register(this, TestKeyComposable.class);
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        // it happens on the client side
        Sender.composable(new TestKeyComposable(Keyboard.getEventKey(), Keyboard.getEventKeyState()))
                .fromClient()
                .send();
        // or PacketHandlerClient.getInstance().sendComposable(new TestKeyComposable(Keyboard.getEventKey(), Keyboard.getEventKeyState()));
    }

    @ComposableCatcher
    public void catcher(TestKeyComposable testKeyComposable, EntityPlayer entityPlayer) {
        // it happens on the server side
        System.out.println(entityPlayer.getName() + (testKeyComposable.isPressed() ? " " : " un") + "pressed the key " + Keyboard.getKeyName(testKeyComposable.getKeyCode()));
    }
}
