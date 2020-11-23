package ru.xlv.packetapi.example.a1_7_10.weighty_composable;

import net.minecraft.item.ItemStack;
import ru.xlv.packetapi.common.composable.Composable;

import java.util.UUID;

public class ProductItemStack implements Composable {

    private final UUID id;
    private final String displayName;
    private final ItemStack itemStack;

    public ProductItemStack(UUID id, String displayName, ItemStack itemStack) {
        this.id = id;
        this.displayName = displayName;
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ProductItemStack{" +
                "id=" + id +
                ", displayName='" + displayName + '\'' +
                ", itemStack=" + itemStack +
                '}';
    }
}
