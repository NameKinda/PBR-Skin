package com.example.pbrskin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;

public final class IrisBridge {
    private static final String HOLDER_INTERFACE = "net.irisshaders.iris.pbr.texture.PBRTextureHolder";
    private static final String[] ID_METHODS = {"glId", "getGlId", "id", "getId"};

    private static int skinGlId = -1;
    private static Object holder;
    private static boolean buildFailed;
    private static boolean warnedAboutId;

    private IrisBridge() {}

    public static void refresh() {
        holder = null;
        buildFailed = false;
        skinGlId = glId(PbrSkinTextures.skinTexture());
    }

    public static Object holderFor(int albedoGlId) {
        if (albedoGlId < 0 || albedoGlId != skinGlId || !PbrSkinTextures.hasPbr()) {
            return null;
        }
        if (holder == null && !buildFailed) {
            holder = build();
            buildFailed = holder == null;
        }
        return holder;
    }

    private static int glId(DynamicTexture texture) {
        if (texture == null) {
            return -1;
        }
        Object gpu = call(texture, "getTexture");
        if (gpu == null) {
            warnAboutId("DynamicTexture has no getTexture() method");
            return -1;
        }
        for (String name : ID_METHODS) {
            Object value = call(gpu, name);
            if (value instanceof Integer id) {
                return id;
            }
        }
        warnAboutId("no int id method found on " + gpu.getClass().getName());
        return -1;
    }

    private static Object call(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static void warnAboutId(String reason) {
        if (!warnedAboutId) {
            warnedAboutId = true;
            PbrSkinMod.LOGGER.warn("Could not read the OpenGL id of the skin texture ({}), PBR maps will not apply", reason);
        }
    }

    private static Object build() {
        try {
            Class<?> iface = Class.forName(HOLDER_INTERFACE, true, IrisBridge.class.getClassLoader());
            AbstractTexture normal = PbrSkinTextures.normalTexture();
            AbstractTexture specular = PbrSkinTextures.specularTexture();
            return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "normalTexture" -> normal;
                    case "specularTexture" -> specular;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "PbrSkinHolder";
                    default -> null;
                };
            });
        } catch (ClassNotFoundException e) {
            PbrSkinMod.LOGGER.warn("Iris PBR classes not found, PBR maps will not be applied");
        } catch (RuntimeException e) {
            PbrSkinMod.LOGGER.error("Could not build Iris PBR holder", e);
        }
        return null;
    }
}
