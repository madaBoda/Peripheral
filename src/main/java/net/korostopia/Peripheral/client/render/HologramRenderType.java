package net.korostopia.Peripheral.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class HologramRenderType {
    private static final Map<ResourceLocation, RenderType> CACHE = new ConcurrentHashMap<>();

    private HologramRenderType() {
    }

    public static RenderType forTexture(ResourceLocation texture, RenderTarget spriteTarget) {
        return CACHE.computeIfAbsent(texture, id -> create(id, spriteTarget));
    }

    private static RenderType create(ResourceLocation texture, RenderTarget spriteTarget) {
        // Keep model vertices in the off-screen sprite instead of sending them to the main target.
        RenderStateShard.OutputStateShard spriteOutput = new RenderStateShard.OutputStateShard(
                "peripheral_sprite_target",
                () -> spriteTarget.bindWrite(false),
                spriteTarget::unbindWrite
        );

        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(StateAccess.entityTranslucentShader())
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(StateAccess.translucentTransparency())
                .setDepthTestState(StateAccess.depthTest())
                .setCullState(StateAccess.noCull())
                .setLightmapState(StateAccess.lightmap())
                .setOverlayState(StateAccess.overlay())
                .setLayeringState(StateAccess.noLayering())
                .setOutputState(spriteOutput)
                .setWriteMaskState(StateAccess.colorAndDepthWrite())
                .createCompositeState(false);

        return RenderType.create(
                "peripheral_sprite/" + texture.getNamespace() + "/" + texture.getPath(),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                2048,
                false,
                true,
                state
        );
    }

    private static final class StateAccess extends RenderStateShard {
        private StateAccess() {
            super("peripheral_state_access", () -> { }, () -> { });
        }

        private static ShaderStateShard entityTranslucentShader() {
            return RENDERTYPE_ENTITY_TRANSLUCENT_SHADER;
        }

        private static TransparencyStateShard translucentTransparency() {
            return TRANSLUCENT_TRANSPARENCY;
        }

        private static DepthTestStateShard depthTest() {
            return LEQUAL_DEPTH_TEST;
        }

        private static CullStateShard noCull() {
            return NO_CULL;
        }

        private static LightmapStateShard lightmap() {
            return LIGHTMAP;
        }

        private static OverlayStateShard overlay() {
            return OVERLAY;
        }

        private static LayeringStateShard noLayering() {
            return NO_LAYERING;
        }

        private static WriteMaskStateShard colorAndDepthWrite() {
            return COLOR_DEPTH_WRITE;
        }
    }
}
