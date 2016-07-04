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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import net.doubledoordev.d3core.D3Core;
import net.doubledoordev.lumberjack.client.ClientHelper;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

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
    private HashMultimap<String, BlockPos> pointMap = HashMultimap.create();
    private HashMultimap<String, BlockPos> nextMap = HashMultimap.create();
    private Configuration configuration;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        MinecraftForge.EVENT_BUS.register(this);

        configuration = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (D3Core.isDebug()) logger.info("Registering all tools");
        for (Item.ToolMaterial material : Item.ToolMaterial.values())
        {
            ItemStack repairStack = ItemStack.copyItemStack(material.getRepairItemStack());
            //noinspection ConstantConditions
            if (repairStack == null)
            {
                if (D3Core.isDebug()) logger.warn("The ToolMaterial " + material + " doesn't have a repair/crafting item set. No LumberAxe from that! You can use the materials.json file from D3Core to add an item yourself, or ask the mod author.");
                continue;
            }

            try
            {
                new ItemLumberAxe(material, repairStack);
            }
            catch (Exception e)
            {
                logger.warn("Something went wrong registering a lumberaxe. This is not a crash, the lumberaxe for material '" + material + "' will not exist.", e);
            }
        }
        if (D3Core.isDebug()) ItemLumberAxe.debug();
        if (event.getSide().isClient()) ClientHelper.init();
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        if (!event.side.isServer()) return;

        String name = event.player.getName();
        if (!nextMap.containsKey(name) || nextMap.get(name).isEmpty()) return;
        for (BlockPos point : ImmutableSet.copyOf(nextMap.get(name)))
        {
            ((EntityPlayerMP) event.player).interactionManager.tryHarvestBlock(point);
            nextMap.remove(name, point);
            if (pointMap.get(name).size() > limit) nextMap.removeAll(name);
        }
        if (!nextMap.containsKey(name) || !nextMap.get(name).isEmpty()) pointMap.removeAll(name);
    }

    @SubscribeEvent
    public void breakEvent(BlockEvent.BreakEvent event)
    {
        if (event.getPlayer() == null) return;
        if (!(event.getState().getMaterial() == Material.WOOD || (leaves && event.getState().getMaterial() == Material.LEAVES))) return;

        ItemStack itemStack = event.getPlayer().getHeldItemMainhand();
        if (itemStack == null || !(itemStack.getItem() instanceof ItemLumberAxe)) return;

        String name = event.getPlayer().getName();
        pointMap.put(name, event.getPos());

        for (int offsetX = -1; offsetX <= 1; offsetX++)
        {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            {
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    BlockPos newPoint = event.getPos().add(offsetX, offsetY, offsetZ);
                    if (nextMap.containsEntry(name, newPoint) || pointMap.containsEntry(name, newPoint)) continue;

                    IBlockState newBlockState = event.getWorld().getBlockState(newPoint);
                    boolean isLeaves = leaves && newBlockState.getMaterial() == Material.LEAVES;

                    if ((mode == 0 && (isLeaves || newBlockState.getBlock() == event.getState().getBlock()))
                            || mode == 1 && (isLeaves || newBlockState.getMaterial() == Material.WOOD))
                        nextMap.put(name, newPoint);
                }
            }
        }
    }

    private void syncConfig()
    {
        configuration.setCategoryLanguageKey(MODID, "d3.lumberjack.config.lumberjack");
        limit = configuration.getInt("limit", MODID, limit, 1, 10000, "Hard limit of the amount that can be broken in one go. If you put this too high you might crash your server!! The maximum is dependant on your RAM settings.");
        mode = configuration.getInt("mode", MODID, mode, 0, 1, "Valid modes:\n0: Only chop blocks with the same blockid\n1: Chop all wooden blocks");
        leaves = configuration.getBoolean("leaves", MODID, leaves, "Harvest leaves too.");

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
}
