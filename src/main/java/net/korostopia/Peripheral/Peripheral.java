package net.korostopia.Peripheral;

import net.korostopia.Peripheral.config.PeripheralClientConfig;
import net.korostopia.Peripheral.config.PeripheralServerConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Peripheral.MOD_ID)
public final class Peripheral {
    public static final String MOD_ID = "peripheral";

    public Peripheral() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.CLIENT, PeripheralClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, PeripheralServerConfig.SPEC);
    }
}
