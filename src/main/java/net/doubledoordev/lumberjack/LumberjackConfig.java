package net.doubledoordev.lumberjack;

import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;

public class LumberjackConfig
{
    public static final LumberjackConfig.General GENERAL;
    static final ForgeConfigSpec spec;

    static
    {
        final Pair<General, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LumberjackConfig.General::new);
        spec = specPair.getRight();
        GENERAL = specPair.getLeft();
    }

    public static class General
    {
        public ForgeConfigSpec.BooleanValue leaves;

        public ForgeConfigSpec.IntValue totalLimit;
        public ForgeConfigSpec.IntValue tickLimit;
        public ForgeConfigSpec.IntValue mode;
        public ForgeConfigSpec.DoubleValue damageMultiplier;
        public ForgeConfigSpec.DoubleValue speed;
        public ForgeConfigSpec.DoubleValue durabilityMultiplier;

        General(ForgeConfigSpec.Builder builder)
        {
            builder.comment("General configuration settings")
                    .push("General");

            totalLimit = builder
                    .comment("Hard limit of the amount that can be broken in one go.")
                    .translation("lumberjack.config.totalLimit")
                    .defineInRange("totalLimit", 1024, 1, Integer.MAX_VALUE);

            tickLimit = builder
                    .comment("Hard limit of the amount that can be broken in one go.")
                    .translation("lumberjack.config.tickLimit")
                    .defineInRange("tickLimit", 32, 1, Integer.MAX_VALUE);

            mode = builder
                    .comment("Valid modes:" +
                            "0: Only chop blocks with the same blockid" +
                            "1: Chop all wooden blocks")
                    .translation("lumberjack.config.mode")
                    .defineInRange("mode", 0, 0, 1);

            leaves = builder
                    .comment("Harvest leaves too.")
                    .translation("lumberjack.config.leaves")
                    .define("leaves", false);

            damageMultiplier = builder
                    .comment("Multiplier used for attack damage. Tool material * this value = axe damage")
                    .translation("lumberjack.config.damageMultiplier")
                    .defineInRange("damageMultiplier", 3.2, 0, Integer.MAX_VALUE);

            durabilityMultiplier = builder
                    .comment("Multiplier used for durability. Tool material * this value = axe durability")
                    .translation("lumberjack.config.durabilityMultiplier")
                    .defineInRange("durabilityMultiplier", 1.5, 0, Integer.MAX_VALUE);

            speed = builder
                    .comment("Speed used for attack speed. 4 - this value = axe speed")
                    .translation("lumberjack.config.speed")
                    .defineInRange("speed", -3.3, -3.9, 0);

        }
    }
}


//
//    @Config.Name("Ban List")
//    @Config.LangKey("lumberjack.config.banList")
//    @Config.Comment({
//            "A list of names you don't want to see as lumberaxes.\n" +
//                    "Not case sensitive, but it uses the RAW ToolMaterial name. AKA it does not strip modid's or oter 'unique making techniques' mod authors may use to prevent conflicts with materials from other mods.\n" +
//                    "Use * as a wildcard at the beginning or end to match with endsWith or startsWith respectively.\n" +
//                    "Example: 'BASEMETALS_*' will prevent any material that starts with 'BASEMETALS_' from becoming a lumberaxe."
//    })
//    public static String[] banList = new String[0];
//}
