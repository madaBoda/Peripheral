package net.korostopia.Peripheral.client;

import net.korostopia.Peripheral.Peripheral;
import net.korostopia.Peripheral.client.input.PeripheralKeyMappings;
import net.korostopia.Peripheral.client.math.CameraFovCache;
import net.korostopia.Peripheral.client.render.ThreatHologramOverlay;
import net.korostopia.Peripheral.config.PeripheralClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Peripheral.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class PeripheralForgeClientEvents {
    private PeripheralForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (PeripheralKeyMappings.TOGGLE_HOLOGRAMS.consumeClick()) {
            PeripheralKeyMappings.toggleHolograms();
        }
    }

    // Ignore temporary FOV requests from held items and special render passes.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            CameraFovCache.capture(event.getCamera(), event.getFOV());
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!PeripheralClientConfig.SHOW_IN_MENUS.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        // Draw after the screen so the holograms sit above opaque menu panels.
        ThreatHologramOverlay.renderOnScreen(
                event.getScreen(),
                event.getGuiGraphics(),
                event.getPartialTick(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight()
        );
    }
}
