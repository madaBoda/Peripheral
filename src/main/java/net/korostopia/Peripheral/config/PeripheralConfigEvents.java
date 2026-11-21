package net.korostopia.Peripheral.config;

import com.mojang.logging.LogUtils;
import net.korostopia.Peripheral.Peripheral;
import net.korostopia.Peripheral.debug.PeripheralDebug;
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
        if (config.getSpec() != PeripheralClientConfig.SPEC
                && config.getSpec() != PeripheralServerConfig.SPEC) {
            return;
        }

        if (!PeripheralClientConfig.SPEC.isLoaded()) {
            return;
        }

        if (PeripheralDebug.isEnabled("logConfigSummary")) {
            PeripheralDebug.log(
                    "logConfigSummary",
                    "Client config active: opacity={}, showInMenus={}, screenExclusions={}, behindColor={}, "
                            + "frontColor={}, glowAmount={}, minSizeDistance={}, worldSmoothingSpeed={}, "
                            + "hudMovementSmoothingSpeed={}, opacityInterpolationSpeed={}, "
                            + "fadeInDurationMs={}, fadeOutDurationMs={}",
                    PeripheralClientConfig.OPACITY.get(),
                    PeripheralClientConfig.SHOW_IN_MENUS.get(),
                    PeripheralClientConfig.SCREEN_EXCLUSIONS.get(),
                    PeripheralClientConfig.HOLOGRAM_COLOR.get(),
                    PeripheralClientConfig.FRONT_HOLOGRAM_COLOR.get(),
                    PeripheralClientConfig.GLOW_AMOUNT.get(),
                    PeripheralClientConfig.MIN_SIZE_DISTANCE.get(),
                    PeripheralClientConfig.SMOOTHING_SPEED.get(),
                    PeripheralClientConfig.HUD_MOVEMENT_SMOOTHING_SPEED.get(),
                    PeripheralClientConfig.OPACITY_INTERPOLATION_SPEED.get(),
                    PeripheralClientConfig.FADE_IN_DURATION_MS.get(),
                    PeripheralClientConfig.FADE_OUT_DURATION_MS.get()
            );

            if (PeripheralServerConfig.SPEC.isLoaded()) {
                PeripheralDebug.log(
                        "logConfigSummary",
                        "Server config active: detectionRange={} blocks, radarRange={} blocks, maxTargets={}, "
                                + "allAroundRadar={}, showThroughWalls={}, listen={}, requireCrouchToListen={}, "
                                + "includeMobs={}, excludeMobs={}",
                        PeripheralServerConfig.detectionRange(),
                        PeripheralServerConfig.radarRange(),
                        PeripheralServerConfig.maxTargets(),
                        PeripheralServerConfig.includeFrontHemisphere(),
                        PeripheralServerConfig.showThroughWalls(),
                        PeripheralServerConfig.listenEnabled(),
                        PeripheralServerConfig.requireCrouchToListen(),
                        PeripheralServerConfig.includedMobs(),
                        PeripheralServerConfig.excludedMobs()
                );
            }
        }

        if (PeripheralServerConfig.SPEC.isLoaded()) {
            warnUnknownEntries("includeMobs", PeripheralServerConfig.includedMobs());
            warnUnknownEntries("excludeMobs", PeripheralServerConfig.excludedMobs());
        }
    }

    private static void warnUnknownEntries(String settingName, List<? extends String> configuredIds) {
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
                        "Peripheral detection.{} contains '{}', but that entity type is not registered.",
                        settingName,
                        configuredId
                );
            }
        }
    }

    private static void warnUnknownNamespace(String settingName, String configuredId, String namespace) {
        boolean namespaceExists = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                .anyMatch(key -> key.getNamespace().equals(namespace));

        if (!namespaceExists) {
            LOGGER.warn(
                    "Peripheral detection.{} contains wildcard '{}', but no entity types use namespace '{}'.",
                    settingName,
                    configuredId,
                    namespace
            );
        }
    }

}
