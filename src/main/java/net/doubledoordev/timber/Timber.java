/*
 * Copyright (c) 2014, DoubleDoorDevelopment
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
 *  Neither the name of the project nor the names of its
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
 */

package net.doubledoordev.timber;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.doubledoordev.lib.DevPerks;
import net.doubledoordev.timber.items.ItemLumberAxe;
import net.doubledoordev.timber.util.Point;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.logging.log4j.Logger;

import static net.doubledoordev.timber.util.Constants.MODID;

/**
 * @author Dries007
 */
@Mod(modid = MODID)
public class Timber
{
    @Mod.Instance(MODID)
    public static Timber instance;

    public Logger logger;
    public boolean debug = false;
    public int limit = 1024;
    public int mode = 0;
    public boolean leaves = false;
    public HashMultimap<String, Point> pointMap = HashMultimap.create();
    public HashMultimap<String, Point> nextMap  = HashMultimap.create();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());

        limit = configuration.getInt("limit", MODID, limit, 1, 10000, "Hard limit of the amount that can be broken in one go. If you put this too high you might crash your server!! The maximum is dependant on your RAM settings.");
        mode = configuration.getInt("mode", MODID, mode, 0, 1, "Valid modes:\n0: Only chop blocks with the same blockid\n1: Chop all wooden blocks");
        leaves = configuration.getBoolean("leaves", MODID, leaves, "Harvest leaves too.");

        debug = configuration.getBoolean("debug", MODID, debug, "Enable extra debug output.");
        if (configuration.getBoolean("sillyness", MODID, true, "Disable sillyness only if you want to piss off the developers XD")) MinecraftForge.EVENT_BUS.register(new DevPerks(debug));

        if (configuration.hasChanged()) configuration.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        for (Item.ToolMaterial material : Item.ToolMaterial.values())
        {
            try
            {
                new ItemLumberAxe(material);
            }
            catch (Exception e)
            {
                // Noop
            }
        }
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        if (!event.side.isServer()) return;

        String name = event.player.getCommandSenderName();
        if (!nextMap.containsKey(name) || nextMap.get(name).isEmpty()) return;
        for (Point point : ImmutableSet.copyOf(nextMap.get(name)))
        {
            ((EntityPlayerMP) event.player).theItemInWorldManager.tryHarvestBlock(point.x, point.y, point.z);
            nextMap.remove(name, point);
            if (pointMap.get(name).size() > limit) nextMap.removeAll(name);
        }
        if (!nextMap.containsKey(name) || !nextMap.get(name).isEmpty()) pointMap.removeAll(name);
    }

    @SubscribeEvent
    public void breakEvent(BlockEvent.BreakEvent event)
    {
        if (event.getPlayer() == null) return;
        if (!(event.block.getMaterial() == Material.wood || (leaves && event.block.getMaterial() == Material.leaves))) return;

        ItemStack itemStack = event.getPlayer().getHeldItem();
        if (itemStack == null || !(itemStack.getItem() instanceof ItemLumberAxe)) return;

        String name = event.getPlayer().getCommandSenderName();
        pointMap.put(name, new Point(event.x, event.y, event.z));

        for (int offsetX = -1; offsetX <= 1; offsetX++)
        {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            {
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    int newX = event.x + offsetX;
                    int newY = event.y + offsetY;
                    int newZ = event.z + offsetZ;

                    Point newPoint = new Point(newX, newY, newZ);
                    if (nextMap.containsEntry(name, newPoint) || pointMap.containsEntry(name, newPoint)) continue;

                    Block newBlock = event.world.getBlock(newX, newY, newZ);
                    switch (mode)
                    {
                        case 0:
                            if (!(newBlock == event.block || (leaves && newBlock.getMaterial() == Material.leaves))) continue;
                            break;

                        case 1:
                            if (!(newBlock.getMaterial() == Material.wood || (leaves && newBlock.getMaterial() == Material.leaves))) continue;
                            break;
                    }

                    nextMap.put(name, newPoint);
                }
            }
        }
    }
}
