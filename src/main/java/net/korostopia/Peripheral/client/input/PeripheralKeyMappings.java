package net.korostopia.Peripheral.client.input;

import net.korostopia.Peripheral.Peripheral;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = Peripheral.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PeripheralKeyMappings {
    public static final KeyMapping TOGGLE_HOLOGRAMS = new KeyMapping(
            "key.peripheral.toggle_holograms",
            GLFW.GLFW_KEY_H,
            "key.categories.peripheral"
    );

    private static boolean hologramsEnabled = true;

    private PeripheralKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_HOLOGRAMS);
    }

    public static boolean areHologramsEnabled() {
        return hologramsEnabled;
    }

    public static void toggleHolograms() {
        hologramsEnabled = !hologramsEnabled;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui != null) {
            minecraft.gui.setOverlayMessage(
                    Component.translatable(hologramsEnabled
                            ? "message.peripheral.holograms_enabled"
                            : "message.peripheral.holograms_disabled"),
                    false
            );
        }
    }
}
