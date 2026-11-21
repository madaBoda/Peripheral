package net.korostopia.Peripheral.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Client only stuff, interface, and diagnostic settings. */
public final class PeripheralClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue OPACITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_IN_MENUS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SCREEN_EXCLUSIONS;
    public static final ForgeConfigSpec.DoubleValue SMOOTHING_SPEED;
    public static final ForgeConfigSpec.DoubleValue HUD_MOVEMENT_SMOOTHING_SPEED;
    public static final ForgeConfigSpec.DoubleValue OPACITY_INTERPOLATION_SPEED;
    public static final ForgeConfigSpec.IntValue FADE_IN_DURATION_MS;
    public static final ForgeConfigSpec.IntValue FADE_OUT_DURATION_MS;
    public static final ForgeConfigSpec.DoubleValue MIN_SIZE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue GLOW_AMOUNT;
    public static final ForgeConfigSpec.ConfigValue<String> HOLOGRAM_COLOR;
    public static final ForgeConfigSpec.ConfigValue<String> FRONT_HOLOGRAM_COLOR;

    public static final ForgeConfigSpec.BooleanValue DEBUG_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_SCREEN_CLASSES;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_DETECTION_RANGE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_ENTITY_PROBES;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_ENTITY_DECISIONS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_RENDERER_COMPATIBILITY;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG_CONFIG_SUMMARY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("holograms");

        OPACITY = builder
                .comment("Lower values are more transparent.")
                .defineInRange("opacity", 1D, 0.05D, 1D);

        SHOW_IN_MENUS = builder
                .comment("Draw holograms over open menus.",
                        "Will probably need a lot of configuring to not get in the way of some menus, especially menus added from mods.")
                .define("showInMenus", false);

        SCREEN_EXCLUSIONS = builder
                .comment(
                        "Screens excluded from menu rendering. Use a full class name or a package wildcard.",
                        "A listed superclass also excludes its subclasses. Leave empty to allow all screens (not recommended)"
                )
                .defineListAllowEmpty("screenExclusions", List.of(), PeripheralClientConfig::isValidScreenExclusion);

        SMOOTHING_SPEED = builder
                .comment("Higher values follow entities faster.")
                .defineInRange("smoothingSpeed", 24.0D, 1.0D, 60.0D);

        HOLOGRAM_COLOR = builder
                .comment("hologram tint in #RRGGBB format.")
                .define("color", "#FF332E", PeripheralClientConfig::isValidColor);

        FRONT_HOLOGRAM_COLOR = builder
                .comment("hologram tint in #RRGGBB format.")
                .define("frontColor", "#42D9FF", PeripheralClientConfig::isValidColor);

        MIN_SIZE_DISTANCE = builder
                .comment("Minimum distance used for hologram sizing. Set to 0 to disable the limit.")
                .defineInRange("minSizeDistance", 2.0D, 0.0D, 32.0D);

        GLOW_AMOUNT = builder
                .comment("Glow strength. Set to 0 to disable the halo.")
                .defineInRange("glowAmount", 0.0D, 0.0D, 1.0D);

        HUD_MOVEMENT_SMOOTHING_SPEED = builder
                .comment("Higher values follow projected positions faster.")
                .defineInRange("hudMovementSmoothingSpeed", 36.0D, 1.0D, 60.0D);

        OPACITY_INTERPOLATION_SPEED = builder
                .comment("Opacity interpolation speed while a hologram is active.")
                .defineInRange("opacityInterpolationSpeed", 18.0D, 1.0D, 60.0D);

        FADE_IN_DURATION_MS = builder
                .comment("Fade in duration for new holograms in milliseconds.")
                .defineInRange("fadeInDurationMs", 220, 0, 3000);

        FADE_OUT_DURATION_MS = builder
                .comment("Fade out duration for a hologram in milliseconds.")
                .defineInRange("fadeOutDurationMs", 180, 0, 3000);

        builder.pop();

        builder.push("debug");

        DEBUG_ENABLED = builder
                .comment(
                        "Master switch for diagnostic logging.")
                .define("enabled", false);

        DEBUG_LOG_SCREEN_CLASSES = builder
                .comment("Log the Java class name whenever a screen opens.")
                .define("logScreenClasses", false);

        DEBUG_LOG_DETECTION_RANGE = builder
                .comment("Log changes to the effective threat detection range.")
                .define("logDetectionRange", false);

        DEBUG_LOG_ENTITY_PROBES = builder
                .comment("Log nearby modded entity IDs, classes, renderers, and hostility signals.")
                .define("logEntityProbes", false);

        DEBUG_LOG_ENTITY_DECISIONS = builder
                .comment("Log why a modded entity was accepted or ignored.")
                .define("logEntityDecisions", false);

        DEBUG_LOG_RENDERER_COMPATIBILITY = builder
                .comment("Log renderer selection and fallback decisions.")
                .define("logRendererCompatibility", false);

        DEBUG_LOG_CONFIG_SUMMARY = builder
                .comment("Log the active gameplay settings when the config loads or reloads.")
                .define("logConfigSummary", false);

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

    private PeripheralClientConfig() {
    }
}
