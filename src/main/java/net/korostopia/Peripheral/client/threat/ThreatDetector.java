package net.korostopia.Peripheral.client.threat;

import net.korostopia.Peripheral.client.math.HudProjection;
import net.korostopia.Peripheral.config.PeripheralServerConfig;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ThreatDetector {
    private static final double RANGE_EPSILON_SQUARED = 0.01D;
    private static final double[] BODY_SAMPLE_HEIGHTS = {
            0.92D, 0.72D, 0.50D, 0.28D, 0.08D
    };
    private static final double[][] SIDE_SAMPLES = {
            {1.0D, 1.0D}, {1.0D, -1.0D}, {-1.0D, 1.0D}, {-1.0D, -1.0D}
    };
    private static final Logger LOGGER = LoggerFactory.getLogger("Peripheral/ThreatDetector");
    private static int lastLoggedRange = Integer.MIN_VALUE;
    private static final Set<String> ENTITY_PROBES = ConcurrentHashMap.newKeySet();
    private static final Set<String> ENTITY_DECISIONS = ConcurrentHashMap.newKeySet();

    private ThreatDetector() {
    }

    public static DetectionResult detect(
            Minecraft minecraft,
            ClientLevel level,
            Camera camera,
            float partialTick,
            double range,
            double radarRange,
            int maxTargets,
            boolean includeFrontHemisphere,
            boolean showThroughWalls,
            boolean listenModeActive,
            boolean guiScreenPass
    ) {
        if (level == null || minecraft.player == null || camera == null) {
            return new DetectionResult(List.of(), Set.of());
        }

        int effectiveRange = Math.max(1, (int) Math.floor(range));
        if (effectiveRange != lastLoggedRange) {
            lastLoggedRange = effectiveRange;
            PeripheralDebug.log(
                    "logDetectionRange",
                    "Threat detector active range: {} blocks (3D distance from camera to mob center)",
                    effectiveRange
            );
        }
        double detectionRange = effectiveRange;

        Vec3 cameraPosition = camera.getPosition();
        double rangeSquared = detectionRange * detectionRange;
        double effectiveRadarRange = Math.min(detectionRange, Math.max(0.0D, radarRange));
        double radarRangeSquared = effectiveRadarRange * effectiveRadarRange;
        AABB searchBounds = new AABB(
                cameraPosition.x - detectionRange,
                cameraPosition.y - detectionRange,
                cameraPosition.z - detectionRange,
                cameraPosition.x + detectionRange,
                cameraPosition.y + detectionRange,
                cameraPosition.z + detectionRange
        );

        Set<String> includedMobIds = normalizeEntityIds(PeripheralServerConfig.includedMobs());
        Set<String> excludedMobIds = normalizeEntityIds(PeripheralServerConfig.excludedMobs());

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBounds,
                entity -> entity.isAlive() && !entity.isRemoved()
        );

        List<ThreatTarget> selected = new ArrayList<>();
        Set<UUID> suppressedEntities = new HashSet<>();
        for (LivingEntity entity : entities) {
            Vec3 feet = interpolatedFeet(entity, partialTick);
            Vec3 center = feet.add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            double distanceSquared = center.distanceToSqr(cameraPosition);
            if (distanceSquared < RANGE_EPSILON_SQUARED || distanceSquared > rangeSquared) {
                continue;
            }

            ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (typeKey == null) {
                continue;
            }
            String typeId = typeKey.toString().toLowerCase(Locale.ROOT);
            boolean matchesInclude = matchesConfiguredEntityId(includedMobIds, typeId);
            boolean matchesExclude = matchesConfiguredEntityId(excludedMobIds, typeId);
            logModdedEntityProbe(minecraft, entity, typeId, matchesInclude, matchesExclude);

            if (!isPotentialThreat(entity, minecraft.player, matchesInclude, matchesExclude)) {
                if (matchesExclude) {
                    logEntityDecision(typeId, "excluded", "matches excludeMobs; exclusion takes priority");
                } else {
                    logEntityDecision(typeId, "not-hostile", automaticThreatReason(entity, minecraft.player));
                }
                continue;
            }

            boolean inFront = HudProjection.isInFront(
                    minecraft.player.getYRot(), cameraPosition, center
            );

            // Listen Mode deliberately ignores the normal radar rules: eligible threats are
            // shown in every direction, whether visible or occluded by blocks, up to detectionRange.
            if (!listenModeActive) {
                if (inFront && !includeFrontHemisphere) {
                    logEntityDecision(
                            typeId,
                            "direction-filtered",
                            "front-side detection is disabled by the server config"
                    );
                    suppressedEntities.add(entity.getUUID());
                    continue;
                }

                boolean insideRadarRange = distanceSquared <= radarRangeSquared;
                boolean clearLineOfSight = hasLineOfSight(level, minecraft, cameraPosition, entity, feet);

                if (!clearLineOfSight) {
                    if (!showThroughWalls || !insideRadarRange) {
                        logEntityDecision(typeId, "wall-occlusion-suppressed",
                                !showThroughWalls
                                        ? "mob is blocked by solid blocks and showThroughWalls is disabled"
                                        : "mob is blocked by solid blocks and is outside radarRange");
                        suppressedEntities.add(entity.getUUID());
                        continue;
                    }
                } else if (!guiScreenPass && inFront) {
                    logEntityDecision(typeId, "visible-front-suppressed",
                            "front-facing mob is clearly visible during normal gameplay");
                    suppressedEntities.add(entity.getUUID());
                    continue;
                }
            }

            logEntityDecision(
                    typeId,
                    "candidate",
                    (inFront ? "front" : "rear") + " threat eligible; listenMode=" + listenModeActive
                            + ", radarRange=" + effectiveRadarRange + ", detectionRange=" + detectionRange
                            + ", guiPass=" + guiScreenPass
            );
            selected.add(new ThreatTarget(entity, feet, center, distanceSquared, inFront));
        }

        selected.sort(Comparator.comparingDouble(ThreatTarget::distanceSquared));
        if (!listenModeActive && selected.size() > maxTargets) {
            selected.subList(maxTargets, selected.size()).clear();
        }
        return new DetectionResult(List.copyOf(selected), Set.copyOf(suppressedEntities));
    }

    private static boolean isPotentialThreat(
            LivingEntity entity,
            LivingEntity localPlayer,
            boolean matchesInclude,
            boolean matchesExclude
    ) {
        if (matchesExclude) {
            return false;
        }
        if (matchesInclude) {
            return true;
        }
        return isAutomaticallyThreatening(entity, localPlayer);
    }

    private static boolean isAutomaticallyThreatening(LivingEntity entity, LivingEntity localPlayer) {
        if (entity instanceof Enemy) {
            return true;
        }
        if (entity.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }
        if (entity instanceof Mob mob) {
            return mob.getTarget() == localPlayer || mob.isAggressive();
        }
        return false;
    }

    private static String automaticThreatReason(LivingEntity entity, LivingEntity localPlayer) {
        if (entity instanceof Enemy) {
            return "not expected: entity implements Enemy; check include/exclude config and registry diagnostics";
        }
        if (entity.getType().getCategory() == MobCategory.MONSTER) {
            return "not expected: registry category is MONSTER; check include/exclude config and registry diagnostics";
        }
        if (entity instanceof Mob mob) {
            return "not automatically classified: category=" + entity.getType().getCategory()
                    + ", aggressive=" + mob.isAggressive()
                    + ", targetsPlayer=" + (mob.getTarget() == localPlayer)
                    + "; add the exact registry ID to includeMobs if this is a threat";
        }
        return "not automatically classified: entity has no standard hostility signal; "
                + "add its registry ID to includeMobs if needed";
    }

    private static void logModdedEntityProbe(
            Minecraft minecraft,
            LivingEntity entity,
            String entityId,
            boolean matchesInclude,
            boolean matchesExclude
    ) {
        if (!PeripheralDebug.isEnabled("logEntityProbes")
                || entityId.startsWith("minecraft:") || !ENTITY_PROBES.add(entityId)) {
            return;
        }
        String rendererClass;
        try {
            rendererClass = minecraft.getEntityRenderDispatcher().getRenderer(entity).getClass().getName();
        } catch (RuntimeException exception) {
            rendererClass = "<renderer lookup failed: " + exception.getClass().getSimpleName() + ">";
        }
        boolean enemyMarker = entity instanceof Enemy;
        boolean aggressive = entity instanceof Mob mob && mob.isAggressive();
        boolean targetsPlayer = entity instanceof Mob mob && mob.getTarget() == minecraft.player;
        PeripheralDebug.log("logEntityProbes",
                "Modded-entity probe: registryId={}, entityClass={}, rendererClass={}, "
                        + "includeMatch={}, excludeMatch={}, enemyInterface={}, category={}, "
                        + "aggressive={}, targetsPlayer={}",
                entityId, entity.getClass().getName(), rendererClass, matchesInclude, matchesExclude,
                enemyMarker, entity.getType().getCategory(), aggressive, targetsPlayer
        );
    }

    private static void logEntityDecision(String entityId, String decision, String explanation) {
        if (!PeripheralDebug.isEnabled("logEntityDecisions")
                || entityId == null || entityId.startsWith("minecraft:")) {
            return;
        }
        String key = entityId + ":" + decision;
        if (ENTITY_DECISIONS.add(key)) {
            PeripheralDebug.log("logEntityDecisions",
                    "Entity decision: {} -> {} ({}).", entityId, decision, explanation);
        }
    }

    private static Set<String> normalizeEntityIds(List<? extends String> ids) {
        Set<String> normalized = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String candidate = id.trim().toLowerCase(Locale.ROOT);
            String valueToValidate = candidate.endsWith(":*")
                    ? candidate.substring(0, candidate.length() - 1) + "peripheral_wildcard"
                    : candidate;
            if (ResourceLocation.isValidResourceLocation(valueToValidate) && candidate.indexOf(':') > 0) {
                normalized.add(candidate);
            } else {
                LOGGER.warn("Ignoring invalid entity ID or namespace wildcard in Peripheral config: '{}'", id);
            }
        }
        return normalized;
    }

    private static boolean matchesConfiguredEntityId(Set<String> configuredIds, String entityId) {
        if (configuredIds.contains(entityId)) {
            return true;
        }
        int separator = entityId.indexOf(':');
        return separator > 0 && configuredIds.contains(entityId.substring(0, separator + 1) + "*");
    }

    private static boolean hasLineOfSight(
            ClientLevel level,
            Minecraft minecraft,
            Vec3 cameraPosition,
            LivingEntity entity,
            Vec3 feet
    ) {
        double width = entity.getBbWidth();
        double height = entity.getBbHeight();
        double side = width * 0.32D;

        // One clear sample is enough, so don't build rays we won't need.
        for (double heightFactor : BODY_SAMPLE_HEIGHTS) {
            if (hasClearLine(level, minecraft, cameraPosition, feet.add(0.0D, height * heightFactor, 0.0D))) {
                return true;
            }
        }

        for (double[] sideSample : SIDE_SAMPLES) {
            Vec3 sample = feet.add(side * sideSample[0], height * 0.60D, side * sideSample[1]);
            if (hasClearLine(level, minecraft, cameraPosition, sample)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClearLine(
            ClientLevel level,
            Minecraft minecraft,
            Vec3 cameraPosition,
            Vec3 targetPosition
    ) {
        BlockHitResult hit = level.clip(new ClipContext(
                cameraPosition,
                targetPosition,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static Vec3 interpolatedFeet(LivingEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ())
        );
    }

    public record DetectionResult(List<ThreatTarget> threats, Set<UUID> suppressedEntities) {
    }
}
