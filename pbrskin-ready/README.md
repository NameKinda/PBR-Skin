# PBR Skin 0.3.0 (client-only, Fabric, Minecraft 26.3)

Use your own skin on your player model, and optionally add PBR maps so Iris shader packs render
parts of it shiny and parts of it flat. Only you see it for now (no multiplayer sync).
Other players see whatever skin your account normally uses.

## Using it in game

Open **Mod Menu > PBR Skin > the settings button**. The screen has two tabs, a live player preview on the
right, and Done and Reset buttons at the bottom.

**Skin Files tab**
- Click a slot (Colormap, Normal map, Specular map) to select it. The selected one is marked with `>`.
- **Drag a .png file onto the Minecraft window** to fill the selected slot.
  File names ending in `_n` or `_s` (or `_normal` / `_specular`) go to Normal or Specular automatically.
- **Clear** removes that slot's file.

**Settings tab**
- **Model** cycles Auto (from your account), Classic (wide arms) and Slim (thin arms).
- **Open folder** opens `.minecraft/config/pbrskin/`. You can also drop files there by hand.
- **Reload** re-reads the files. Changes to files in the folder are also picked up automatically.

**Reset** puts the settings back to default. It does not delete your skin files, use Clear for that.
The preview turns slowly and shows the colormap only. Shine and flat areas show in the world with a shader.

## What you need

Only the colormap. Normal and specular maps are optional:

| Slot | File | Needed? | If missing |
|---|---|---|---|
| Colormap | `skin.png` | yes | no custom skin, you keep your account skin |
| Normal map | `skin_n.png` | no | flat default |
| Specular map | `skin_s.png` | no | matte default (no shine) |

With a colormap alone, the mod shows your custom skin and does not touch Iris at all.
Add either map and PBR is switched on, with the other one filled in by a default.

## Supported skin types

- 64x64 modern skins, with Classic or Slim arms (set with the Model button).
- Old 64x32 skins. They are converted to 64x64 the same way Minecraft does it.
- High-resolution square skins: 128x128, 256x256 and so on.
- Normal and specular maps must be the same size as the colormap, or they are ignored and the screen says so.

Capes and elytra still come from your account.

## Painting the specular map (labPBR 1.3)

| Channel | Meaning | For shiny | For flat/matte |
|---|---|---|---|
| Red | Smoothness | high (200-255) | 0 |
| Green | Reflectance / metal | 230-255 = metal, 0-229 = non-metal | 0 |
| Blue | Porosity / SSS | leave 0 | 0 |
| Alpha | Emission (255 = none) | 255 | 255 |

Keep alpha at 255 unless you want that part to glow. Most image editors hide transparency, so check it.

## Building

This folder is a complete Fabric project for Minecraft 26.3. The jar is built by `gradlew build`
(or by GitHub, using `.github/workflows/build.yml`) and ends up in `build/libs/pbrskin-0.3.0.jar`.
Use the jar that does not end in `-sources`.

Put the jar in your instance with Fabric API, Mod Menu, Sodium and Iris (OpenGL backend, not Vulkan),
and use a labPBR shader pack. Mod Menu is only compiled against, it is not bundled.
If the build cannot find Mod Menu, change `modmenu_version` in `gradle.properties`.

All of the mod's code is in `src/client` because the project uses split client and common sources.

## What is checked and what is not

Checked against the Iris 26.3 source: `PBRTextureManager` has public `getHolder(int)` and `getOrLoadHolder(int)`
keyed by GL texture id, and `PBRTextureHolder` is a public interface with `normalTexture()` and
`specularTexture()`. The mod hooks both lookups and answers with its own holder for your skin's GL id.

Checked by reading the compiled class signatures of a published 26.3 mod (no code was copied):
`GuiGraphicsExtractor` with `fill`, `text` and `entity`; `Screen.extractRenderState(GuiGraphicsExtractor, int, int, float)`;
`Button.builder`; `CommonComponents.GUI_DONE`; `AbstractClientPlayer.getSkin()` returning `PlayerSkin`, whose `body()`
is a `ClientAsset.Texture` with `texturePath()`; `Identifier`; `NativeImage.read/getWidth/getHeight/close`;
`DynamicTexture.getPixels()`; the player preview calls (`getEntityRenderDispatcher().getRenderer(..).createRenderState(..)`);
and Mod Menu's `ModMenuApi.getModConfigScreenFactory()` returning `ConfigScreenFactory<?>`.

Not checked. A compile error or log message will point at these:

- `AbstractClientPlayerMixin`: the `PlayerSkin` constructor, `ClientAsset.ResourceTexture`, that `getSkin` is
  declared directly on `AbstractClientPlayer`, and that `original.model()` is an enum with `WIDE` and `SLIM`.
- `IrisBridge.glId`: it reads the GL id by reflection (method `getTexture()` on the texture, then `glId()`),
  because 26.3 moved its graphics classes (some are now under `com.mojang.renderpearl`). If the log says it could
  not read the id, the skin shows but is not shiny, and the log line names the class to look at.
- `PbrSkinScreen`: `Screen.onFilesDrop`, `StringWidget`'s six-argument constructor, `rebuildWidgets`,
  `Util.getPlatform().openPath`, and the exact `createRenderState` call chain.
- `PbrSkinTextures`: `NativeImage` methods (`copyFrom`, `copyRect`, `fillRect`, `getPixel`/`setPixel` as ARGB),
  and the `DynamicTexture` constructor and `TextureManager.register/release` signatures.

Iris is LGPL, and this mod only references its classes by name at runtime, so it does not need Iris to compile.
