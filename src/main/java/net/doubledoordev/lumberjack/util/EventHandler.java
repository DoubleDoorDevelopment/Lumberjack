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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * This event handler relies on the interaction of both events.
 */
public class EventHandler
{
    public static final EventHandler I = new EventHandler();

    private EventHandler()
    {
    }

    // Keeps track of the chopped blocks across multiple ticks, until there is no more left. Then gets cleared.
    private HashMultimap<UUID, BlockPos> pointMap = HashMultimap.create();
    // Keeps track of what blocks to chop next tick.
    private HashMultimap<UUID, BlockPos> nextMap = HashMultimap.create();

    /*
     * To avoid the server lagging to death for large tries.
     */
    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        if (!event.side.isServer()) return;

        final UUID uuid = event.player.getUniqueID();

        // If there are no blocks to chop, return
        if (!nextMap.containsKey(uuid) || nextMap.get(uuid).isEmpty()) return;

        // Immutable and not an iterator because breakEvent can modify this list!
        int i = 0;
        for (BlockPos point : ImmutableSet.copyOf(nextMap.get(uuid)))
        {
            // This indirectly causes breakEvent to be invoked
            ((EntityPlayerMP) event.player).interactionManager.tryHarvestBlock(point);
            // Remove the current point
            nextMap.remove(uuid, point);
            if (i ++ > Lumberjack.getTickLimit()) break;
        }
        // If more blocks then the total limit have been chopped, clear out the next list, thereby breaking the chain
        if (pointMap.get(uuid).size() > Lumberjack.getTotalLimit()) nextMap.removeAll(uuid);
        // If the next map does not reference this player anymore, we can get rid of the old data
        if (!nextMap.containsKey(uuid) || !nextMap.get(uuid).isEmpty()) pointMap.removeAll(uuid);
    }

    @SubscribeEvent
    public void breakEvent(BlockEvent.BreakEvent event)
    {
        final EntityPlayer player = event.getPlayer();
        if (player == null) return;
        final UUID uuid = player.getUniqueID();
        final IBlockState state = event.getState();
        // Only interact if wood or leaves
        if (!(state.getMaterial() == Material.WOOD || (Lumberjack.getLeaves() && state.getMaterial() == Material.LEAVES))) return;

        // Only interact if  the item matches
        ItemStack itemStack = player.getHeldItemMainhand();
        if (itemStack == null || !(itemStack.getItem() instanceof ItemLumberAxe)) return;

        // We are chopping the current block, so save that info
        pointMap.put(uuid, event.getPos());

        // For each block in a 3x3x3 cube around this one
        for (int offsetX = -1; offsetX <= 1; offsetX++)
        {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            {
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    BlockPos newPoint = event.getPos().add(offsetX, offsetY, offsetZ);
                    // Avoid doing the same block more then once
                    if (nextMap.containsEntry(uuid, newPoint) || pointMap.containsEntry(uuid, newPoint)) continue;

                    IBlockState newBlockState = event.getWorld().getBlockState(newPoint);
                    boolean isLeaves = Lumberjack.getLeaves() && newBlockState.getMaterial() == Material.LEAVES;

                    // Mode 0: leaves or same blocktype
                    // Mode 1: leaves or all wood
                    if ((Lumberjack.getMode() == 0 && (isLeaves || newBlockState.getBlock() == state.getBlock()))
                            || Lumberjack.getMode() == 1 && (isLeaves || newBlockState.getMaterial() == Material.WOOD))
                        nextMap.put(uuid, newPoint); // Add the block for next tick
                }
            }
        }
    }
}
