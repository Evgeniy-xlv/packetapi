package ru.xlv.packetapi.example.a1_7_10.weighty_composable;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import ru.xlv.packetapi.PacketAPI;
import ru.xlv.packetapi.common.composable.ComposableCatcher;
import ru.xlv.packetapi.common.composable.ComposeAdapter;
import ru.xlv.packetapi.common.sender.Sender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This example will show you how to send weighty Composables using the @Lightweight annotation,
 * which optimizes the object's weight as much as possible so as not to reach the packet weight limits set in the game.
 *
 * This example will send the player a category with products if the player jumps.
 * */
@Mod(modid = "weighty_composable")
public class WeightyComposableMod {

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        // registering a new compose adapter for UUID
        PacketAPI.getComposer().registerComposeAdapter(
                UUID.class,
                new ComposeAdapter<>(
                        (uuid, byteBufOutputStream) -> byteBufOutputStream.writeUTF(uuid.toString()),
                        byteBufInputStream -> UUID.fromString(byteBufInputStream.readUTF())
                )
        );
        // registering a new compose adapter for ItemStack
        PacketAPI.getComposer().registerComposeAdapter(
                ItemStack.class,
                new ComposeAdapter<>(
                        (itemStack, byteBufOutputStream) -> ByteBufUtils.writeItemStack(byteBufOutputStream.buffer(), itemStack),
                        byteBufInputStream -> ByteBufUtils.readItemStack(byteBufInputStream.getBuffer())
                )
        );
        if(event.getSide().isClient()) {
            // registering this object as event listener
            MinecraftForge.EVENT_BUS.register(this);
            // registering this object as composable catcher
            PacketAPI.getComposableCatcherBus().register(this, ShopCategory.class);
        }
    }

    @SubscribeEvent
    public void event(LivingEvent.LivingJumpEvent event) {
        if(!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPlayer) {
            // it happens on the server side
            for (int i = 0; i < 1000; i++) {
                // generating a new category
                ShopCategory composable = new ShopCategory(
                        i,
                        "testDisplayName" + i,
                        asList(
                                new ProductItemStack(UUID.randomUUID(), "testDispayName 1", new ItemStack(Items.apple, 64)),
                                new ProductItemStack(UUID.randomUUID(), "testDispayName 2", new ItemStack(Items.carrot, 12)),
                                new ProductItemStack(UUID.randomUUID(), "testDispayName 3", new ItemStack(Items.fish, 3))
                        )
                );
                // sending composable to the player
                Sender.composable(composable)
                        .fromServer()
                        .to((EntityPlayer) event.entityLiving)
                        .send();
            }
        }
    }

    @ComposableCatcher
    public void catcher(ShopCategory shopCategory) {
        // it happens on the client side
        System.out.println("category from the server side: " + shopCategory);
    }

    /**
     * Utility method collects all objects from the array into a new ArrayList.
     * */
    @SafeVarargs
    @SuppressWarnings("varargs")
    private static <T> List<T> asList(T... ts) {
        List<T> list = new ArrayList<>(ts.length);
        for (T t : ts) {
            list.add(t);
        }
        return list;
    }
}
