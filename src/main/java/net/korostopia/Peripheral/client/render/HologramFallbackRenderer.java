package net.korostopia.Peripheral.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public final class HologramFallbackRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<EntityType<?>> LOGGED_TYPES = ConcurrentHashMap.newKeySet();

    private static final float OPAQUE_WHITE = 1.0F;

    private HologramFallbackRenderer() {
    }

    public static boolean renderIntoSprite(LivingEntity entity) {
        return renderIntoSprite(entity, false);
    }

    public static boolean renderBackgroundIntoSprite(LivingEntity entity) {
        return renderIntoSprite(entity, true);
    }

    private static boolean renderIntoSprite(LivingEntity entity, boolean background) {
        if (PeripheralDebug.isEnabled("logRendererCompatibility")
                && LOGGED_TYPES.add(entity.getType())) {
            PeripheralDebug.log("logRendererCompatibility",
                    "Drew a generic hologram silhouette for entity type {}; "
                            + "it may be a standalone fallback or a backing layer for a custom renderer.",
                    ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
        }

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (background) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            }
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            drawHead(buffer);
            quad(buffer, -0.20F, 0.40F, 0.20F, 0.40F, 0.17F, -0.14F, -0.17F, -0.14F);
            quad(buffer, -0.19F, 0.32F, -0.08F, 0.27F, -0.30F, -0.13F, -0.39F, -0.08F);
            quad(buffer, 0.08F, 0.27F, 0.19F, 0.32F, 0.39F, -0.08F, 0.30F, -0.13F);
            quad(buffer, -0.15F, -0.12F, -0.02F, -0.12F, -0.06F, -0.85F, -0.20F, -0.85F);
            quad(buffer, 0.02F, -0.12F, 0.15F, -0.12F, 0.20F, -0.85F, 0.06F, -0.85F);
            triangle(buffer, -0.20F, 0.34F, -0.36F, 0.48F, -0.15F, 0.25F);
            triangle(buffer, 0.20F, 0.34F, 0.36F, 0.48F, 0.15F, 0.25F);

            Tesselator.getInstance().end();
            return true;
        } catch (RuntimeException exception) {
            if (buffer.building()) {
                buffer.discard();
            }
            LOGGER.error("Peripheral could not draw a fallback silhouette for {}.",
                    ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()), exception);
            return false;
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }
    }

    private static void drawHead(BufferBuilder buffer) {
        final int segments = 10;
        final float centerX = 0.0F;
        final float centerY = 0.615F;
        final float radiusX = 0.17F;
        final float radiusY = 0.235F;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            triangle(buffer,
                    centerX, centerY,
                    centerX + (float) Math.cos(a0) * radiusX,
                    centerY + (float) Math.sin(a0) * radiusY,
                    centerX + (float) Math.cos(a1) * radiusX,
                    centerY + (float) Math.sin(a1) * radiusY);
        }
    }

    private static void quad(
            BufferBuilder buffer,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4
    ) {
        vertex(buffer, x1, y1);
        vertex(buffer, x2, y2);
        vertex(buffer, x3, y3);
        vertex(buffer, x4, y4);
    }

    // The shared buffer uses QUADS, so a triangle repeats its last vertex.
    private static void triangle(
            BufferBuilder buffer,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3
    ) {
        vertex(buffer, x1, y1);
        vertex(buffer, x2, y2);
        vertex(buffer, x3, y3);
        vertex(buffer, x3, y3);
    }

    private static void vertex(BufferBuilder buffer, float x, float y) {
        buffer.vertex(x, y, 0.0F)
                .color(OPAQUE_WHITE, OPAQUE_WHITE, OPAQUE_WHITE, OPAQUE_WHITE)
                .endVertex();
    }
}
