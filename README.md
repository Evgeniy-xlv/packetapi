# PacketAPI is designed to make networking easier in Minecraft.

### Briefly:

* No more handlers for each packet, keeping OOP style and processing synchronous
* Registering packets without the need to specify descriminators and other things
* Callbacks. Sending requests to the server and asynchronously waiting for a response, and then processing the result synchronously, all in one line of code
* Control over packages. Suppression of spam with packets of one annotation
* Rich functionality for sending packets in packs with filtering and black jack
* Converting Object <-> byte[] without pain
* Full `BukkitAPI` and `Forge` support
* Lazy data sending without packets

## Usage examples

```java
@Mod(modid = "test")
public class MyMod {
    public void exampleMethod() {
        Sender.callback(new MyMathCallback("2 * 2 = 4 ?"))
            .send() // sending a callback to the server
            .onResult(result -> { // when the response came back from the server
                if(result.isTrue()) {
                    System.out.println("It is true");
                } else {
                    System.out.println("It is not true");
                }
            })
            .onTimeout(() -> System.out.println("TIMEOUT")) // on timeout
            .onError(exception -> System.out.println("EXCEPTION: " + exception)); // if an exception thrown during execution
    }
}
```

```java
@Packet
public class MyPacket implements IPacketOutServer, IPacketInClient { 
    // writing on the server
    @Override
    public void write(EntityPlayer entityPlayer, ByteBufOutputStream bbos) throws IOException {
        ShopCategory category = new ShopCategory();
        for (int i = 0; i < 10; i++) {
            category.add(new ShopItem("Name", 1000));
        }
        writeObject(bbos, category);
    }
    // reading on the client
    @Override
    public void read(ByteBufInputStream bbis) throws IOException {
        ShopCategory category = readObject(bbis, ShopCategory.class);
    }
}
```

You can find more examples in the source code.

## Tools

* Forge client tools:
    * `PacketHandlerClient` is a tool handling and sending packets from the client side
    * Base packets:
        * `IPacketInClient` is a base incoming packet
        * `IPacketOutClient` is a base outgoing packet
        * `ICallbackOut` is a base outgoing callback packet
* Forge server tools:
    * `PacketHandlerServer` is a tool handling and sending packets from the server side
    * Base packets:
        * `IPacketInServer` is a base incoming packet
        * `IPacketOutServer` is a base outgoing packet
        * `ICallbackInServer` is a base incoming callback packet
* Bukkit tools:
    * `PacketHandlerBukkitServer` is a tool handling and sending packets from the bukkit side
    * Base packets:
        * `IPacketInBukkit` is a base incoming packet
        * `IPacketOutBukkit` is a base outgoing packet
        * `ICallbackInBukkit` is a base incoming callback packet
    
* Misc tools:
    * `Composable` is a type of objects that can be sent without packets. See examples and docs for more information
    * `Sender` is a one of the main tools for sending outgoing packets, callbacks and `Composable` objects. See examples and docs for more information
    * `RequestController` is a tool for filtering and scheduling packet execution. See examples and docs for more information
    * `Packet` and `PacketSubscriber` are annotations to automatically register your packets. See examples and docs for more information

## PacketAPI supports two ways to register packets

* **Using annotations.** The api provides the developer with two annotations to easily register packets: `@Packet` and `@PacketSubscriber`
    * `@PacketSubscriber` annotation should mark the main class of your mod(class annotated with `@Mod`).
    * `@Packet` annotation should be used to mark your packets classes. See examples in the source code for more information.
* **Using methods of packet handlers.**
    * `PacketHandlerClient.getInstance().registerPacket(channelName, packet);`
    * `PacketHandlerServer.getInstance().registerPacket(channelName, packet);`
    * `PacketHandlerBukkit.getInstance().registerPacket(channelName, packet);`

## Install

**Go to [Releases](https://github.com/Evgeniy-xlv/packetapi/releases) and download one of them.**

**ACHTUNG!** PacketAPI was developed for Minecraft `v1.7.10` and `v1.12.2` and works correctly there. It does not mean that it will 
definitely fail on other versions, but you should use it at your own peril and risk.

p.s. use a jar file called `packetapi-@VERSION@-no-reobf.jar` if you cannot run the game with this library from the IDE. 
It's possible when working with `Minecraft v1.7.10`

## Dependencies

PacketAPI depends on the [Reflections v0.9.11 library](https://mvnrepository.com/artifact/org.reflections/reflections/0.9.11) 
to scan packages for `@Packet` annotations. It is an optional dependency. PacketAPI will work correctly without this dependency, 
but auto scanning will not work.

## License

See the [License](https://github.com/Evgeniy-xlv/packetapi/blob/master/LICENSE)
