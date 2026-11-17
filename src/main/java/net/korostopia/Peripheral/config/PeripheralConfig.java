package net.korostopia.Peripheral.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class PeripheralConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue DETECTION_RANGE;
    public static final ForgeConfigSpec.IntValue RADAR_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_TARGETS;
    public static final ForgeConfigSpec.DoubleValue OPACITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_MENUS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCREEN_EXCLUSIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRONT_HEMISPHERE;
    public static final ForgeConfigSpec.BooleanValue SHOW_THROUGH_WALLS;
    public static final ForgeConfigSpec.DoubleValue SMOOTHING_SPEED;
    public static final ForgeConfigSpec.DoubleValue HUD_MOVEMENT_SMOOTHING_SPEED;
    public static final ForgeConfigSpec.DoubleValue OPACITY_INTERPOLATION_SPEED;
    public static final ForgeConfigSpec.IntValue FADE_IN_DURATION_MS;
    public static final ForgeConfigSpec.IntValue FADE_OUT_DURATION_MS;
    public static final ForgeConfigSpec.DoubleValue MIN_SIZE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue GLOW_AMOUNT;

    public static final ForgeConfigSpec.ConfigValue<String> HOLOGRAM_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> FRONT_HOLOGRAM_COLOR;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INCLUDE_MOBS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDE_MOBS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("holograms");

        DETECTION_RANGE = builder
                .comment(
                        "Maximum 3D distance, in blocks, for threat detection.",
                        "Manual edits to peripheral-client.toml require a game restart."
                )
                .defineInRange("detectionRange", 48, 1, 128);

        RADAR_RANGE = builder
                .comment(
                        "Maximum distance for threats hidden by blocks.",
                        "Visible rear threats may use the full detectionRange."
                )
                .defineInRange("radarRange", 16, 1, 128);

        MAX_TARGETS = builder
                .comment("Maximum number of visible holograms. Nearest threats take priority.")
                .defineInRange("maxTargets", 8, 1, 24);

        OPACITY = builder
                .comment("Base hologram opacity. Lower values are more transparent.")
                .defineInRange("opacity", 0.40D, 0.05D, 1.0D);

        SHOW_IN_MENUS = builder
                .comment(
                        "Draw eligible holograms over open menus.",
                        "Hidden threats still require showThroughWalls and must be inside radarRange."
                )
                .define("showInMenus", false);

        SCREEN_EXCLUSIONS = builder
                .comment(
                        "Screens excluded from menu rendering. Use a full class name or a package wildcard.",
                        "A listed superclass also excludes its subclasses. Leave empty to allow all screens."
                )
                .defineListAllowEmpty("screenExclusions", List.of(), PeripheralConfig::isValidScreenExclusion);

        ENABLE_FRONT_HEMISPHERE = builder
                .comment(
                        "Controls whether front-facing threats are considered.",
                        "Visible front threats appear over menus; hidden front threats still require radarRange."
                )
                .define("allAroundRadar", true);

        SHOW_THROUGH_WALLS = builder
                .comment("Allow threats hidden by blocks when they are inside radarRange.")
                .define("showThroughWalls", true);

        SMOOTHING_SPEED = builder
                .comment("World-space movement smoothing. Higher values follow entities faster.")
                .defineInRange("smoothingSpeed", 24.0D, 1.0D, 60.0D);

        HOLOGRAM_COLOR = builder
                .comment("Behind-player hologram tint in #RRGGBB format.")
                .define("color", "#FF332E", PeripheralConfig::isValidColor);

        FRONT_HOLOGRAM_COLOR = builder
                .comment("Front-player hologram tint in #RRGGBB format.")
                .define("frontColor", "#42D9FF", PeripheralConfig::isValidColor);

        MIN_SIZE_DISTANCE = builder
                .comment("Minimum distance used for hologram sizing. Set to 0 to disable the limit.")
                .defineInRange("minSizeDistance", 4.0D, 0.0D, 32.0D);

        GLOW_AMOUNT = builder
                .comment("Glow strength. Set to 0 to disable the halo.")
                .defineInRange("glowAmount", 0.28D, 0.0D, 1.0D);

        HUD_MOVEMENT_SMOOTHING_SPEED = builder
                .comment("Screen-space movement smoothing. Higher values follow projected positions faster.")
                .defineInRange("hudMovementSmoothingSpeed", 36.0D, 1.0D, 60.0D);

        OPACITY_INTERPOLATION_SPEED = builder
                .comment("Opacity interpolation speed while a hologram is active.")
                .defineInRange("opacityInterpolationSpeed", 18.0D, 1.0D, 60.0D);

        FADE_IN_DURATION_MS = builder
                .comment("Fade-in duration for new holograms, in milliseconds.")
                .defineInRange("fadeInDurationMs", 220, 0, 3000);

        FADE_OUT_DURATION_MS = builder
                .comment("Fade-out duration when a hologram stops qualifying, in milliseconds.")
                .defineInRange("fadeOutDurationMs", 180, 0, 3000);

        builder.pop();

        builder.push("entityDetection");

        INCLUDE_MOBS = builder
                .comment(
                        "Entity registry IDs that are always treated as threats.",
                        "Use full IDs such as \"minecraft:creeper\" or \"examplemod:infected\".",
                        "A namespace wildcard such as \"examplemod:*\" includes all living entities from that mod.",
                        "Edit this list in peripheral-client.toml; no datapack is needed."
                )
                .defineListAllowEmpty("includeMobs", List.of(), PeripheralConfig::isValidEntityId);

        EXCLUDE_MOBS = builder
                .comment(
                        "Entity registry IDs that are never treated as threats.",
                        "Exclusions take priority over includeMobs and automatic detection.",
                        "Use a full ID or a namespace wildcard such as \"examplemod:*\"."
                )
                .defineListAllowEmpty("excludeMobs", List.of(), PeripheralConfig::isValidEntityId);

        builder.pop();
        SPEC = builder.build();
    }

    private static boolean isValidColor(Object value) {
        return value instanceof String color && color.matches("#[0-9a-fA-F]{6}");
    }

    private static boolean isValidScreenExclusion(Object value) {
        if (!(value instanceof String pattern) || pattern.isBlank()) {
            return false;
        }
        return pattern.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+(\\.\\*)?");
    }

    private static boolean isValidEntityId(Object value) {
        if (!(value instanceof String id) || id.isBlank() || id.indexOf(':') <= 0) {
            return false;
        }
        String valueToValidate = id.endsWith(":*")
                ? id.substring(0, id.length() - 1) + "peripheral_wildcard"
                : id;
        return ResourceLocation.isValidResourceLocation(valueToValidate);
    }

    private PeripheralConfig() {
    }
}
