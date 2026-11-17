package net.korostopia.Peripheral.debug;

import net.korostopia.Peripheral.Peripheral;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Peripheral.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class PeripheralDebugEvents {
    private PeripheralDebugEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!PeripheralDebug.isEnabled("logScreenClasses")) {
            return;
        }
        Screen screen = event.getNewScreen();
        if (screen != null) {
            PeripheralDebug.log("logScreenClasses", "Opened screen class: {}", screen.getClass().getName());
        }
    }
}
