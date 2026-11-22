package com.example.pbrskin;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class PbrSkinTextures {
    public static final Identifier SKIN_ID = Identifier.fromNamespaceAndPath(PbrSkinMod.ID, "skin");
    public static final Identifier NORMAL_ID = Identifier.fromNamespaceAndPath(PbrSkinMod.ID, "skin_n");
    public static final Identifier SPECULAR_ID = Identifier.fromNamespaceAndPath(PbrSkinMod.ID, "skin_s");

    private static final int FLAT_NORMAL = 0xFF8080FF;
    private static final int NO_SPECULAR = 0xFF000000;
    private static final String NOT_SET = "not set";

    private static final int[][] LEGACY_COPIES = {
        {4, 16, 16, 32, 4, 4},
        {8, 16, 16, 32, 4, 4},
        {0, 20, 24, 32, 4, 12},
        {4, 20, 16, 32, 4, 12},
        {8, 20, 8, 32, 4, 12},
        {12, 20, 16, 32, 4, 12},
        {44, 16, -8, 32, 4, 4},
        {48, 16, -8, 32, 4, 4},
        {40, 20, 0, 32, 4, 12},
        {44, 20, -8, 32, 4, 12},
        {48, 20, -16, 32, 4, 12},
        {52, 20, -8, 32, 4, 12}
    };

    private static DynamicTexture skin;
    private static DynamicTexture normal;
    private static DynamicTexture specular;
    private static final String[] status = {NOT_SET, NOT_SET, NOT_SET};
    private static String message = "";
    private static long stamp = -1;
    private static int counter = 0;

    private PbrSkinTextures() {}

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(PbrSkinMod.ID);
    }

    public static void ensureFolder() {
        try {
            Files.createDirectories(dir());
        } catch (IOException e) {
            PbrSkinMod.LOGGER.error("Could not create {}", dir(), e);
        }
    }

    public static boolean isActive() {
        return skin != null;
    }

    public static boolean hasPbr() {
        return skin != null && normal != null && specular != null;
    }

    public static DynamicTexture skinTexture() {
        return skin;
    }

    public static DynamicTexture normalTexture() {
        return normal;
    }

    public static DynamicTexture specularTexture() {
        return specular;
    }

    public static String status(Slot slot) {
        return status[slot.ordinal()];
    }

    public static String message() {
        return message;
    }

    public static void setMessage(String text) {
        message = text;
    }

    public static void tick() {
        if (++counter < 20) {
            return;
        }
        counter = 0;
        long now = fileStamp();
        if (now != stamp) {
            stamp = now;
            reload();
        }
    }

    private static long fileStamp() {
        long sum = 0;
        for (Slot slot : Slot.values()) {
            Path p = dir().resolve(slot.file);
            try {
                if (Files.isRegularFile(p)) {
                    sum += Files.getLastModifiedTime(p).toMillis() + Files.size(p);
                }
            } catch (IOException ignored) {
            }
        }
        return sum;
    }

    public static void reload() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            doReload();
        } else {
            mc.execute(PbrSkinTextures::doReload);
        }
    }

    private static void doReload() {
        release(SKIN_ID);
        release(NORMAL_ID);
        release(SPECULAR_ID);
        skin = null;
        normal = null;
        specular = null;
        for (int i = 0; i < status.length; i++) {
            status[i] = NOT_SET;
        }

        NativeImage skinImage = prepare(read(dir().resolve(Slot.COLOR.file)), Slot.COLOR, 0);
        if (skinImage == null) {
            for (Slot slot : new Slot[] {Slot.NORMAL, Slot.SPECULAR}) {
                if (Files.isRegularFile(dir().resolve(slot.file))) {
                    status[slot.ordinal()] = "waiting for a colormap";
                }
            }
            stamp = fileStamp();
            IrisBridge.refresh();
            return;
        }
        int size = skinImage.getWidth();
        status[Slot.COLOR.ordinal()] = size + "x" + size;

        NativeImage normalImage = prepare(read(dir().resolve(Slot.NORMAL.file)), Slot.NORMAL, size);
        NativeImage specularImage = prepare(read(dir().resolve(Slot.SPECULAR.file)), Slot.SPECULAR, size);
        if (normalImage != null) {
            status[Slot.NORMAL.ordinal()] = size + "x" + size;
        }
        if (specularImage != null) {
            status[Slot.SPECULAR.ordinal()] = size + "x" + size;
        }

        boolean pbr = normalImage != null || specularImage != null;
        if (pbr && normalImage == null) {
            normalImage = flat(size, FLAT_NORMAL);
            status[Slot.NORMAL.ordinal()] = "default (flat)";
        }
        if (pbr && specularImage == null) {
            specularImage = flat(size, NO_SPECULAR);
            status[Slot.SPECULAR.ordinal()] = "default (matte)";
        }

        skin = register(SKIN_ID, "skin", skinImage);
        if (pbr) {
            normal = register(NORMAL_ID, "skin_n", normalImage);
            specular = register(SPECULAR_ID, "skin_s", specularImage);
        }

        stamp = fileStamp();
        IrisBridge.refresh();
        PbrSkinMod.LOGGER.info("Loaded skin from {} (PBR maps: {})", dir(), pbr);
    }

    public static String importFile(Path src, Slot hint) {
        String name = src.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".png")) {
            return "Only .png files are supported: " + name;
        }
        String base = lower.substring(0, lower.length() - 4);
        Slot slot = hint;
        if (base.endsWith("_n") || base.endsWith("_normal")) {
            slot = Slot.NORMAL;
        } else if (base.endsWith("_s") || base.endsWith("_specular")) {
            slot = Slot.SPECULAR;
        }
        NativeImage check = read(src);
        if (check == null) {
            return "Could not read " + name;
        }
        int w = check.getWidth();
        int h = check.getHeight();
        check.close();
        if (!validSize(w, h)) {
            return name + " is " + w + "x" + h + ". Use 64x64, 128x128, 256x256 or the old 64x32.";
        }
        try {
            ensureFolder();
            Files.copy(src, dir().resolve(slot.file), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PbrSkinMod.LOGGER.error("Could not copy {}", src, e);
            return "Could not copy " + name;
        }
        reload();
        return slot.label + " set from " + name;
    }

    public static String clear(Slot slot) {
        try {
            Files.deleteIfExists(dir().resolve(slot.file));
        } catch (IOException e) {
            return "Could not delete " + slot.file;
        }
        reload();
        return slot.label + " cleared";
    }

    private static boolean validSize(int w, int h) {
        return w >= 64 && w % 64 == 0 && (h == w || h * 2 == w);
    }

    private static void release(Identifier id) {
        Minecraft.getInstance().getTextureManager().release(id);
    }

    private static DynamicTexture register(Identifier id, String label, NativeImage image) {
        DynamicTexture texture = new DynamicTexture(() -> PbrSkinMod.ID + "/" + label, image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return texture;
    }

    private static NativeImage read(Path p) {
        if (!Files.isRegularFile(p)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(p)) {
            return NativeImage.read(in);
        } catch (IOException e) {
            PbrSkinMod.LOGGER.error("Could not read {}", p, e);
            return null;
        }
    }

    private static NativeImage prepare(NativeImage raw, Slot slot, int requiredSize) {
        if (raw == null) {
            return null;
        }
        int w = raw.getWidth();
        int h = raw.getHeight();
        if (!validSize(w, h)) {
            message = slot.file + " is " + w + "x" + h + " and was ignored";
            PbrSkinMod.LOGGER.warn(message);
            raw.close();
            return null;
        }
        NativeImage image = h * 2 == w ? upgradeLegacy(raw) : raw;
        if (slot == Slot.COLOR) {
            makeBaseOpaque(image);
        }
        if (requiredSize > 0 && image.getWidth() != requiredSize) {
            message = slot.file + " must be the same size as skin.png and was ignored";
            PbrSkinMod.LOGGER.warn(message);
            image.close();
            return null;
        }
        return image;
    }

    private static NativeImage upgradeLegacy(NativeImage raw) {
        int w = raw.getWidth();
        int s = w / 64;
        NativeImage out = new NativeImage(w, w, true);
        out.copyFrom(raw);
        raw.close();
        out.fillRect(0, 32 * s, w, 32 * s, 0);
        for (int[] a : LEGACY_COPIES) {
            out.copyRect(a[0] * s, a[1] * s, a[2] * s, a[3] * s, a[4] * s, a[5] * s, true, false);
        }
        return out;
    }

    private static void makeBaseOpaque(NativeImage image) {
        int s = image.getWidth() / 64;
        opaque(image, 0, 0, 32 * s, 16 * s);
        opaque(image, 0, 16 * s, 64 * s, 32 * s);
        opaque(image, 16 * s, 48 * s, 48 * s, 64 * s);
    }

    private static void opaque(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                image.setPixel(x, y, image.getPixel(x, y) | 0xFF000000);
            }
        }
    }

    private static NativeImage flat(int size, int color) {
        NativeImage image = new NativeImage(size, size, false);
        image.fillRect(0, 0, size, size, color);
        return image;
    }
}
