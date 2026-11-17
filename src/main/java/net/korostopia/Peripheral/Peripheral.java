package net.korostopia.Peripheral;

import net.korostopia.Peripheral.config.PeripheralConfig;
import net.korostopia.Peripheral.debug.PeripheralDebug;
import net.korostopia.Peripheral.debug.PeripheralDebugConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Peripheral.MOD_ID)
public final class Peripheral {
    public static final String MOD_ID = "peripheral";

    public Peripheral() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PeripheralDebug.prepareConfigDirectory();
        }

        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.CLIENT, PeripheralConfig.SPEC);
        context.registerConfig(
                ModConfig.Type.CLIENT,
                PeripheralDebugConfig.SPEC,
                "peripheral/debug/debug.toml"
        );
    }
}
