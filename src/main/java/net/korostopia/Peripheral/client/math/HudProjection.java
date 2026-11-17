package net.korostopia.Peripheral.client.math;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HudProjection {
    private static final double MAX_PROJECTED_ANGLE_DEGREES = 89.0D;
    private static final double REAR_CENTER_DEADZONE_DEGREES = 0.75D;
    private static final double REAR_CENTER_BLEND_DEGREES = 3.0D;

    private HudProjection() {
    }

    public static Projection project(
            Camera camera,
            Vec3 referencePosition,
            Vec3 feetPosition,
            Vec3 centerPosition,
            int screenWidth,
            int screenHeight,
            double verticalFovDegrees
    ) {
        double dx = centerPosition.x - referencePosition.x;
        double dy = centerPosition.y - referencePosition.y;
        double dz = centerPosition.z - referencePosition.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double bearingDegrees = bearingDegrees(dx, dz, camera.getYRot());
        double rearOffsetDegrees = 180.0D - Math.abs(bearingDegrees);
        double projectedBearingDegrees;
        if (rearOffsetDegrees < REAR_CENTER_DEADZONE_DEGREES) {
            projectedBearingDegrees = 0.0D;
        } else if (rearOffsetDegrees < REAR_CENTER_BLEND_DEGREES) {
            double blend = (rearOffsetDegrees - REAR_CENTER_DEADZONE_DEGREES)
                    / (REAR_CENTER_BLEND_DEGREES - REAR_CENTER_DEADZONE_DEGREES);
            projectedBearingDegrees = Math.copySign(blend * REAR_CENTER_BLEND_DEGREES, bearingDegrees);
        } else if (bearingDegrees > 90.0D) {
            projectedBearingDegrees = 180.0D - bearingDegrees;
        } else if (bearingDegrees < -90.0D) {
            projectedBearingDegrees = -180.0D - bearingDegrees;
        } else {
            projectedBearingDegrees = bearingDegrees;
        }

        double verticalFov = Math.toRadians(clampFov(verticalFovDegrees));
        double focalLength = (screenHeight * 0.5D) / Math.tan(verticalFov * 0.5D);
        double projectedBearing = Math.toRadians(Mth.clamp(
                (float) projectedBearingDegrees,
                (float) -MAX_PROJECTED_ANGLE_DEGREES,
                (float) MAX_PROJECTED_ANGLE_DEGREES
        ));
        double screenCenterX = screenWidth * 0.5D;
        double screenCenterY = screenHeight * 0.5D;
        double screenX = screenCenterX + Math.tan(projectedBearing) * focalLength;

        double feetDx = feetPosition.x - referencePosition.x;
        double feetDy = feetPosition.y - referencePosition.y;
        double feetDz = feetPosition.z - referencePosition.z;
        double feetHorizontalDistance = Math.sqrt(feetDx * feetDx + feetDz * feetDz);
        double targetElevation = Math.atan2(feetDy, Math.max(1.0E-4D, feetHorizontalDistance));
        double worldElevationDegrees = Math.toDegrees(targetElevation);
        double relativeVerticalAngle = targetElevation + Math.toRadians(camera.getXRot());
        double maxAngle = Math.toRadians(MAX_PROJECTED_ANGLE_DEGREES);
        relativeVerticalAngle = Math.max(-maxAngle, Math.min(maxAngle, relativeVerticalAngle));
        double screenY = screenCenterY - Math.tan(relativeVerticalAngle) * focalLength;

        double horizontalHalfFov = Math.toDegrees(Math.atan(
                Math.tan(verticalFov * 0.5D) * ((double) screenWidth / Math.max(1, screenHeight))
        ));

        return new Projection(
                clampFinite(screenX, -screenWidth * 8.0D, screenWidth * 9.0D, screenCenterX),
                clampFinite(screenY, -screenHeight * 8.0D, screenHeight * 9.0D, screenCenterY),
                distance,
                horizontalDistance,
                horizontalHalfFov,
                verticalFov,
                projectedBearingDegrees,
                worldElevationDegrees,
                Math.abs(bearingDegrees) <= 90.0D
        );
    }

    public static double relativeBearingDegrees(
            float facingYawDegrees,
            Vec3 referencePosition,
            Vec3 targetPosition
    ) {
        return bearingDegrees(
                targetPosition.x - referencePosition.x,
                targetPosition.z - referencePosition.z,
                facingYawDegrees
        );
    }

    public static boolean isInFront(
            float facingYawDegrees,
            Vec3 referencePosition,
            Vec3 targetPosition
    ) {
        return Math.abs(relativeBearingDegrees(facingYawDegrees, referencePosition, targetPosition)) <= 90.0D;
    }

    private static double bearingDegrees(double dx, double dz, float facingYawDegrees) {
        double targetYawDegrees = Math.toDegrees(Math.atan2(-dx, dz));
        return Mth.wrapDegrees((float) (targetYawDegrees - facingYawDegrees));
    }

    private static double clampFov(double fovDegrees) {
        return Mth.clamp((float) fovDegrees, 30.0F, 150.0F);
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Mth.clamp(value, min, max) : fallback;
    }

    public record Projection(
            double x,
            double y,
            double distance,
            double horizontalDistance,
            double horizontalHalfFovDegrees,
            double verticalFovRadians,
            double relativeYawDegrees,
            double worldElevationDegrees,
            boolean inFront
    ) {
    }
}
