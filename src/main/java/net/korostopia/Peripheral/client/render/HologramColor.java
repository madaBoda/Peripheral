package net.korostopia.Peripheral.client.render;

import net.korostopia.Peripheral.client.tracking.HologramTrack;
import net.korostopia.Peripheral.config.PeripheralConfig;

public record HologramColor(float red, float green, float blue) {
    public static HologramColor forTrack(HologramTrack track) {
        String configured = track != null && track.isInFront()
                ? PeripheralConfig.FRONT_HOLOGRAM_COLOR.get()
                : PeripheralConfig.HOLOGRAM_COLOR.get();
        return parse(configured);
    }

    private static HologramColor parse(String value) {
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            int rgb = Integer.parseInt(hex, 16);
            return new HologramColor(((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        } catch (RuntimeException ignored) {
            return new HologramColor(1.0F, 0.20F, 0.18F);
        }
    }
}
