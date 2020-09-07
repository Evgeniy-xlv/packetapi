package ru.xlv.packetapi.example.bukkit;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.common.registry.PacketRegistry;
import ru.xlv.packetapi.server.PacketHandlerBukkitServer;

import java.util.UUID;

public class OnlineTimeSenderPlugin extends JavaPlugin implements Listener {

    private PacketHandlerBukkitServer packetHandlerBukkitServer;
    public static final TObjectLongMap<UUID> ONLINE_MAP = new TObjectLongHashMap<>();

    @Override
    public void onEnable() {
        String channelName = "timesenderexample";
        PacketRegistry packetRegistry = new PacketRegistry()
                .register(channelName, new PacketOnlineSend())
                .applyRegistration();
        packetHandlerBukkitServer = new PacketHandlerBukkitServer(this, packetRegistry, channelName);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> packetHandlerBukkitServer.sendPacketToAll(new PacketOnlineSend()), 0L, 20L);
    }

    @EventHandler
    public void event(PlayerLoginEvent event) {
        System.out.println("Logged in " + event.getPlayer().getName());
        ONLINE_MAP.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
