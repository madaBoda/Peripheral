package net.korostopia.Peripheral.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.korostopia.Peripheral.client.tracking.HologramTrack;
import net.korostopia.Peripheral.config.PeripheralClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HologramSpriteRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Peripheral/HologramSpriteRenderer");

    private static final int SPRITE_SIZE = 128;
    private static final double MODEL_HEIGHT_FRACTION = 0.85D;
    private static final double FEET_ANCHOR_FROM_TOP = 0.925D;

    private static TextureTarget spriteTarget;
    private static boolean spriteRenderFailureLogged;

    private HologramSpriteRenderer() {
    }

    public static void draw(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            HologramTrack track,
            float partialTick,
            int screenWidth,
            int screenHeight
    ) {
        if (track.opacity() <= 0.002D || track.scale() <= 0.0D) {
            return;
        }

        ensureSpriteTarget();

        double spriteSide = Math.max(
                2.0D,
                track.entity().getBbHeight() * track.scale() / MODEL_HEIGHT_FRACTION
        );
        double left = track.x() - spriteSide * 0.5D;
        double top = track.y() - spriteSide * FEET_ANCHOR_FROM_TOP;
        if (left + spriteSide <= 0.0D || left >= screenWidth
                || top + spriteSide <= 0.0D || top >= screenHeight) {
            return;
        }
        // Flush before switching targets; queued GUI vertices belong on the main framebuffer.
        guiGraphics.flush();
        try {
            if (!renderSprite(minecraft, track, partialTick)) {
                return;
            }

            float quadSize = (float) Math.max(1.0D, spriteSide);
            float x = (float) left;
            float y = (float) top;
            HologramColor tint = HologramColor.forTrack(track);
            double opacity = Math.max(0.0D, Math.min(1.0D, track.opacity()));
            double glow = Math.max(0.0D, Math.min(1.0D, PeripheralClientConfig.GLOW_AMOUNT.get()));
            if (glow > 0.0D) {
                float outerSize = quadSize * 1.30F;
                float middleSize = quadSize * 1.17F;
                float innerSize = quadSize * 1.08F;
                drawTintedQuad(guiGraphics, x - (outerSize - quadSize) * 0.5F,
                        y - (outerSize - quadSize) * 0.5F, outerSize, opacity * glow * 0.10D, tint);
                drawTintedQuad(guiGraphics, x - (middleSize - quadSize) * 0.5F,
                        y - (middleSize - quadSize) * 0.5F, middleSize, opacity * glow * 0.14D, tint);
                drawTintedQuad(guiGraphics, x - (innerSize - quadSize) * 0.5F,
                        y - (innerSize - quadSize) * 0.5F, innerSize, opacity * glow * 0.18D, tint);
            }
            drawTintedQuad(guiGraphics, x, y, quadSize, opacity, tint);
        } catch (RuntimeException exception) {
            if (!spriteRenderFailureLogged) {
                spriteRenderFailureLogged = true;
                LOGGER.error(
                        "Peripheral's off-screen hologram sprite render failed; the GUI state was restored.",
                        exception
                );
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void ensureSpriteTarget() {
        if (spriteTarget == null) {
            spriteTarget = new TextureTarget(SPRITE_SIZE, SPRITE_SIZE, true, Minecraft.ON_OSX);
            spriteTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private static void drawTintedQuad(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float size,
            double opacity,
            HologramColor tint
    ) {
        int alpha = (int) Math.round(Math.max(0.0D, Math.min(1.0D, opacity)) * 255.0D);
        if (alpha <= 0) {
            return;
        }
        drawTexturedQuad(guiGraphics, x, y, size, alpha, tint);
    }

    private static void drawTexturedQuad(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float size,
            int alpha,
            HologramColor tint
    ) {
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, spriteTarget.getColorTextureId());

            Matrix4f matrix = guiGraphics.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            // Render-target textures are upside down when sampled by the GUI.
            addSpriteVertex(buffer, matrix, x, y, 0.0F, 1.0F, tint, alpha);
            addSpriteVertex(buffer, matrix, x, y + size, 0.0F, 0.0F, tint, alpha);
            addSpriteVertex(buffer, matrix, x + size, y + size, 1.0F, 0.0F, tint, alpha);
            addSpriteVertex(buffer, matrix, x + size, y, 1.0F, 1.0F, tint, alpha);
            Tesselator.getInstance().end();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }
    }

    private static void addSpriteVertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x,
            float y,
            float u,
            float v,
            HologramColor tint,
            int alpha
    ) {
        buffer.vertex(matrix, x, y, 0.0F)
                .uv(u, v)
                .color(channel(tint.red()), channel(tint.green()), channel(tint.blue()), alpha)
                .endVertex();
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    private static boolean renderSprite(Minecraft minecraft, HologramTrack track, float partialTick) {
        RenderTarget target = spriteTarget;
        target.bindWrite(true);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);

        boolean projectionBackedUp = false;
        boolean modelViewPushed = false;
        boolean rendered;
        PoseStack modelView = RenderSystem.getModelViewStack();
        try {
            RenderSystem.backupProjectionMatrix();
            projectionBackedUp = true;
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(-1.0F, 1.0F, -1.0F, 1.0F, -10.0F, 10.0F),
                    com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z
            );

            modelView.pushPose();
            modelViewPushed = true;
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();

            rendered = HologramModelRenderer.renderIntoSprite(minecraft, track.entity(), target, partialTick);
            if (!rendered) {
                target.bindWrite(true);
                target.clear(Minecraft.ON_OSX);
                rendered = HologramFallbackRenderer.renderIntoSprite(track.entity());
            }
        } finally {
            target.unbindWrite();
            minecraft.getMainRenderTarget().bindWrite(true);

            if (modelViewPushed) {
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
            if (projectionBackedUp) {
                RenderSystem.restoreProjectionMatrix();
            }
        }
        return rendered;
    }
}
