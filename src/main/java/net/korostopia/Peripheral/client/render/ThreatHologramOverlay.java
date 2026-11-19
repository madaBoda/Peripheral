package net.korostopia.Peripheral.client.render;

import net.korostopia.Peripheral.client.input.PeripheralKeyMappings;
import net.korostopia.Peripheral.client.math.CameraFovCache;
import net.korostopia.Peripheral.client.math.HudProjection;
import net.korostopia.Peripheral.client.threat.ThreatDetector;
import net.korostopia.Peripheral.client.tracking.HologramTrack;
import net.korostopia.Peripheral.client.tracking.HologramTracker;
import net.korostopia.Peripheral.config.PeripheralConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;

public final class ThreatHologramOverlay {
    private static final HologramTracker TRACKER = new HologramTracker();

    private static ClientLevel lastLevel;
    private static long previousFrameNanos;

    private ThreatHologramOverlay() {
    }

    public static void render(
            ForgeGui forgeGui,
            GuiGraphics guiGraphics,
            float partialTick,
            int screenWidth,
            int screenHeight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PeripheralKeyMappings.areHologramsEnabled()) {
            clearAll(minecraft.level);
            return;
        }
        if (minecraft.screen != null) {
            if (!PeripheralConfig.SHOW_IN_MENUS.get()) {
                clearIfNeeded(minecraft.level);
            }
            return;
        }
        renderContent(minecraft, guiGraphics, partialTick, screenWidth, screenHeight, false);
    }

    public static void renderOnScreen(
            Screen screen,
            GuiGraphics guiGraphics,
            float partialTick,
            int screenWidth,
            int screenHeight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PeripheralKeyMappings.areHologramsEnabled()) {
            clearAll(minecraft.level);
            return;
        }
        if (!PeripheralConfig.SHOW_IN_MENUS.get() || screen == null || minecraft.screen == null
                || isScreenExcluded(screen)) {
            return;
        }
        renderContent(minecraft, guiGraphics, partialTick, screenWidth, screenHeight, true);
    }

    private static boolean isScreenExcluded(Screen screen) {
        List<? extends String> exclusions = PeripheralConfig.SCREEN_EXCLUSIONS.get();
        if (exclusions.isEmpty()) {
            return false;
        }
        for (Class<?> type = screen.getClass(); type != null && Screen.class.isAssignableFrom(type);
             type = type.getSuperclass()) {
            String className = type.getName();
            for (String configured : exclusions) {
                if (configured == null || configured.isBlank()) {
                    continue;
                }
                String pattern = configured.trim();
                if (pattern.endsWith(".*")) {
                    String packagePrefix = pattern.substring(0, pattern.length() - 1);
                    if (className.startsWith(packagePrefix)) {
                        return true;
                    }
                } else if (className.equals(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void renderContent(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            float partialTick,
            int screenWidth,
            int screenHeight,
            boolean screenPass
    ) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || (!screenPass && minecraft.options.hideGui)) {
            clearIfNeeded(level);
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        if (lastLevel != level) {
            TRACKER.clear();
            lastLevel = level;
            previousFrameNanos = 0L;
        }

        long nowNanos = System.nanoTime();
        double deltaSeconds = previousFrameNanos == 0L
                ? 1.0D / 60.0D
                : Mth.clamp((nowNanos - previousFrameNanos) / 1_000_000_000.0D, 0.0D, 0.05D);
        previousFrameNanos = nowNanos;

        double verticalFovDegrees = CameraFovCache.get(camera, minecraft.options.fov().get());
        int detectionRange = PeripheralConfig.DETECTION_RANGE.get();
        ThreatDetector.DetectionResult detection = ThreatDetector.detect(
                minecraft,
                level,
                camera,
                partialTick,
                detectionRange,
                PeripheralConfig.RADAR_RANGE.get(),
                PeripheralConfig.MAX_TARGETS.get(),
                PeripheralConfig.ENABLE_FRONT_HEMISPHERE.get(),
                PeripheralConfig.SHOW_THROUGH_WALLS.get(),
                screenPass
        );
        TRACKER.update(
                detection.threats(),
                detection.suppressedEntities(),
                deltaSeconds,
                PeripheralConfig.SMOOTHING_SPEED.get(),
                PeripheralConfig.OPACITY_INTERPOLATION_SPEED.get(),
                PeripheralConfig.OPACITY.get(),
                detectionRange,
                PeripheralConfig.FADE_IN_DURATION_MS.get(),
                PeripheralConfig.FADE_OUT_DURATION_MS.get(),
                nowNanos
        );
        TRACKER.removeOutsideRange(camera.getPosition(), detectionRange, nowNanos);

        if (TRACKER.isEmpty()) {
            return;
        }

        Vec3 playerEyeReference = interpolatedPlayerEyePosition(minecraft.player, partialTick);
        double hudMovementSpeed = Math.max(
                0.0D,
                PeripheralConfig.HUD_MOVEMENT_SMOOTHING_SPEED.get()
        );
        double hudMovementAlpha = 1.0D - Math.exp(-hudMovementSpeed * deltaSeconds);
        double minSizeDistance = PeripheralConfig.MIN_SIZE_DISTANCE.get();
        Vec3 cameraPosition = camera.getPosition();
        double verticalFovRadians = Math.toRadians(Mth.clamp(
                (float) verticalFovDegrees, 30.0F, 150.0F
        ));
        double focalLengthY = (screenHeight * 0.5D) / Math.tan(verticalFovRadians * 0.5D);

        for (HologramTrack track : TRACKER.tracks()) {
            Vec3 feet = track.feetPosition();
            Vec3 center = feet.add(0.0D, track.entity().getBbHeight() * 0.5D, 0.0D);
            HudProjection.Projection projection = HudProjection.project(
                    camera, playerEyeReference, feet, center, screenWidth, screenHeight,
                    verticalFovDegrees, minSizeDistance
            );

            double cameraDistanceToCenter = cameraPosition.distanceTo(center);
            double sizingDistance = minSizeDistance <= 0.0D
                    ? Math.max(0.1D, cameraDistanceToCenter)
                    : Math.max(minSizeDistance, cameraDistanceToCenter);
            double pixelsPerBlock = focalLengthY / sizingDistance;
            track.setHudProjection(projection.x(), projection.y(), pixelsPerBlock, hudMovementAlpha);
        }

        for (HologramTrack track : TRACKER.tracks()) {
            if (track.opacity() > 0.002D) {
                HologramSpriteRenderer.draw(
                        minecraft, guiGraphics, track, partialTick, screenWidth, screenHeight
                );
            }
        }
    }

    private static Vec3 interpolatedPlayerEyePosition(LocalPlayer player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY()) + player.getEyeHeight();
        double z = Mth.lerp(partialTick, player.zo, player.getZ());
        return new Vec3(x, y, z);
    }

    private static void clearAll(ClientLevel currentLevel) {
        TRACKER.clear();
        lastLevel = currentLevel;
        previousFrameNanos = 0L;
    }

    private static void clearIfNeeded(ClientLevel currentLevel) {
        if (currentLevel == null || currentLevel != lastLevel) {
            TRACKER.clear();
            lastLevel = currentLevel;
        }
        previousFrameNanos = 0L;
    }
}
