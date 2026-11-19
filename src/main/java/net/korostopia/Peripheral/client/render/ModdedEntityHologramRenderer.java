package net.korostopia.Peripheral.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModdedEntityHologramRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Class<?>> GECKOLIB_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> NON_GECKOLIB_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_SUCCESS_TYPES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_FAILURE_TYPES = ConcurrentHashMap.newKeySet();

    private static final float TARGET_MODEL_HEIGHT = 1.70F;
    private static final float TARGET_FEET_Y = -0.85F;
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new BufferBuilder(65_536));

    private ModdedEntityHologramRenderer() {
    }

    public static boolean isGeckoLibRenderer(EntityRenderer<?> renderer) {
        return renderer != null && isGeckoLibClass(renderer.getClass());
    }

    private static boolean isGeckoLibClass(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (GECKOLIB_CLASSES.contains(type)) {
            return true;
        }
        if (NON_GECKOLIB_CLASSES.contains(type)) {
            return false;
        }

        boolean found = type.getName().startsWith("software.bernie.geckolib.");
        if (!found) {
            for (Class<?> iface : type.getInterfaces()) {
                if (isGeckoLibClass(iface)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            found = isGeckoLibClass(type.getSuperclass());
        }
        (found ? GECKOLIB_CLASSES : NON_GECKOLIB_CLASSES).add(type);
        return found;
    }

    public static boolean renderIntoSprite(
            Minecraft minecraft,
            LivingEntity entity,
            EntityRenderer<? super LivingEntity> renderer,
            RenderTarget spriteTarget,
            float partialTick
    ) {
        if (renderer == null) {
            return false;
        }

        boolean geckoLibRenderer = isGeckoLibRenderer(renderer);
        String rendererKind = geckoLibRenderer ? "GeckoLib" : "modded";
        String entityId = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        spriteTarget.bindWrite(true);

        float oldYaw = entity.getYRot();
        float oldYawOld = entity.yRotO;
        float oldBodyYaw = entity.yBodyRot;
        float oldBodyYawOld = entity.yBodyRotO;
        float oldHeadYaw = entity.yHeadRot;
        float oldHeadYawOld = entity.yHeadRotO;
        float oldPitch = entity.getXRot();
        float oldPitchOld = entity.xRotO;

        boolean bufferFlushed = false;
        try {
            // The fixed sprite pose is temporary; all of these rotations are restored below.
            entity.setYRot(0.0F);
            entity.yRotO = 0.0F;
            entity.yBodyRot = 0.0F;
            entity.yBodyRotO = 0.0F;
            entity.yHeadRot = 0.0F;
            entity.yHeadRotO = 0.0F;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;

            float entityHeight = Math.max(0.25F, entity.getBbHeight());
            float modelScale = Mth.clamp(TARGET_MODEL_HEIGHT / entityHeight, 0.08F, 6.0F);
            PoseStack modelPose = new PoseStack();
            modelPose.translate(0.0F, TARGET_FEET_Y, 0.0F);
            modelPose.scale(modelScale, modelScale, modelScale);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            renderer.render(entity, 0.0F, partialTick, modelPose, BUFFERS, LightTexture.FULL_BRIGHT);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            BUFFERS.endBatch();
            bufferFlushed = true;

            if (PeripheralDebug.isEnabled("logRendererCompatibility")
                    && LOGGED_SUCCESS_TYPES.add(entityId)) {
                PeripheralDebug.log("logRendererCompatibility",
                        "Rendered modded entity {} using its registered {} renderer {} with silhouette backing.",
                        entityId, rendererKind, renderer.getClass().getName());
            }
            return true;
        } catch (RuntimeException | LinkageError exception) {
            if (!bufferFlushed) {
                try {
                    BUFFERS.endBatch();
                } catch (RuntimeException ignored) {
                }
            }
            if (LOGGED_FAILURE_TYPES.add(entityId)) {
                LOGGER.warn(
                        "Peripheral could not invoke registered {} renderer {} for entity {}; "
                                + "the generic hologram fallback will be used.",
                        rendererKind,
                        renderer.getClass().getName(),
                        entityId,
                        exception
                );
            }
            return false;
        } finally {
            entity.setYRot(oldYaw);
            entity.yRotO = oldYawOld;
            entity.yBodyRot = oldBodyYaw;
            entity.yBodyRotO = oldBodyYawOld;
            entity.yHeadRot = oldHeadYaw;
            entity.yHeadRotO = oldHeadYawOld;
            entity.setXRot(oldPitch);
            entity.xRotO = oldPitchOld;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }
    }
}
