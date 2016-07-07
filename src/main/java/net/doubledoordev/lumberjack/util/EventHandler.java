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

package net.doubledoordev.lumberjack.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import net.doubledoordev.lumberjack.Lumberjack;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * This mod relies on the fact it calls
 */
public class EventHandler
{
    public static final EventHandler I = new EventHandler();

    private EventHandler()
    {
    }

    private HashMultimap<String, BlockPos> pointMap = HashMultimap.create();
    private HashMultimap<String, BlockPos> nextMap = HashMultimap.create();

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
            if (pointMap.get(name).size() > Lumberjack.getLimit()) nextMap.removeAll(name);
        }
        if (!nextMap.containsKey(name) || !nextMap.get(name).isEmpty()) pointMap.removeAll(name);
    }

    @SubscribeEvent
    public void breakEvent(BlockEvent.BreakEvent event)
    {
        if (event.getPlayer() == null) return;
        if (!(event.getState().getMaterial() == Material.WOOD || (Lumberjack.getLeaves() && event.getState().getMaterial() == Material.LEAVES))) return;

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
                    boolean isLeaves = Lumberjack.getLeaves() && newBlockState.getMaterial() == Material.LEAVES;

                    if ((Lumberjack.getMode() == 0 && (isLeaves || newBlockState.getBlock() == event.getState().getBlock()))
                            || Lumberjack.getMode() == 1 && (isLeaves || newBlockState.getMaterial() == Material.WOOD))
                        nextMap.put(name, newPoint);
                }
            }
        }
    }
}
