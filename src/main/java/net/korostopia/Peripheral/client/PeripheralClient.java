package net.korostopia.Peripheral.client;

import net.korostopia.Peripheral.Peripheral;
import net.korostopia.Peripheral.client.render.ThreatHologramOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Peripheral.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PeripheralClient {
    private PeripheralClient() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("threat_holograms", ThreatHologramOverlay::render);
    }
}
