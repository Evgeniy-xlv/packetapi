package ru.xlv.packetapi.example.shop;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerServer;

import java.util.Random;

import static ru.xlv.packetapi.example.shop.ShopMod.MODID;

@Mod(MODID)
@Mod.EventBusSubscriber
public class ShopMod {

    static final String MODID = "exampleshopmod";

    public static ShopMod INSTANCE;

    private final ShopItemManager shopItemManager = new ShopItemManager();

    private final PacketHandlerClient packetHandlerClient = DistExecutor.callWhenOn(Dist.CLIENT, () -> () -> new PacketHandlerClient(new SimplePacketRegistry().register(0, new PacketShopCategoryGet()), MODID));
    private final PacketHandlerServer packetHandlerServer = DistExecutor.callWhenOn(Dist.DEDICATED_SERVER, () -> () -> new PacketHandlerServer(new SimplePacketRegistry().register(0, new PacketShopCategoryGetOnServer()), MODID));

    @SubscribeEvent
    public void event(FMLCommonSetupEvent event) {
        INSTANCE = this;
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(this));
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        // it happens on the client side
        if(event.getAction() == 0 && event.getKey() == 0x18) {
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
                    })
                    .onTimeout(() -> System.out.println("timeout"))
                    .onException(() -> System.out.println("exception"));
        }
    }

    public ShopItemManager getShopItemManager() {
        return this.shopItemManager;
    }
}
