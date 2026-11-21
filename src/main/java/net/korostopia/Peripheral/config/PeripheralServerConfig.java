package net.korostopia.Peripheral.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Server stuff. */
public final class PeripheralServerConfig {
    private static final int DEFAULT_DETECTION_RANGE = 12;
    private static final int DEFAULT_RADAR_RANGE = 5;
    private static final int DEFAULT_MAX_TARGETS = 8;
    private static final boolean DEFAULT_LISTEN_MODE = false;
    private static final boolean DEFAULT_REQUIRE_CROUCH_TO_LISTEN = true;
    private static final boolean DEFAULT_ALL_AROUND_RADAR = true;
    private static final boolean DEFAULT_SHOW_THROUGH_WALLS = true;

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue DETECTION_RANGE;
    public static final ForgeConfigSpec.IntValue RADAR_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_TARGETS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRONT_HEMISPHERE;
    public static final ForgeConfigSpec.BooleanValue SHOW_THROUGH_WALLS;
    public static final ForgeConfigSpec.BooleanValue LISTEN_MODE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_CROUCH_TO_LISTEN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INCLUDE_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDE_MOBS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("detection");

        DETECTION_RANGE = builder
                .comment("Maximum distance in blocks for threat detection.")
                .defineInRange("detectionRange", DEFAULT_DETECTION_RANGE, 1, 128);

        RADAR_RANGE = builder
                .comment(
                        "Maximum distance for threats hidden by blocks while Listen is inactive.",
                        "mobs from behind use the full detectionRange."
                )
                .defineInRange("radarRange", DEFAULT_RADAR_RANGE, 1, 128);

        MAX_TARGETS = builder
                .comment("Maximum number of visible holograms and nearest threats take priority.")
                .defineInRange("maxTargets", DEFAULT_MAX_TARGETS, 1, 24);

        ENABLE_FRONT_HEMISPHERE = builder
                .comment("controls whether front facing threats are considered outside Listen Mode.")
                .define("allAroundRadar", DEFAULT_ALL_AROUND_RADAR);

        SHOW_THROUGH_WALLS = builder
                .comment("Allow threats hidden by blocks when they are inside radarRange outside Listen Mode.")
                .define("showThroughWalls", DEFAULT_SHOW_THROUGH_WALLS);

        INCLUDE_MOBS = builder
                .comment(
                        "Entity registry IDs that are always treated as threats.",
                        "Use full IDs such as \"minecraft:creeper\" or \"examplemod:infected\".",
                        "A namespace wildcard such as \"examplemod:*\" includes all living entities from that mod."
                )
                .defineListAllowEmpty("includeMobs", List.of(), PeripheralServerConfig::isValidEntityId);

        EXCLUDE_MOBS = builder
                .comment(
                        "Entity registry IDs that are never treated as threats.",
                        "Exclusions take priority over includeMobs and automatic detection.",
                        "Use a full ID or a namespace wildcard such as \"examplemod:*\"."
                )
                .defineListAllowEmpty("excludeMobs", List.of(), PeripheralServerConfig::isValidEntityId);

        builder.pop();

        builder.push("listen");

        LISTEN_MODE = builder
                .comment(
                        "When enabled, holograms are hidden until a player holds the Listen key.",
                        "While listening, all mobs inside detectionRange are shown regardless of direction or line of sight."
                )
                .define("enabled", DEFAULT_LISTEN_MODE);

        REQUIRE_CROUCH_TO_LISTEN = builder
                .comment("Require the player to be crouching while holding the Listen key.")
                .define("requireCrouchToListen", DEFAULT_REQUIRE_CROUCH_TO_LISTEN);

        builder.pop();
        SPEC = builder.build();
    }

    public static int detectionRange() {
        return SPEC.isLoaded() ? DETECTION_RANGE.get() : DEFAULT_DETECTION_RANGE;
    }

    public static int radarRange() {
        return SPEC.isLoaded() ? RADAR_RANGE.get() : DEFAULT_RADAR_RANGE;
    }

    public static int maxTargets() {
        return SPEC.isLoaded() ? MAX_TARGETS.get() : DEFAULT_MAX_TARGETS;
    }

    public static boolean includeFrontHemisphere() {
        return SPEC.isLoaded() ? ENABLE_FRONT_HEMISPHERE.get() : DEFAULT_ALL_AROUND_RADAR;
    }

    public static boolean showThroughWalls() {
        return SPEC.isLoaded() ? SHOW_THROUGH_WALLS.get() : DEFAULT_SHOW_THROUGH_WALLS;
    }

    public static boolean listenEnabled() {
        return SPEC.isLoaded() ? LISTEN_MODE.get() : DEFAULT_LISTEN_MODE;
    }

    public static boolean requireCrouchToListen() {
        return SPEC.isLoaded() ? REQUIRE_CROUCH_TO_LISTEN.get() : DEFAULT_REQUIRE_CROUCH_TO_LISTEN;
    }

    public static List<? extends String> includedMobs() {
        return SPEC.isLoaded() ? INCLUDE_MOBS.get() : List.of();
    }

    public static List<? extends String> excludedMobs() {
        return SPEC.isLoaded() ? EXCLUDE_MOBS.get() : List.of();
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

    private PeripheralServerConfig() {
    }
}
