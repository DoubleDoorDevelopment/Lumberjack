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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.Logger;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.doubledoordev.lumberjack.client.ClientHelper;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;
import net.doubledoordev.lumberjack.util.Constants;
import net.doubledoordev.lumberjack.util.EventHandler;

import static net.doubledoordev.lumberjack.util.Constants.MODID;

/**
 * @author Dries007
 */
@Mod(modid = Constants.MODID,
		name = Constants.MOD_NAME,
		version = Constants.VERSION)
public class Lumberjack
{
    @Mod.Instance(MODID)
    public static Lumberjack instance;

    private static Logger logger;
    private int totalLimit = ModConfig.totalLimit;
    private int tickLimit = ModConfig.tickLimit;
    private int mode = ModConfig.mode;
    private boolean leaves = ModConfig.leaves;
    private static boolean useAllMaterials = ModConfig.useAllMaterials;

    private String[] banList = ModConfig.banList;
    
	public static List<Item> itemList = new ArrayList<>();
	public static List<IRecipe> recipesList = new ArrayList<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(EventHandler.I);
        MinecraftForge.EVENT_BUS.register(new RegistrationHandler());

    }

    /**
     * While it's in general not good to register items in init instead of preInit, I feel here it is appropriate.
     * This avoids a more complex mod sorting order
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (event.getSide().isClient()) ClientHelper.init();
    }

	public static void registerItem(Item item, String name) {
		if (item.getRegistryName() == null) {
			item.setRegistryName(name);
		}
		itemList.add(item);
	}
	
	@Mod.EventBusSubscriber(modid = MODID)
	public static class RegistrationHandler {
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void registerItemsEvent(RegistryEvent.Register<Item> event) {
			HashSet<Item.ToolMaterial> unusedMaterials = Sets.newHashSet(Item.ToolMaterial.values());
	        // First (since it's the way we can get more accurate damage/speed values) we find all axes
	        for (Item i : ImmutableList.copyOf(Item.REGISTRY))
	        {
	            if (!(i instanceof ItemAxe)) continue;
	            try
	            {
	                ItemAxe axe = ((ItemAxe) i);
	                Item.ToolMaterial m = axe.toolMaterial;
	                if (m == null || m.name() == null)
	                {
	                    logger.error("Found horribly broken axe {} with material {}. Please report.", i.getRegistryName(), m);
	                    continue;
	                }

	                if (!unusedMaterials.remove(m) || ItemLumberAxe.usedMaterial(m))
	                {
	                    logger.debug("Material {} ({}) already in use.", m, ItemLumberAxe.normalizeName(m));
	                    continue;
	                }

	                if (isBlacklisted(m.name())) continue;

	                new ItemLumberAxe(m, axe);
	            }
	            catch (Exception e)
	            {
	                logger.warn("New Lumberaxe error. Axe trying to imitate: " + i.getRegistryName(), e);
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

	                    if (ItemLumberAxe.usedMaterial(m))
	                    {
	                        logger.debug("Material {} ({}) already in use.", m, ItemLumberAxe.normalizeName(m));
	                        continue;
	                    }

	                    if (isBlacklisted(m.name())) continue;

	                    new ItemLumberAxe(m);
	                }
	                catch (Exception e)
	                {
	                    logger.warn("New Lumberaxe error. ToolMaterial '" + m + "' will not exist.", e);
	                }
	            }
	        }

			for (Item item : itemList) {
				event.getRegistry().register(item);
			}
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void registerRecipesEvent(RegistryEvent.Register<IRecipe> event) {
			for (IRecipe item : recipesList) {
				event.getRegistry().register(item);
			}
		}
	}

    /**+
     * Does blacklist matching based on rules explained in the coding comment.
     * Also prints out the entry that got hit, to aid in debugging faulty configs.
     */
    public static boolean isBlacklisted(String name)
    {
        // Let's avoid that unpleasantly before it even happens. Cannot print though, since we don't have info.
        if (Strings.isNullOrEmpty(name)) return true;

        for (String ban : instance.banList)
        {
            if (ban.equalsIgnoreCase(name))
            {
                logger.info("Material {} is blacklisted. It matches {} literally.", name, ban);
                return true;
            }
            if (ban.charAt(0) == '*' && name.endsWith(ban.substring(1)))
            {
                logger.info("Material {} is blacklisted. It matches {} as a suffix.", name, ban);
                return true;
            }
            if (ban.charAt(ban.length() - 1) == '*' && name.startsWith(ban.substring(0, ban.length() - 2)))
            {
                logger.info("Material {} is blacklisted. It matches {} as a suffix.", name, ban);
                return true;
            }
        }

        return false;
    }

    public static Logger getLogger()
    {
        return logger;
    }

    public static int getTotalLimit()
    {
        return instance.totalLimit;
    }

    public static int getTickLimit()
    {
        return instance.tickLimit;
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
