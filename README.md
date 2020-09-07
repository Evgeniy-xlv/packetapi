# PacketAPI is designed to make networking easier in Minecraft. Briefly:

- Make life easier for yourself and get rid of the need to create a handler for each packet, while maintaining OOP style and synchronization of handling
- Register packets without the need to specify descriminators and other things
- Callbacks. Send the request to the server and wait for the response asynchronously, and then process the result synchronously, all in one line of code
- Control over packets. Suppress spam of packets with one annotation
- Use convenient PacketHandlers, which contain a set of popular methods for sending data to and fro
- Convert Object <-> byte [] without pain
- Work with both ForgeClient <-> ForgeServer and ForgeClient <-> BukkitAPI
- Lazy data sending without packets

```java
packetHandler.sendCallback(new MyMathQuestion("2 * 2 = 4 ?"))
    .onResult(result -> {
        if(result.isTrue()) {
            sout("Success");
        } else {
            sout("Failure");
        }
    })
    .onTimeout(() -> sout("TIMEOUT"))
    .onException(() -> sout("EXCEPTION"));
```

```java
// writing
void write(EntityPlayerMP entityPlayer, ByteBufOutputStream bbos) throws IOException {
    ShopCategory category = new ShopCategory();
    for (int i = 0; i < ; i++) {
        category.add(new ShopItem("Name", 1000));
    }
    writeObject(bbos, category);
}
// reading
void read(ByteBufInputStream bbis) throws IOException {
    ShopCategory category = readObject(bbis, ShopCategory.class);
}
```
