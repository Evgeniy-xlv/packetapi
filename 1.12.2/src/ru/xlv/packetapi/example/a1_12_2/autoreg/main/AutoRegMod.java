package ru.xlv.packetapi.example.a1_12_2.autoreg.main;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;
import ru.xlv.packetapi.example.a1_12_2.autoreg.packets.FirstPacketClientExample;
import ru.xlv.packetapi.example.a1_12_2.autoreg.packets.SecondPacketClientExample;
import ru.xlv.packetapi.example.a1_12_2.autoreg.packets.SecondPacketServerExample;
import ru.xlv.packetapi.example.a1_12_2.autoreg.packets.ThirdPacketClientExample;

import static ru.xlv.packetapi.example.a1_12_2.autoreg.main.AutoRegMod.MODID;

@Mod(
        modid = "autoregexample"
)
@PacketSubscriber(channelName = MODID, packages = "ru.xlv.packetapi.example.a1_12_2.autoreg.packets", enableReflectionsScanner = true)
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
        }
        if (Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_U)) {
            Sender.packet(new SecondPacketClientExample())
                    .send();
        }
        if (Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_J)) {
            Sender.packet(new ThirdPacketClientExample())
                    .send();
        }
    }

    @SubscribeEvent
    public void event(LivingEvent.LivingJumpEvent event) {
        if(FMLCommonHandler.instance().getSide() == Side.SERVER && event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            Sender.packet(new SecondPacketServerExample())
                    .to(player)
                    .send();
            Sender.packet(new SecondPacketServerExample())
                    .toDimension(event.getEntityLiving().dimension)
                    .except(player)
                    .except(Entity::isInvisible)
                    .send();
        }
    }
}
