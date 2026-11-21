package net.korostopia.Peripheral.debug;

import com.mojang.logging.LogUtils;
import net.korostopia.Peripheral.config.PeripheralClientConfig;
import org.slf4j.Logger;

public final class PeripheralDebug {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PeripheralDebug() {
    }

    public static boolean isEnabled(String category) {
        if (!PeripheralClientConfig.SPEC.isLoaded()) {
            return false;
        }

        if (!PeripheralClientConfig.DEBUG_ENABLED.get()) {
            return false;
        }

        return switch (category) {
            case "logScreenClasses" -> PeripheralClientConfig.DEBUG_LOG_SCREEN_CLASSES.get();
            case "logDetectionRange" -> PeripheralClientConfig.DEBUG_LOG_DETECTION_RANGE.get();
            case "logEntityProbes" -> PeripheralClientConfig.DEBUG_LOG_ENTITY_PROBES.get();
            case "logEntityDecisions" -> PeripheralClientConfig.DEBUG_LOG_ENTITY_DECISIONS.get();
            case "logRendererCompatibility" -> PeripheralClientConfig.DEBUG_LOG_RENDERER_COMPATIBILITY.get();
            case "logConfigSummary" -> PeripheralClientConfig.DEBUG_LOG_CONFIG_SUMMARY.get();
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
