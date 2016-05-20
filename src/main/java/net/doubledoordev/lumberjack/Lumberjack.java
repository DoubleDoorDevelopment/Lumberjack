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

package net.doubledoordev.lumberjack;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.doubledoordev.d3core.D3Core;
import net.doubledoordev.d3core.util.ID3Mod;
import net.doubledoordev.lumberjack.Proxy.IProxy;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;
import net.doubledoordev.lumberjack.util.Point;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.BlockEvent;

import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static net.doubledoordev.lumberjack.util.Constants.*;

/**
 * @author Dries007
 */
@Mod(modid = MODID, dependencies = "after:D3Core")
public class Lumberjack implements ID3Mod
{
    @Mod.Instance(MODID)
    public static Lumberjack instance;

    @SidedProxy(clientSide = "net.doubledoordev.lumberjack.Proxy.ClientProxy", serverSide = "net.doubledoordev.lumberjack.Proxy.CommonProxy")
    public static IProxy proxy;
    public Logger logger;

    public int                         limit    = 1024;
    public int                         mode     = 0;
    public boolean                     leaves   = false;
    public HashMultimap<String, Point> pointMap = HashMultimap.create();
    public HashMultimap<String, Point> nextMap  = HashMultimap.create();
    private Configuration configuration;
    private static ArrayList<ItemLumberAxe> lumberAxes = new ArrayList<>();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);

        configuration = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (D3Core.debug()) logger.info("Registering all tools");
        HashSet<ItemStack> itemStack = new HashSet<>(Item.ToolMaterial.values().length);
        for (Item.ToolMaterial material : Item.ToolMaterial.values())
        {
            try
            {
                if (material.getRepairItemStack() == null)
                {
                    if (D3Core.debug()) logger.warn("The ToolMaterial " + material + " doesn't have a crafting itemstack set. No LumberAxe from that!");
                }
                else if (itemStack.contains(material.getRepairItemStack()))
                {
                    if (D3Core.debug()) logger.warn("The ToolMaterial " + material + " uses an itemStack that has already been used.");
                }
                else
                {
                    try
                    {
                        lumberAxes.add(new ItemLumberAxe(material));
                        itemStack.add(material.getRepairItemStack());
                    }
                    catch (Exception e)
                    {
                        // Noop
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Some ToolMaterial has a corrupt state: " + material);
            }
        }
        if (D3Core.debug())
        {
            logger.info("Table of materials");
            logger.info(makeTable(new TableData("Tool Material", ItemLumberAxe.toolMaterials),
                    new TableData("Texture string", ItemLumberAxe.textureStrings),
                    new TableData("Item name", ItemLumberAxe.itemNames),
                    new TableData("Crafting Items", ItemLumberAxe.craftingItems)));
        }

        proxy.registerItemRenders(event);
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        if (!event.side.isServer()) return;

        String name = event.player.getCommandSenderEntity().getName();
        if (!nextMap.containsKey(name) || nextMap.get(name).isEmpty()) return;
        for (Point point : ImmutableSet.copyOf(nextMap.get(name)))
        {
            ((EntityPlayerMP) event.player).interactionManager.tryHarvestBlock(new BlockPos(point.x, point.y, point.z));
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

        String name = event.getPlayer().getCommandSenderEntity().getName();
        pointMap.put(name, new Point(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));

        for (int offsetX = -1; offsetX <= 1; offsetX++)
        {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            {
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    int newX = event.getPos().getX() + offsetX;
                    int newY = event.getPos().getY() + offsetY;
                    int newZ = event.getPos().getZ() + offsetZ;

                    Point newPoint = new Point(newX, newY, newZ);
                    if (nextMap.containsEntry(name, newPoint) || pointMap.containsEntry(name, newPoint)) continue;

                    Block newBlock = event.getWorld().getBlockState(new BlockPos(newX,newY,newZ)).getBlock();
                    switch (mode)
                    {
                        case 0:
                            if (!(newBlock.equals(event.getState().getBlock()) || (leaves && newBlock.getMaterial(event.getState()).equals(Material.LEAVES)))) continue;
                            break;

                        case 1:
                            if (!(newBlock.getMaterial(event.getState()).equals(Material.WOOD) || (leaves && newBlock.getMaterial(event.getState()).equals(Material.LEAVES)))) continue;
                            break;
                    }

                    nextMap.put(name, newPoint);
                }
            }
        }
    }

    @Override
    public void syncConfig()
    {
        configuration.setCategoryLanguageKey(MODID, "d3.lumberjack.config.lumberjack");
        limit = configuration.getInt("limit", MODID, limit, 1, 10000, "Hard limit of the amount that can be broken in one go. If you put this too high you might crash your server!! The maximum is dependant on your RAM settings.");
        mode = configuration.getInt("mode", MODID, mode, 0, 1, "Valid modes:\n0: Only chop blocks with the same blockid\n1: Chop all wooden blocks");
        leaves = configuration.getBoolean("leaves", MODID, leaves, "Harvest leaves too.");

        if (configuration.hasChanged()) configuration.save();
    }

    @Override
    public void addConfigElements(List<IConfigElement> configElements)
    {
        configElements.add(new ConfigElement(configuration.getCategory(MODID.toLowerCase())));
    }

    public static ArrayList<ItemLumberAxe> getLumberAxes()
    {
        return lumberAxes;
    }
}
