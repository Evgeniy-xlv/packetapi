package ru.xlv.packetapi.example.a1_7_10.bukkit;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacketSubscriber;
import ru.xlv.packetapi.common.sender.Sender;

import java.util.UUID;

@AutoRegPacketSubscriber
public class OnlineTimeSenderPlugin extends JavaPlugin implements Listener {

    public static final TObjectLongMap<UUID> ONLINE_MAP = new TObjectLongHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> Sender.packet(new PacketOnlineSend()).toAll().send(), 0L, 20L);
    }

    @EventHandler
    public void event(PlayerLoginEvent event) {
        System.out.println("Logged in " + event.getPlayer().getName());
        ONLINE_MAP.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
