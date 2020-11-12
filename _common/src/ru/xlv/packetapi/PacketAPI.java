package ru.xlv.packetapi;

import ru.xlv.packetapi.capability.ICapabilityAdapter;
import ru.xlv.packetapi.capability.VersionCheckerImpl;
import ru.xlv.packetapi.common.composable.ComposableCatcherBus;
import ru.xlv.packetapi.common.composable.Composer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class PacketAPI {

    private static ICapabilityAdapter capabilityAdapter;
    private static final ComposableCatcherBus COMPOSABLE_CATCHER_BUS = new ComposableCatcherBus();
    private static final Composer COMPOSER = new Composer();

    public static final String NAME = "packetapi";
    public static final String VERSION = "@PACKET_API_VERSION@";
    public static final String DEFAULT_NET_CHANNEL_NAME = "packetapi";

    private static int callbackThreadPoolSize = 2;
    private static int asyncPacketThreadPoolSize = 2;

    private PacketAPI() {}

    private static void initCapabilityAdapter(String gameVersion) {
        if (gameVersion != null) {
            try {
                Class<?> adapter = Class.forName("ru.xlv.packetapi.capability.CapabilityAdapter" + gameVersion.replace(".", "_"));
                VersionCheckerImpl annotation = adapter.getAnnotation(VersionCheckerImpl.class);
                if (annotation != null && annotation.value().newInstance().check()) {
                    setCapabilityAdapter((ICapabilityAdapter) adapter.newInstance());
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ignored) {
            }
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

    public static Composer getComposer() {
        return COMPOSER;
    }

    public static int getCallbackThreadPoolSize() {
        return callbackThreadPoolSize;
    }

    public static int getAsyncPacketThreadPoolSize() {
        return asyncPacketThreadPoolSize;
    }

    private static void processProperties() {
        String callbackThreadPoolSize = System.getProperty("ru.xlv.packetapi.callbackThreadPoolSize");
        if (callbackThreadPoolSize != null) {
            try {
                PacketAPI.callbackThreadPoolSize = Integer.parseInt(callbackThreadPoolSize);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String asyncPacketThreadPoolSize = System.getProperty("ru.xlv.packetapi.asyncPacketThreadPoolSize");
        if (asyncPacketThreadPoolSize != null) {
            try {
                PacketAPI.asyncPacketThreadPoolSize = Integer.parseInt(asyncPacketThreadPoolSize);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private static void defineCurrentMinecraftVersion() {
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

    static {
        processProperties();
        defineCurrentMinecraftVersion();
    }
}
