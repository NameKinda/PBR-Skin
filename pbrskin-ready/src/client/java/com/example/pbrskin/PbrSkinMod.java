package com.example.pbrskin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PbrSkinMod implements ClientModInitializer {
    public static final String ID = "pbrskin";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    @Override
    public void onInitializeClient() {
        PbrSkinTextures.ensureFolder();
        PbrSkinSettings.load();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PbrSkinTextures.reload());
        ClientTickEvents.END_CLIENT_TICK.register(client -> PbrSkinTextures.tick());
    }
}
