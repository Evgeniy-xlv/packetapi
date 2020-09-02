package ru.xlv.packetapi.example.shop;

import lombok.Getter;
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

import static ru.xlv.packetapi.example.shop.ShopMod.MODID;

@Mod(
        modid = MODID
)
public class ShopMod {

    static final String MODID = "exampleshopmod";

    @Mod.Instance(MODID)
    public static ShopMod INSTANCE;

    @Getter
    private final ShopItemManager shopItemManager = new ShopItemManager();

    private PacketHandlerClient packetHandlerClient;
    private PacketHandlerServer packetHandlerServer;

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        PacketRegistry packetRegistry = new PacketRegistry();
        if(event.getSide() == Side.CLIENT) {
            packetRegistry.register(MODID, new PacketShopCategoryGet());
            packetRegistry.applyRegistration();
            packetHandlerClient = new PacketHandlerClient(packetRegistry, MODID);
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            packetRegistry.register(MODID, new PacketShopCategoryGetOnServer());
            packetRegistry.applyRegistration();
            packetHandlerServer = new PacketHandlerServer(packetRegistry, MODID);
        }
    }

    @SubscribeEvent
    public void event(InputEvent.KeyInputEvent event) {
        if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_O)) {
            String categoryChars = "qweasdzxcrtyfghvbbnuiojklmp";
            Random random = new Random();
            StringBuilder category = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                category.append(categoryChars.charAt(random.nextInt(categoryChars.length())));
            }
            System.out.println("Отправляю запрос товаров магазина по категории: " + category.toString());
            packetHandlerClient.sendPacketEffectiveCallback(new PacketShopCategoryGet(category.toString()))
                    .thenAcceptSync(result -> {
                        System.out.println("Пришедший список товаров из магазина:");
                        result.getShopItemList().forEach(System.out::println);
                        System.out.println("Сообщение от сервера: " + result.getResponseMessage());
                    });
        }
    }
}
