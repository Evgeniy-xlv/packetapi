package ru.xlv.packetapi;

import ru.xlv.packetapi.capability.ICapabilityAdapter;
import ru.xlv.packetapi.capability.VersionCheckerImpl;
import ru.xlv.packetapi.common.composable.ComposableCatcherBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class PacketAPI {

    private static ICapabilityAdapter capabilityAdapter;
    private static final ComposableCatcherBus COMPOSABLE_CATCHER_BUS = new ComposableCatcherBus();

    private static final Logger LOGGER = Logger.getLogger(PacketAPI.class.getSimpleName());

    public static final String NAME = "packetapi";
    public static final String VERSION = "@PACKET_API_VERSION@";
    private static final String API_DEFAULT_CHANNEL_NAME = "packetapi";

    private static boolean isBukkitFound, isForgeFound;

    private PacketAPI() {}

    private static void initCapabilityAdapter(String gameVersion) {
        if (gameVersion != null) {
            try {
                LOGGER.info("Checking version: " + gameVersion);
                Class<?> adapter = Class.forName("ru.xlv.packetapi.capability.CapabilityAdapter" + gameVersion.replace(".", "_"));
                VersionCheckerImpl annotation = adapter.getAnnotation(VersionCheckerImpl.class);
                if (annotation != null && annotation.value().newInstance().check()) {
                    setCapabilityAdapter((ICapabilityAdapter) adapter.newInstance());
                    LOGGER.info("Success.");
                    return;
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ignored) {
            }
            LOGGER.info("Error.");
        }
    }

    public static void setCapabilityAdapter(ICapabilityAdapter capabilityAdapter) {
        PacketAPI.capabilityAdapter = capabilityAdapter;
    }

    public static ICapabilityAdapter getCapabilityAdapter() {
        return capabilityAdapter;
    }

    public static ComposableCatcherBus getComposableCatcherBus() {
        return COMPOSABLE_CATCHER_BUS;
    }

    public static String getApiDefaultChannelName() {
        return API_DEFAULT_CHANNEL_NAME;
    }

    public static boolean isBukkitFound() {
        return isBukkitFound;
    }

    public static boolean isForgeFound() {
        return isForgeFound;
    }

    static {
        try {
            Class.forName("net.minecraft.entity.player.EntityPlayer");
            isForgeFound = true;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("org.bukkit.Bukkit");
            isBukkitFound = true;
        } catch (ClassNotFoundException ignored) {
        }
        String gameVersions = "Nothing.";
        try {
            Enumeration<URL> resources = PacketAPI.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while(resources.hasMoreElements()) {
                URL x = resources.nextElement();
                InputStream inputStream = x.openStream();
                Manifest manifest = new Manifest(inputStream);
                gameVersions = manifest.getMainAttributes().getValue("PacketAPI_GameVersions");
                if (gameVersions != null) {
                    String[] gameVersions1 = gameVersions.replace("[", "").replace("]", "").split(",");
                    for (String gameVersion : gameVersions1) {
                        if (getCapabilityAdapter() == null) {
                            initCapabilityAdapter(gameVersion.trim());
                        } else {
                            break;
                        }
                    }
                    inputStream.close();
                    break;
                }
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (getCapabilityAdapter() == null) {
            initCapabilityAdapter(System.getProperty("ru.xlv.packetapi.gameVersion"));
        }
        if(getCapabilityAdapter() == null) {
            throw new RuntimeException("The current game version isn't supported by PacketAPI. Supported game versions: " + gameVersions);
        }
    }
}
