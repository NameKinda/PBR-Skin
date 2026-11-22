package com.example.pbrskin.screen;

import com.example.pbrskin.PbrSkinSettings;
import com.example.pbrskin.PbrSkinTextures;
import com.example.pbrskin.Slot;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PbrSkinScreen extends Screen {
    private enum Tab {
        SKIN("Skin Files"),
        SETTINGS("Settings");

        final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private static final int MARGIN = 20;

    private final Screen parent;
    private Tab tab = Tab.SKIN;
    private Slot target = Slot.COLOR;
    private int previewX1;
    private int previewY1;
    private int previewX2;
    private int previewY2;

    public PbrSkinScreen(Screen parent) {
        super(Component.literal("PBR Skin Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = MARGIN;
        int previewWidth = Math.max(110, Math.min(170, this.width / 3));
        previewX2 = this.width - MARGIN;
        previewX1 = previewX2 - previewWidth;
        previewY1 = 58;
        previewY2 = this.height - 38;
        int listWidth = previewX1 - 14 - left;

        addRenderableWidget(new StringWidget(left, 10, listWidth, 12, this.title, this.font));

        int tabX = left;
        for (Tab t : Tab.values()) {
            Button button = Button.builder(Component.literal(t.title), b -> {
                        tab = t;
                        rebuildWidgets();
                    })
                    .bounds(tabX, 26, 110, 20)
                    .build();
            button.active = tab != t;
            addRenderableWidget(button);
            tabX += 114;
        }

        if (tab == Tab.SKIN) {
            buildSkinTab(left, previewY1, listWidth);
        } else {
            buildSettingsTab(left, previewY1, listWidth);
        }

        addRenderableWidget(new StringWidget(
                left, this.height - 46, listWidth, 12, Component.literal(PbrSkinTextures.message()), this.font));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left, this.height - 28, 120, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
                    PbrSkinSettings.reset();
                    PbrSkinTextures.setMessage("Settings reset");
                    rebuildWidgets();
                })
                .bounds(previewX2 - 120, this.height - 28, 120, 20)
                .build());
    }

    private void buildSkinTab(int left, int top, int listWidth) {
        int y = top;
        for (Slot slot : Slot.values()) {
            String mark = slot == target ? "> " : "   ";
            String need = slot.required ? " (required)" : " (optional)";
            Component text = Component.literal(mark + slot.label + need + ": " + PbrSkinTextures.status(slot));
            addRenderableWidget(Button.builder(text, b -> {
                        target = slot;
                        rebuildWidgets();
                    })
                    .bounds(left, y, listWidth - 59, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                        PbrSkinTextures.setMessage(PbrSkinTextures.clear(slot));
                        rebuildWidgets();
                    })
                    .bounds(left + listWidth - 55, y, 55, 20)
                    .build());
            y += 24;
        }

        y += 6;
        String[] hints = {
            "Drag a .png onto this window to fill the > slot.",
            "Names ending in _n or _s go to Normal or Specular.",
            "Sizes: 64x64, 128x128, 256x256 or the old 64x32.",
            "Only the colormap is needed. The maps are optional."
        };
        for (String hint : hints) {
            addRenderableWidget(new StringWidget(left, y, listWidth, 12, Component.literal(hint), this.font));
            y += 12;
        }
    }

    private void buildSettingsTab(int left, int top, int listWidth) {
        int y = top;
        addRenderableWidget(Button.builder(Component.literal("Model: " + PbrSkinSettings.modelLabel()), b -> {
                    PbrSkinSettings.cycleModel();
                    rebuildWidgets();
                })
                .bounds(left, y, listWidth, 20)
                .build());
        y += 24;

        int half = (listWidth - 4) / 2;
        addRenderableWidget(Button.builder(Component.literal("Open folder"), b -> {
                    PbrSkinTextures.ensureFolder();
                    Util.getPlatform().openPath(PbrSkinTextures.dir());
                })
                .bounds(left, y, half, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reload"), b -> {
                    PbrSkinTextures.reload();
                    PbrSkinTextures.setMessage("Reloaded");
                    rebuildWidgets();
                })
                .bounds(left + half + 4, y, half, 20)
                .build());
        y += 30;

        String[] hints = {
            "Auto uses the arm type from your account.",
            "Only you see this skin. Other players see",
            "your normal account skin.",
            "The preview shows the colormap only.",
            "Shine and flat areas show in the world with a shader."
        };
        for (String hint : hints) {
            addRenderableWidget(new StringWidget(left, y, listWidth, 12, Component.literal(hint), this.font));
            y += 12;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(previewX1, previewY1, previewX2, previewY2, 0xFF000000);

        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            graphics.text(
                    this.font,
                    Component.literal("Join a world to see the preview"),
                    previewX1 + 6,
                    previewY1 + 6,
                    0xFFAAAAAA);
            return;
        }

        float boxHeight = previewY2 - previewY1;
        float scale = boxHeight * 0.4F / player.getScale();
        float yaw = (float) ((System.currentTimeMillis() % 12000L) / 12000.0 * Math.PI * 2.0);
        EntityRenderState state = this.minecraft
                .getEntityRenderDispatcher()
                .getRenderer(player)
                .createRenderState(player, 1.0F);
        Vector3f translation = new Vector3f(0.0F, player.getBbHeight() / 2.0F + 0.0625F, 0.0F);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).rotateY(yaw);
        graphics.entity(state, scale, translation, rotation, new Quaternionf(), previewX1, previewY1, previewX2, previewY2);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        String result = "";
        for (Path path : paths) {
            result = PbrSkinTextures.importFile(path, target);
        }
        if (paths.size() > 1) {
            result = paths.size() + " files dropped. Last: " + result;
        }
        PbrSkinTextures.setMessage(result);
        tab = Tab.SKIN;
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
