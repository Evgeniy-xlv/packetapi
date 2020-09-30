package ru.xlv.packetapi.example.a1_7_10.autoreg.main;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;
import ru.xlv.packetapi.example.a1_7_10.autoreg.packets.FirstPacketClientExample;
import ru.xlv.packetapi.example.a1_7_10.autoreg.packets.SecondPacketClientExample;
import ru.xlv.packetapi.example.a1_7_10.autoreg.packets.SecondPacketServerExample;

@Mod(
        modid = "autoregexample"
)
@AutoRegPacketSubscriber(packages = "ru.xlv.packetapi.example.a1_7_10.autoreg.packets")
public class AutoRegMod {

    public static final String MODID = "autoregexample";

    @Mod.EventHandler
    public void event(FMLPostInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_I)) {
            Sender.callback(new FirstPacketClientExample())
                    .send()
                    .onResult(System.out::println)
                    .onTimeout(() -> System.out.println("No pong today..."));
            Sender.packet(new SecondPacketClientExample())
                    .send();
        }
    }

    @SubscribeEvent
    public void event(LivingEvent.LivingJumpEvent event) {
        if(FMLCommonHandler.instance().getSide() == Side.SERVER && event.entityLiving instanceof EntityPlayer) {
            Sender.packet(new SecondPacketServerExample())
                    .to((EntityPlayer) event.entityLiving)
                    .send();
            Sender.packet(new SecondPacketServerExample())
                    .toDimension(event.entityLiving.dimension)
                    .except(Entity::isInvisible)
                    .send();
        }
    }
}
