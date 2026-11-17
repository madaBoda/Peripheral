package net.korostopia.Peripheral.client.tracking;

import net.korostopia.Peripheral.client.threat.ThreatTarget;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HologramTracker {
    private static final long VISIBILITY_GRACE_NANOS = 90_000_000L;
    private static final double REMOVE_OPACITY = 0.004D;
    private static final double FAR_OPACITY_FRACTION = 0.25D;

    private final Map<UUID, HologramTrack> tracks = new HashMap<>();

    public void update(
            List<ThreatTarget> targets,
            Set<UUID> suppressedEntities,
            double deltaSeconds,
            double worldSmoothingSpeed,
            double opacityInterpolationSpeed,
            double baseOpacity,
            double maxRange,
            int fadeInDurationMs,
            int fadeOutDurationMs,
            long nowNanos
    ) {
        for (UUID suppressedId : suppressedEntities) {
            HologramTrack track = tracks.get(suppressedId);
            if (track != null) {
                track.beginFadeOut(nowNanos);
            }
        }

        double safeRange = Math.max(1.0D, maxRange);
        for (ThreatTarget target : targets) {
            LivingEntity entity = target.entity();
            Vec3 feetPosition = target.feetPosition();
            HologramTrack track = tracks.get(entity.getUUID());
            if (track == null) {
                track = new HologramTrack(entity, feetPosition, nowNanos);
                tracks.put(entity.getUUID(), track);
            }

            track.cancelFadeOut();
            track.entity = entity;
            track.inFront = target.inFront();
            track.lastSeenNanos = nowNanos;
            track.targetWorldX = feetPosition.x;
            track.targetWorldY = feetPosition.y;
            track.targetWorldZ = feetPosition.z;

            double normalizedDistance = Math.min(
                    1.0D,
                    Math.sqrt(Math.max(0.0D, target.distanceSquared())) / safeRange
            );
            double distanceOpacity = 1.0D - (1.0D - FAR_OPACITY_FRACTION) * normalizedDistance;
            track.targetOpacity = baseOpacity * distanceOpacity;
        }

        double positionAlpha = exponentialAlpha(worldSmoothingSpeed, deltaSeconds);
        double opacityAlpha = exponentialAlpha(opacityInterpolationSpeed, deltaSeconds);
        long fadeInDurationNanos = Math.max(0, fadeInDurationMs) * 1_000_000L;
        long fadeOutDurationNanos = Math.max(0, fadeOutDurationMs) * 1_000_000L;

        Iterator<Map.Entry<UUID, HologramTrack>> iterator = tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            HologramTrack track = iterator.next().getValue();

            if (track.entity.isRemoved() || !track.entity.isAlive()) {
                track.beginFadeOut(nowNanos);
            }
            if (!track.retiring && nowNanos - track.lastSeenNanos > VISIBILITY_GRACE_NANOS) {
                track.beginFadeOut(nowNanos);
            }

            track.worldX = lerp(track.worldX, track.targetWorldX, positionAlpha);
            track.worldY = lerp(track.worldY, track.targetWorldY, positionAlpha);
            track.worldZ = lerp(track.worldZ, track.targetWorldZ, positionAlpha);

            if (track.retiring) {
                double progress = fadeOutDurationNanos == 0L
                        ? 1.0D
                        : clamp01((nowNanos - track.fadeOutStartedNanos) / (double) fadeOutDurationNanos);

                track.opacity = track.fadeOutStartOpacity * (1.0D - smoothstep(progress));
                if (progress >= 1.0D || track.opacity < REMOVE_OPACITY) {
                    iterator.remove();
                }
                continue;
            }

            track.opacity = lerp(track.opacity, track.targetOpacity, opacityAlpha);

            double fadeInProgress = fadeInDurationNanos == 0L
                    ? 1.0D
                    : clamp01((nowNanos - track.createdNanos) / (double) fadeInDurationNanos);
            track.opacity = Math.min(
                    track.opacity,
                    track.targetOpacity * smoothstep(fadeInProgress)
            );
        }
    }

    public void removeOutsideRange(Vec3 cameraPosition, double maxRange, long nowNanos) {
        double rangeSquared = maxRange * maxRange;

        for (HologramTrack track : tracks.values()) {
            LivingEntity entity = track.entity;
            Vec3 liveCenter = new Vec3(
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.5D,
                    entity.getZ()
            );

            if (liveCenter.distanceToSqr(cameraPosition) > rangeSquared) {
                track.beginFadeOut(nowNanos);
            }
        }
    }

    public Collection<HologramTrack> tracks() {
        return tracks.values();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public void clear() {
        tracks.clear();
    }

    private static double exponentialAlpha(double speed, double deltaSeconds) {
        double safeSpeed = Math.max(0.0D, speed);
        double safeDelta = Math.max(0.0D, Math.min(0.05D, deltaSeconds));
        return 1.0D - Math.exp(-safeSpeed * safeDelta);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static double smoothstep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }
}
