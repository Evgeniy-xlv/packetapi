package ru.xlv.packetapi.example.a1_7_10.shop;

import ru.xlv.packetapi.common.composable.Composable;

public class ShopItem implements Composable {

    private final int id;
    private final String name;
    private final int price;

    public ShopItem(int id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getPrice() {
        return this.price;
    }

    public String toString() {
        return "ShopItem(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ")";
    }
}
