# PacketAPI is designed to make networking easier in Minecraft.

[![](https://jitpack.io/v/Evgeniy-Xlv/packetapi.svg)](https://jitpack.io/#Evgeniy-Xlv/packetapi)

### Briefly:

* Make life easier for yourself and get rid of the need to create a handler for each packet, while maintaining OOP style and synchronization of handling
* Register packets without the need to specify descriminators and other things
* Callbacks. Send the request to the server and wait for the response asynchronously, and then process the result synchronously, all in one line of code
* Control over packets. Suppress spam of packets with one annotation
* Use convenient PacketHandlers, which contain a set of popular methods for sending data to and fro
* Convert Object <-> byte [] without pain
* Work with both ForgeClient <-> ForgeServer and ForgeClient <-> BukkitAPI
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
@AutoRegPacket(channelName = "test")
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
    * `RequestController` is a tool for filtering and scheduling packet execution. See examples and docs for more information
    * `AutoRegPacket` and AutoRegPacketSubscriber are annotations to automatically register your packets. See examples and docs for more information

## PacketAPI supports two ways to register packets

* **Using annotations.** The api provides the developer with two annotations to easily register packets: `@AutoRegPacket` and `@AutoRegPacketSubscriber`
    * `@AutoRegPacketSubscriber` annotation should mark the main class of your mod(class annotated with `@Mod`).
    * `@AutoRegPacket` annotation should be used to mark your packets classes. See examples in the source code for more information.
* **Using methods of packet handlers.**
    * `PacketHandlerClient.getInstance().registerPacket(channelName, packet);`
    * `PacketHandlerServer.getInstance().registerPacket(channelName, packet);`
    * `PacketHandlerBukkit.getInstance().registerPacket(channelName, packet);`

## Install

### Gradle:

Add following lines to **build.gradle**:
```gradle
    repositories {
        maven { url 'https://jitpack.io' }
    }
    
    dependencies {
        compile group: 'org.reflections', name: 'reflections', version: '0.9.11'
        compile group: 'com.github.Evgeniy-Xlv' name:'packetapi' version:'1.12.2-1.2.0'
    }
```

### Maven:

Add folowing lines to **pom.xml**:
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    
    <dependency>
        <groupId>com.github.Evgeniy-Xlv</groupId>
        <artifactId>packetapi</artifactId>
        <version>1.12.2-1.2.0</version>
    </dependency>
```

## Dependencies

PacketAPI depends on the [Reflections v0.9.11 library](https://mvnrepository.com/artifact/org.reflections/reflections/0.9.11) 
for working with `@AutoRegPacket` annotations. It is an optional dependency. PacketAPI will work correctly without this dependency, 
but `@AutoRegPacket` will not work.

## License

See the [License](https://github.com/Evgeniy-xlv/packetapi/blob/master/LICENSE)