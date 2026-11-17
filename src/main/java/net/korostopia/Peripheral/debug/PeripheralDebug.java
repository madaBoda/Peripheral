package net.korostopia.Peripheral.debug;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PeripheralDebug {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PeripheralDebug() {
    }

    public static void prepareConfigDirectory() {
        Path directory = FMLPaths.CONFIGDIR.get().resolve("peripheral").resolve("debug");
        try {
            Files.createDirectories(directory);
        } catch (IOException | SecurityException exception) {
            LOGGER.error(
                    "Could not create Peripheral's debug config directory at {}. "
                            + "The debug config may fail to load.",
                    directory,
                    exception
            );
        }
    }

    public static boolean isEnabled(String category) {
        if (!PeripheralDebugConfig.SPEC.isLoaded()) {
            return false;
        }

        if (!PeripheralDebugConfig.ENABLED.get()) {
            return false;
        }

        return switch (category) {
            case "logScreenClasses" -> PeripheralDebugConfig.LOG_SCREEN_CLASSES.get();
            case "logDetectionRange" -> PeripheralDebugConfig.LOG_DETECTION_RANGE.get();
            case "logEntityProbes" -> PeripheralDebugConfig.LOG_ENTITY_PROBES.get();
            case "logEntityDecisions" -> PeripheralDebugConfig.LOG_ENTITY_DECISIONS.get();
            case "logRendererCompatibility" -> PeripheralDebugConfig.LOG_RENDERER_COMPATIBILITY.get();
            case "logConfigSummary" -> PeripheralDebugConfig.LOG_CONFIG_SUMMARY.get();
            default -> false;
        };
    }

    public static void log(String category, String message, Object... arguments) {
        if (isEnabled(category)) {
            LOGGER.info("[Peripheral debug/{}] " + message, prependCategory(category, arguments));
        }
    }

    private static Object[] prependCategory(String category, Object[] arguments) {
        Object[] result = new Object[arguments.length + 1];
        result[0] = category;
        System.arraycopy(arguments, 0, result, 1, arguments.length);
        return result;
    }
}
