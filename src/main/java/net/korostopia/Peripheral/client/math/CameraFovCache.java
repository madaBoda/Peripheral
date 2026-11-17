package net.korostopia.Peripheral.client.math;

import net.minecraft.client.Camera;

public final class CameraFovCache {
    private static Camera camera;
    private static double fovDegrees;
    private static long capturedAtNanos;

    private CameraFovCache() {
    }

    public static void capture(Camera sourceCamera, double fov) {
        if (sourceCamera == null || !Double.isFinite(fov) || fov < 1.0D || fov > 179.0D) {
            return;
        }
        camera = sourceCamera;
        fovDegrees = fov;
        capturedAtNanos = System.nanoTime();
    }

    public static double get(Camera requestedCamera, double fallbackFov) {
        long age = System.nanoTime() - capturedAtNanos;
        if (requestedCamera != null && requestedCamera == camera && age >= 0L && age < 2_000_000_000L) {
            return fovDegrees;
        }
        return fallbackFov;
    }
}
