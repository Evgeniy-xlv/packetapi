package ru.xlv.packetapi.example.a1_12_2.shop;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;

import java.util.Random;

import static ru.xlv.packetapi.example.a1_12_2.shop.ShopMod.MODID;

@Mod(modid = MODID)
@PacketSubscriber(channelName = MODID, packets = {
        PacketShopCategoryGet.class,
        PacketShopCategoryGetOnServer.class,
})
public class ShopMod {

    static final String MODID = "exampleshopmod";

    @Mod.Instance(MODID)
    public static ShopMod INSTANCE;

    private final ShopItemManager shopItemManager = new ShopItemManager();

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        if(event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        // it happens on the client side
        if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_O)) {
            String categoryChars = "qweasdzxcrtyfghvbbnuiojklmp";
            Random random = new Random();
            StringBuilder category = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                category.append(categoryChars.charAt(random.nextInt(categoryChars.length())));
            }
            System.out.println("Sending a request to the server for shop items by category: " + category.toString());
            Sender.callback(new PacketShopCategoryGet(category.toString()))
                    .timeout(2000L)
                    .send()
                    .onResult(result -> {
                        // it will happen when a callback will return back with the result to the client side
                        System.out.println("The incoming set of shop items:");
                        result.getShopItemList().forEach(System.out::println);
                        System.out.println("The message from the server: " + result.getResponseMessage());
                    })
                    .onTimeout(() -> System.out.println("timeout"))
                    .onError(exception -> System.out.println("exception: " + exception));
        }
    }

    public ShopItemManager getShopItemManager() {
        return this.shopItemManager;
    }
}
