package com.example.pbrskin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PbrSkinSettings {
    private static final String[] MODELS = {"auto", "classic", "slim"};
    private static int index = 0;

    private PbrSkinSettings() {}

    private static Path file() {
        return PbrSkinTextures.dir().resolve("settings.properties");
    }

    public static void load() {
        Path p = file();
        if (!Files.isRegularFile(p)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException e) {
            PbrSkinMod.LOGGER.error("Could not read {}", p, e);
            return;
        }
        String model = props.getProperty("model", "auto");
        for (int i = 0; i < MODELS.length; i++) {
            if (MODELS[i].equals(model)) {
                index = i;
            }
        }
    }

    public static void cycleModel() {
        index = (index + 1) % MODELS.length;
        save();
    }

    public static void reset() {
        index = 0;
        save();
    }

    private static void save() {
        Properties props = new Properties();
        props.setProperty("model", MODELS[index]);
        try (OutputStream out = Files.newOutputStream(file())) {
            props.store(out, "PBR Skin settings");
        } catch (IOException e) {
            PbrSkinMod.LOGGER.error("Could not write {}", file(), e);
        }
    }

    public static String modelLabel() {
        return switch (MODELS[index]) {
            case "classic" -> "Classic (wide arms)";
            case "slim" -> "Slim (thin arms)";
            default -> "Auto (from account)";
        };
    }

    public static <E extends Enum<E>> E apply(E original) {
        String name = switch (MODELS[index]) {
            case "classic" -> "WIDE";
            case "slim" -> "SLIM";
            default -> null;
        };
        if (name == null) {
            return original;
        }
        try {
            return Enum.valueOf(original.getDeclaringClass(), name);
        } catch (IllegalArgumentException e) {
            return original;
        }
    }
}
