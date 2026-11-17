package net.korostopia.Peripheral.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramModelRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<EntityType<?>> TYPES_WITH_RENDER_ERRORS = ConcurrentHashMap.newKeySet();
    private static final Set<EntityType<?>> TYPES_USING_FALLBACK = ConcurrentHashMap.newKeySet();
    private static final float MODEL_NDC_HEIGHT = 1.70F;
    private static final float MODEL_FEET_Y = -0.85F;
    private static final float MODEL_FEET_UNIT_Y = 1.501F;
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new BufferBuilder(8192));

    private HologramModelRenderer() {
    }

    public static boolean renderIntoSprite(
            Minecraft minecraft,
            LivingEntity entity,
            RenderTarget spriteTarget,
            float partialTick
    ) {
        try {
            EntityRenderer<? super LivingEntity> baseRenderer =
                    minecraft.getEntityRenderDispatcher().getRenderer(entity);
            ResourceLocation entityKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            boolean moddedEntityType = entityKey != null && !"minecraft".equals(entityKey.getNamespace());
            if (moddedEntityType || ModdedEntityHologramRenderer.isGeckoLibRenderer(baseRenderer)) {
                return ModdedEntityHologramRenderer.renderIntoSprite(
                        minecraft, entity, baseRenderer, spriteTarget, partialTick
                );
            }

            if (!(baseRenderer instanceof LivingEntityRenderer<?, ?>)) {
                logFallbackOnce(entity, baseRenderer, "renderer is not a LivingEntityRenderer or GeckoLib renderer");
                return false;
            }
            renderLivingBaseModel(baseRenderer, entity, spriteTarget, partialTick);
            return true;
        } catch (RuntimeException exception) {
            try {
                BUFFERS.endBatch();
            } catch (RuntimeException ignored) {
                // Keep the rendering error; this is just cleanup.
            }
            if (TYPES_WITH_RENDER_ERRORS.add(entity.getType())) {
                LOGGER.warn(
                        "Peripheral could not render the base model for entity type {}; "
                                + "the generic silhouette fallback will be used.",
                        entity.getType(), exception
                );
            }
            return false;
        }
    }

    private static void logFallbackOnce(
            LivingEntity entity,
            EntityRenderer<?> renderer,
            String reason
    ) {
        if (PeripheralDebug.isEnabled("logRendererCompatibility")
                && TYPES_USING_FALLBACK.add(entity.getType())) {
            PeripheralDebug.log("logRendererCompatibility",
                    "Will use the generic hologram fallback for entity type {} (renderer={}, reason={}).",
                    ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()),
                    renderer == null ? "<none>" : renderer.getClass().getName(), reason);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderLivingBaseModel(
            EntityRenderer<? super LivingEntity> baseRenderer,
            LivingEntity entity,
            RenderTarget spriteTarget,
            float partialTick
    ) {
        LivingEntityRenderer renderer = (LivingEntityRenderer) baseRenderer;
        EntityModel model = renderer.getModel();
        ResourceLocation texture = renderer.getTextureLocation(entity);
        // The world renderer shares this model instance, so save its pose before resetting it.
        List<ModelPart> parts = collectModelParts(model);
        Map<ModelPart, PartPose> originalPartPoses = new IdentityHashMap<>();
        for (ModelPart part : parts) {
            originalPartPoses.put(part, part.storePose());
            part.resetPose();
        }

        boolean oldYoung = model.young;
        boolean oldRiding = model.riding;
        float oldAttackTime = model.attackTime;
        try {
            float limbSwing = entity.walkAnimation.position(partialTick);
            float limbSwingAmount = entity.walkAnimation.speed(partialTick);
            model.young = entity.isBaby();
            model.riding = entity.isPassenger();
            model.attackTime = entity.getAttackAnim(partialTick);
            model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            // Keep the sprite facing forward; the camera only moves the HUD marker.
            model.setupAnim(entity, limbSwing, limbSwingAmount, partialTick, 0.0F, 0.0F);

            float height = Math.max(0.25F, entity.getBbHeight());
            float modelScale = Mth.clamp(MODEL_NDC_HEIGHT / height, 0.08F, 6.0F);
            float feetOffset = MODEL_FEET_Y + MODEL_FEET_UNIT_Y * modelScale;

            PoseStack modelPose = new PoseStack();
            modelPose.translate(0.0F, feetOffset, 0.0F);
            modelPose.mulPose(Axis.YP.rotationDegrees(180.0F));
            modelPose.scale(-modelScale, -modelScale, modelScale);

            model.renderToBuffer(
                    modelPose,
                    BUFFERS.getBuffer(HologramRenderType.forTexture(texture, spriteTarget)),
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
            BUFFERS.endBatch();
        } finally {
            // Don't let a HUD render change the model used by the normal world render.
            for (Map.Entry<ModelPart, PartPose> entry : originalPartPoses.entrySet()) {
                entry.getKey().loadPose(entry.getValue());
            }
            model.young = oldYoung;
            model.riding = oldRiding;
            model.attackTime = oldAttackTime;
        }
    }

    private static List<ModelPart> collectModelParts(EntityModel<?> model) {
        List<ModelPart> parts = new ArrayList<>();
        if (model instanceof HierarchicalModel<?> hierarchical) {
            hierarchical.root().getAllParts().forEach(parts::add);
        } else if (model instanceof ListModel<?> listModel) {
            listModel.parts().forEach(parts::add);
        }
        return parts;
    }
}
