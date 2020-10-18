package ru.xlv.packetapi.server.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xlv.packetapi.common.packet.registration.PacketSubscriber;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
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
        Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin.getClass().isAnnotationPresent(PacketSubscriber.class))
                .map(Plugin::getClass)
                .forEach(PacketRegistrationRouterBukkit.getInstance()::scanThenRegister);
    }

    public static Collection<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
    }

    public static PacketHandlerBukkit getPacketHandler() {
        return instance;
    }
}
