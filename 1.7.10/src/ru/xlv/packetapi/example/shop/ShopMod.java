package ru.xlv.packetapi.example.shop;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.shop.ShopMod.MODID;

@Mod(
        modid = MODID
)
public class ShopMod {

    static final String MODID = "exampleshopmod";

    @Mod.Instance(MODID)
    public static ShopMod INSTANCE;

    private final ShopItemManager shopItemManager = new ShopItemManager();

    private PacketHandlerClient packetHandlerClient;
    private PacketHandlerServer packetHandlerServer;

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        SimplePacketRegistry packetRegistry = new SimplePacketRegistry();
        if(event.getSide().isClient()) {
            packetRegistry.register(0, new PacketShopCategoryGet());
            packetHandlerClient = new PacketHandlerClient(packetRegistry);
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            packetRegistry.register(0, new PacketShopCategoryGetOnServer());
            packetHandlerServer = new PacketHandlerServer(packetRegistry);
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
            packetHandlerClient.sendCallback(new PacketShopCategoryGet(category.toString()))
                    .onResult(result -> {
                        // it will happen when a callback will return back with the result to the client side
                        System.out.println("The incoming set of shop items:");
                        result.getShopItemList().forEach(System.out::println);
                        System.out.println("The message from the server: " + result.getResponseMessage());
                    });
        }
    }

    public ShopItemManager getShopItemManager() {
        return this.shopItemManager;
    }
}
