package net.doubledoordev.lumberjack;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.doubledoordev.lumberjack.util.Constants;


@Config(modid = Constants.MODID, category = "All")
@Mod.EventBusSubscriber(modid = Constants.MODID)
@Config.LangKey("lumberjack.config.lumberjack")
public class ModConfig
{
    @SubscribeEvent
    public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(Constants.MODID))
        {
            ConfigManager.sync(Constants.MODID, Config.Type.INSTANCE);
        }
    }

    @Config.Name("Total Limit")
    @Config.LangKey("lumberjack.config.totalLimit")
    @Config.Comment({
            "Hard limit of the amount that can be broken in one go."
    })
    @Config.RangeInt(min = 1, max = 10000)
    public static int totalLimit = 1024;

    @Config.Name("Tick Limit")
    @Config.LangKey("lumberjack.config.tickLimit")
    @Config.Comment({
            "Hard limit of the amount that can be broken in one go."
    })
    @Config.RangeInt(min = 1, max = 10000)
    public static int tickLimit = 32;

    @Config.Name("Axe Mode")
    @Config.LangKey("lumberjack.config.mode")
    @Config.Comment({
            "Valid modes:\n0: Only chop blocks with the same blockid\n1: Chop all wooden blocks"
    })
    @Config.RangeInt(min = 0, max = 1)
    public static int mode = 0;

    @Config.Name("Chop Leaves")
    @Config.LangKey("lumberjack.config.leaves")
    @Config.Comment("Harvest leaves too.")
    public static boolean leaves = false;

    @Config.Name("Use all Materials")
    @Config.LangKey("lumberjack.config.useAllMaterials")
    @Config.Comment("If you set this to false, we will only clone other axes, and not try to use all ToolMaterials.")
    public static boolean useAllMaterials = false;

    @Config.Name("Ban List")
    @Config.LangKey("lumberjack.config.banList")
    @Config.Comment({
            "A list of names you don't want to see as lumberaxes.\n" +
                    "Not case sensitive, but it uses the RAW ToolMaterial name. AKA it does not strip modid's or oter 'unique making techniques' mod authors may use to prevent conflicts with materials from other mods.\n" +
                    "Use * as a wildcard at the beginning or end to match with endsWith or startsWith respectively.\n" +
                    "Example: 'BASEMETALS_*' will prevent any material that starts with 'BASEMETALS_' from becoming a lumberaxe."
    })
    public static String[] banList = new String[0];
}
