/*
 * Copyright (c) 2014-2016, Dries007 & DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of DoubleDoorDevelopment nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package net.doubledoordev.lumberjack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.doubledoordev.lumberjack.client.ClientHelper;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;
import net.doubledoordev.lumberjack.util.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.regex.Pattern;

import static net.doubledoordev.lumberjack.util.Constants.*;

/**
 * @author Dries007
 */
@Mod(modid = MODID, name = MODID, updateJSON = UPDATE_URL, guiFactory = MOD_GUI_FACTORY, dependencies = "after:D3Core")
public class Lumberjack
{
    @Mod.Instance(MODID)
    public static Lumberjack instance;

    private Logger logger;
    private int limit = 1024;
    private int mode = 0;
    private boolean leaves = false;
    private boolean useAllMaterials = true;
    
    private Configuration configuration;
    private String[] banList;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        configuration = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();

        MinecraftForge.EVENT_BUS.register(EventHandler.I);
    }

    /**
     * While it's in general not good to register items in init instead of preInit, I feel here it is appropriate.
     * This avoids a more complex mod sorting order
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        HashSet<Item.ToolMaterial> unusedMaterials = Sets.newHashSet(Item.ToolMaterial.values());
        // First (since it's the way we can get more accurate damage/speed values) we find all axes
        for (Item i : ImmutableList.copyOf(Item.REGISTRY))
        {
            if (!(i instanceof ItemAxe)) continue;
            try
            {
                ItemAxe axe = ((ItemAxe) i);
                Item.ToolMaterial m = axe.getToolMaterial();
                if (m == null || m.name() == null)
                {
                    logger.error("Found horribly broken axe {} with material {}. Please report.", i.getRegistryName(), m);
                    continue;
                }
                logger.info("Found an axe {} with material {} ({})", i.getRegistryName(), m, ItemLumberAxe.normalizeName(m));

                if (!unusedMaterials.remove(m) || ItemLumberAxe.usedMaterial(m))
                {
                    logger.info("Material {} already in use.", m);
                    continue;
                }

                if (isBlacklisted(m.name())) continue;

                new ItemLumberAxe(m, axe);
            }
            catch (Exception e)
            {
                logger.error("Something went wrong registering a lumberaxe. This is not a crash, the lumberaxe for axe '" + i.getRegistryName() + "' will not exist. Please report.", e);
            }
        }

        // Now we do all other toolmaterials, if allowed by user settings
        if (useAllMaterials)
        {
            for (Item.ToolMaterial m : unusedMaterials)
            {
                try
                {
                    if (m == null || m.name() == null)
                    {
                        logger.error("Found horribly broken material {}. Please report.", m);
                        continue;
                    }
                    logger.info("Found an unused material {} ({})", m, ItemLumberAxe.normalizeName(m));

                    if (ItemLumberAxe.usedMaterial(m))
                    {
                        logger.info("Material {} already in use, probably under a (differently) prefixed name.", m);
                        continue;
                    }

                    if (isBlacklisted(m.name())) continue;

                    new ItemLumberAxe(m);
                }
                catch (Exception e)
                {
                    logger.warn("Something went wrong registering a lumberaxe. This is not a crash, the lumberaxe for ToolMaterial '" + m + "' will not exist.", e);
                }
            }
        }
        if (event.getSide().isClient()) ClientHelper.init();
    }

    public static boolean isBlacklisted(String name)
    {
        for (String ban : instance.banList)
        {
            if (ban.equalsIgnoreCase(name))
            {
                instance.logger.info("Material {} is blacklisted. It matches {} literally.", name, ban);
                return true;
            }
        }
        return false;
    }

    private void syncConfig()
    {
        configuration.setCategoryLanguageKey(MODID, "d3.lumberjack.config.lumberjack");
        limit = configuration.getInt("limit", MODID, limit, 1, 10000, "Hard limit of the amount that can be broken in one go. If you put this too high you might crash your server!! The maximum is dependant on your RAM settings.");
        mode = configuration.getInt("mode", MODID, mode, 0, 1, "Valid modes:\n0: Only chop blocks with the same blockid\n1: Chop all wooden blocks");
        leaves = configuration.getBoolean("leaves", MODID, leaves, "Harvest leaves too.");
        useAllMaterials = configuration.getBoolean("useAllMaterials", MODID, useAllMaterials, "If you set this to false, we will only clone other axes, and not try to use all ToolMaterials.");
        banList = configuration.get("banlist", MODID, new String[0],
                "A list of names you don't want to see as lumberaxes.\n" +
                        "Not case sensitive, but it uses the RAW ToolMaterial name. AKA it does not strip modid's or oter 'unique making techniques' mod authors may use to prevent conflicts with materials from other mods.\n" +
                        "Use * as a wildcard at the beginning or end to match with endsWith or startsWith respectively.\n" +
                        "Example: 'BASEMETALS_*' will prevent any material that starts with 'BASEMETALS_' from becoming a lumberaxe.", Pattern.compile("^\\*[A-z0-9_:|]+$|^[A-z0-9_:|]+\\*$|^[A-z0-9_:|]+$")).getStringList();

        if (configuration.hasChanged()) configuration.save();
    }

    public static Configuration getConfig()
    {
        return instance.configuration;
    }

    public static Logger getLogger()
    {
        return instance.logger;
    }

    public static int getLimit()
    {
        return instance.limit;
    }

    public static boolean getLeaves()
    {
        return instance.leaves;
    }

    public static int getMode()
    {
        return instance.mode;
    }
}
