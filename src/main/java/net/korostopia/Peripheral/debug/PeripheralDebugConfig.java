package net.korostopia.Peripheral.debug;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PeripheralDebugConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue LOG_SCREEN_CLASSES;
    public static final ForgeConfigSpec.BooleanValue LOG_DETECTION_RANGE;
    public static final ForgeConfigSpec.BooleanValue LOG_ENTITY_PROBES;
    public static final ForgeConfigSpec.BooleanValue LOG_ENTITY_DECISIONS;
    public static final ForgeConfigSpec.BooleanValue LOG_RENDERER_COMPATIBILITY;
    public static final ForgeConfigSpec.BooleanValue LOG_CONFIG_SUMMARY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("debug");

        ENABLED = builder
                .comment(
                        "Master switch for diagnostic logging.",
                        "Individual categories are ignored while this is disabled."
                )
                .define("enabled", false);

        LOG_SCREEN_CLASSES = builder
                .comment("Log the Java class name whenever a screen opens.")
                .define("logScreenClasses", false);

        LOG_DETECTION_RANGE = builder
                .comment("Log changes to the effective threat detection range.")
                .define("logDetectionRange", false);

        LOG_ENTITY_PROBES = builder
                .comment(
                        "Log nearby modded entity IDs, classes, renderers, and hostility signals."
                )
                .define("logEntityProbes", false);

        LOG_ENTITY_DECISIONS = builder
                .comment("Log why a modded entity was accepted or ignored.")
                .define("logEntityDecisions", false);

        LOG_RENDERER_COMPATIBILITY = builder
                .comment("Log renderer selection and fallback decisions.")
                .define("logRendererCompatibility", false);

        LOG_CONFIG_SUMMARY = builder
                .comment("Log the active gameplay settings when the config loads or reloads.")
                .define("logConfigSummary", false);

        builder.pop();
        SPEC = builder.build();
    }

    private PeripheralDebugConfig() {
    }
}
