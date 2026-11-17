package net.korostopia.Peripheral.config;

import com.mojang.logging.LogUtils;
import net.korostopia.Peripheral.Peripheral;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.korostopia.Peripheral.debug.PeripheralDebugConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Peripheral.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PeripheralConfigEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CONFIG_SUMMARY =
            "Client config active: "
                    + "detectionRange={} blocks, radarRange={} blocks, maxTargets={}, opacity={}, "
                    + "showInMenus={}, screenExclusions={}, allAroundRadar={}, showThroughWalls={}, "
                    + "behindColor={}, frontColor={}, glowAmount={}, minSizeDistance={}, "
                    + "worldSmoothingSpeed={}, hudMovementSmoothingSpeed={}, "
                    + "opacityInterpolationSpeed={}, fadeInDurationMs={}, fadeOutDurationMs={}, "
                    + "includeMobs={}, excludeMobs={}";

    private PeripheralConfigEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        logIfPeripheral(event.getConfig());
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        logIfPeripheral(event.getConfig());
    }

    private static void logIfPeripheral(ModConfig config) {
        if ((config.getSpec() != PeripheralConfig.SPEC
                && config.getSpec() != PeripheralDebugConfig.SPEC)
                || !PeripheralConfig.SPEC.isLoaded()
                || !PeripheralDebugConfig.SPEC.isLoaded()) {
            return;
        }

        PeripheralDebug.log(
                "logConfigSummary",
                CONFIG_SUMMARY,
                PeripheralConfig.DETECTION_RANGE.get(),
                PeripheralConfig.RADAR_RANGE.get(),
                PeripheralConfig.MAX_TARGETS.get(),
                PeripheralConfig.OPACITY.get(),
                PeripheralConfig.SHOW_IN_MENUS.get(),
                PeripheralConfig.SCREEN_EXCLUSIONS.get(),
                PeripheralConfig.ENABLE_FRONT_HEMISPHERE.get(),
                PeripheralConfig.SHOW_THROUGH_WALLS.get(),
                PeripheralConfig.HOLOGRAM_COLOR.get(),
                PeripheralConfig.FRONT_HOLOGRAM_COLOR.get(),
                PeripheralConfig.GLOW_AMOUNT.get(),
                PeripheralConfig.MIN_SIZE_DISTANCE.get(),
                PeripheralConfig.SMOOTHING_SPEED.get(),
                PeripheralConfig.HUD_MOVEMENT_SMOOTHING_SPEED.get(),
                PeripheralConfig.OPACITY_INTERPOLATION_SPEED.get(),
                PeripheralConfig.FADE_IN_DURATION_MS.get(),
                PeripheralConfig.FADE_OUT_DURATION_MS.get(),
                PeripheralConfig.INCLUDE_MOBS.get(),
                PeripheralConfig.EXCLUDE_MOBS.get()
        );

        warnUnknownEntries("includeMobs", PeripheralConfig.INCLUDE_MOBS.get());
        warnUnknownEntries("excludeMobs", PeripheralConfig.EXCLUDE_MOBS.get());
    }

    private static void warnUnknownEntries(
            String settingName,
            List<? extends String> configuredIds
    ) {
        for (String configuredId : configuredIds) {
            if (configuredId == null || configuredId.isBlank()) {
                continue;
            }

            String id = configuredId.trim().toLowerCase(Locale.ROOT);
            if (id.endsWith(":*")) {
                warnUnknownNamespace(settingName, configuredId, id.substring(0, id.length() - 2));
                continue;
            }

            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !ForgeRegistries.ENTITY_TYPES.containsKey(key)) {
                LOGGER.warn(
                        "Peripheral entityDetection.{} contains '{}', but that entity type is not registered.",
                        settingName,
                        configuredId
                );
            }
        }
    }

    private static void warnUnknownNamespace(
            String settingName,
            String configuredId,
            String namespace
    ) {
        boolean namespaceExists = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                .anyMatch(key -> key.getNamespace().equals(namespace));

        if (!namespaceExists) {
            LOGGER.warn(
                    "Peripheral entityDetection.{} contains wildcard '{}', but no entity types use namespace '{}'.",
                    settingName,
                    configuredId,
                    namespace
            );
        }
    }
}
