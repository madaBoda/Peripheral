package net.korostopia.Peripheral.client.threat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record ThreatTarget(
        LivingEntity entity,
        Vec3 feetPosition,
        Vec3 centerPosition,
        double distanceSquared,
        boolean inFront
) {
}
