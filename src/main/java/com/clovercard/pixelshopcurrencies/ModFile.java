package com.clovercard.pixelshopcurrencies;
import com.clovercard.pixelshopcurrencies.commands.ViewCurrenciesCommand;
import com.clovercard.pixelshopcurrencies.listeners.BoughtFromShopEvent;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModFile.MOD_ID)
@Mod.EventBusSubscriber(modid = ModFile.MOD_ID)
public class ModFile {
    public static final String MOD_ID = "pixelshopcurrencies";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        ModFile.LOGGER.info("Registering Listeners...");
        Pixelmon.EVENT_BUS.register(new BoughtFromShopEvent());
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ModFile.LOGGER.info("Registering Commands...");
        new ViewCurrenciesCommand(event.getDispatcher());
    }
}
