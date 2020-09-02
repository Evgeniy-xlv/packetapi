package ru.xlv.packetapi.example.shop;

import java.util.Arrays;
import java.util.List;

public class ShopItemManager {

    //имитируем выдачу товаров по категории
    public List<ShopItem> getItemListByCategory(String category) {
        return Arrays.asList(
                new ShopItem(0, "Имя", 123),
                new ShopItem(1, "любимое", 321),
                new ShopItem(2, "мое", 132)
        );
    }
}
