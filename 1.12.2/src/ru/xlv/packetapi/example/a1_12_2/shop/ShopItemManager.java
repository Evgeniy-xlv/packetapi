package ru.xlv.packetapi.example.a1_12_2.shop;

import java.util.Arrays;
import java.util.List;

public class ShopItemManager {

    //имитируем выдачу товаров по категории
    public List<ShopItem> getItemListByCategory(String category) {
        return Arrays.asList(
                new ShopItem(0, "AAA", 123),
                new ShopItem(1, "BBB", 321),
                new ShopItem(2, "CCC", 132)
        );
    }
}
