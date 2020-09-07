package ru.xlv.packetapi.example.bukkit;

import net.minecraftforge.fml.common.Mod;
import ru.xlv.packetapi.client.PacketHandlerClient;
import ru.xlv.packetapi.common.registry.SimplePacketRegistry;

@Mod(modid = "timesenderexample")
public class OnlineTimeSenderMod {
    private PacketHandlerClient packetHandlerClient = new PacketHandlerClient(new SimplePacketRegistry().register(new PacketOnlineReceive()), "timesenderexample");
}
