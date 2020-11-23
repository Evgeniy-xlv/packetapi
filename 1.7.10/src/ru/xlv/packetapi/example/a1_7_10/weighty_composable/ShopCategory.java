package ru.xlv.packetapi.example.a1_7_10.weighty_composable;

import ru.xlv.packetapi.common.composable.Composable;
import ru.xlv.packetapi.common.composable.Lightweight;

import java.util.List;

@Lightweight(deep = Integer.MAX_VALUE)
public class ShopCategory implements Composable {

    private final int categoryId;
    private final String displayName;

    private final List<ProductItemStack> productItemStackList;

    public ShopCategory(int categoryId, String displayName, List<ProductItemStack> productItemStackList) {
        this.categoryId = categoryId;
        this.displayName = displayName;
        this.productItemStackList = productItemStackList;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public List<ProductItemStack> getProductItemStackList() {
        return productItemStackList;
    }

    @Override
    public String toString() {
        return "ShopCategory{" +
                "categoryId=" + categoryId +
                ", displayName='" + displayName + '\'' +
                ", productItemStackList=" + productItemStackList +
                '}';
    }
}
