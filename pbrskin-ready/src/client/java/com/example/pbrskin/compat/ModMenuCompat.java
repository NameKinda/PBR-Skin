package com.example.pbrskin.compat;

import com.example.pbrskin.screen.PbrSkinScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PbrSkinScreen::new;
    }
}
