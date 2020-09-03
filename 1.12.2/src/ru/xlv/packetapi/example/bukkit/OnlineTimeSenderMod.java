package ru.xlv.packetapi.example.bukkit;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.PacketRegistry;

@Mod(
        modid = "timesenderexample"
)
public class OnlineTimeSenderMod {

    private PacketHandlerClient packetHandlerClient;

    @Mod.EventHandler
    public void event(FMLInitializationEvent event) {
        PacketRegistry packetRegistry = new PacketRegistry()
                .register("timesenderexample", new PacketOnlineReceive())
                .applyRegistration();
        packetHandlerClient = new PacketHandlerClient(packetRegistry, "timesenderexample");
    }
}
