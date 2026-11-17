package net.korostopia.Peripheral.client.tracking;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HologramTrack {
    LivingEntity entity;
    double worldX;
    double worldY;
    double worldZ;
    double targetWorldX;
    double targetWorldY;
    double targetWorldZ;
    double x;
    double y;
    double scale;
    double opacity;
    double targetOpacity;

    final long createdNanos;
    long lastSeenNanos;
    long fadeOutStartedNanos;
    double fadeOutStartOpacity;
    boolean retiring;
    boolean projectionInitialized;
    boolean inFront;

    HologramTrack(LivingEntity entity, Vec3 feetPosition, long nowNanos) {
        this.entity = entity;
        worldX = targetWorldX = feetPosition.x;
        worldY = targetWorldY = feetPosition.y;
        worldZ = targetWorldZ = feetPosition.z;
        createdNanos = nowNanos;
        lastSeenNanos = nowNanos;
    }

    public LivingEntity entity() {
        return entity;
    }

    public Vec3 feetPosition() {
        return new Vec3(worldX, worldY, worldZ);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double scale() {
        return scale;
    }

    public double opacity() {
        return opacity;
    }

    public boolean isInFront() {
        return inFront;
    }

    public void setHudProjection(double targetX, double targetY, double targetScale, double alpha) {
        if (!projectionInitialized) {
            x = targetX;
            y = targetY;
            scale = targetScale;
            projectionInitialized = true;
            return;
        }

        double interpolation = Math.max(0.0D, Math.min(1.0D, alpha));
        x += (targetX - x) * interpolation;
        y += (targetY - y) * interpolation;
        scale += (targetScale - scale) * interpolation;
    }

    void beginFadeOut(long nowNanos) {
        if (retiring) {
            return;
        }

        retiring = true;
        fadeOutStartedNanos = nowNanos;
        fadeOutStartOpacity = opacity;
        targetOpacity = 0.0D;
    }

    void cancelFadeOut() {
        retiring = false;
        fadeOutStartedNanos = 0L;
    }
}
