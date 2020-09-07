package ru.xlv.packetapi;

import ru.xlv.packetapi.capability.ICapabilityAdapter;
import ru.xlv.packetapi.common.composable.ComposableCatcherBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class PacketAPI {

    static {
        try {
            Enumeration<URL> resources = PacketAPI.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while(resources.hasMoreElements()) {
                URL x = resources.nextElement();
                InputStream inputStream = x.openStream();
                Manifest manifest = new Manifest(inputStream);
                String gameVersion = manifest.getMainAttributes().getValue("PacketAPI_GameVersion");
                if (gameVersion != null) {
                    initCapabilityAdapter(gameVersion);
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
    }

    private static ICapabilityAdapter capabilityAdapter;
    private static final ComposableCatcherBus COMPOSABLE_CATCHER_BUS = new ComposableCatcherBus();

    private static final String API_DEFAULT_CHANNEL_NAME = "packetapi";

    private PacketAPI() {}

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

    private static void initCapabilityAdapter(String gameVersion) {
        try {
            setCapabilityAdapter((ICapabilityAdapter) Class.forName("ru.xlv.packetapi.capability.CapabilityAdapter" + gameVersion.replace(".", "_")).newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
