package ru.xlv.packetapi.example.a1_12_2.bukkit;

import net.minecraftforge.fml.common.Mod;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;

@Mod(modid = "timesenderexample")
@PacketSubscriber(channelName = "timesenderexample", packets = {
        PacketOnlineSend.class,
        PacketOnlineReceive.class
})
public class OnlineTimeSenderMod {
}