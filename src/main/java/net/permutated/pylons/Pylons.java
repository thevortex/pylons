package net.permutated.pylons;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.permutated.pylons.block.AbstractPylonBlock;
import net.permutated.pylons.client.ClientSetup;
import net.permutated.pylons.item.PlayerFilterCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Pylons.MODID)
public class Pylons
{
    public static final String MODID = "pylons";

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public Pylons() {
        LOGGER.info("Registering mod: {}", MODID);

        ModRegistry.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigManager.COMMON_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetupEvent);
        MinecraftForge.EVENT_BUS.addListener(PlayerFilterCard::onPlayerInteractEvent);
        MinecraftForge.EVENT_BUS.addListener(AbstractPylonBlock::onBlockBreakEvent);
    }

    public void onClientSetupEvent(final FMLClientSetupEvent event) {
        ClientSetup.register();
    }
}
