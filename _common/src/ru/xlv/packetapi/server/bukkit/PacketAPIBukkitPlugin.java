package ru.xlv.packetapi.server.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.common.packet.autoreg.AutoRegPacketSubscriber;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

public class PacketAPIBukkitPlugin extends JavaPlugin {

    private static PacketHandlerBukkit instance;

    @Override
    public void onEnable() {
        //todo fix
        try {
            Constructor<PacketHandlerBukkit> constructor = PacketHandlerBukkit.class.getDeclaredConstructor(JavaPlugin.class);
            constructor.setAccessible(true);
            instance = constructor.newInstance(this);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoad() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            AutoRegPacketSubscriber annotation = plugin.getClass().getAnnotation(AutoRegPacketSubscriber.class);
            if(annotation != null) {
                if (annotation.packages().length > 0) {
                    for (String aPackage : annotation.packages()) {
                        AutoRegPacketScannerBukkit.getInstance().scanThenRegister(aPackage);
                    }
                } else {
                    AutoRegPacketScannerBukkit.getInstance().scanThenRegister(plugin.getClass().getPackage().getName());
                }
            }
        }
    }

    public static Collection<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
    }

    public static PacketHandlerBukkit getPacketHandler() {
        return instance;
    }
}
