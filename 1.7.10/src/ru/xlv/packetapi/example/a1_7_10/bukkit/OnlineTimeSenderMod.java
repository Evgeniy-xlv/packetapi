package ru.xlv.packetapi.example.a1_7_10.bukkit;

import cpw.mods.fml.common.Mod;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;

@Mod(
        modid = "timesenderexample"
)
@PacketSubscriber(channelName = "timesenderexample", packets = {
        PacketOnlineSend.class,
        PacketOnlineReceive.class
})
public class OnlineTimeSenderMod {
}
