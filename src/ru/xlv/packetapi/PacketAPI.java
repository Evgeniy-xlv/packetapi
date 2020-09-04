package ru.xlv.packetapi;

import ru.xlv.packetapi.capability.ICapabilityAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class PacketAPI {

    public static final PacketAPI INSTANCE = new PacketAPI();

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
        if (INSTANCE.getCapabilityAdapter() == null) {
            initCapabilityAdapter(System.getProperty("ru.xlv.packetapi.gameVersion"));
        }
    }

    private ICapabilityAdapter capabilityAdapter;

    private PacketAPI() {}

    public void setCapabilityAdapter(ICapabilityAdapter capabilityAdapter) {
        this.capabilityAdapter = capabilityAdapter;
    }

    public ICapabilityAdapter getCapabilityAdapter() {
        return capabilityAdapter;
    }

    private static void initCapabilityAdapter(String gameVersion) {
        try {
            INSTANCE.setCapabilityAdapter((ICapabilityAdapter) Class.forName("ru.xlv.packetapi.capability.CapabilityAdapter" + gameVersion.replace(".", "_")).newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
