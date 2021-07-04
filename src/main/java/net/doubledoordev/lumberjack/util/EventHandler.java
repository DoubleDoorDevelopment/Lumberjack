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

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import net.doubledoordev.lumberjack.Lumberjack;
import net.doubledoordev.lumberjack.LumberjackConfig;
import net.doubledoordev.lumberjack.items.ItemLumberAxe;

/**
 * This event handler relies on the interaction of both events.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandler
{
    @SubscribeEvent
    public static void onRegisterItem(final RegistryEvent.Register<Item> event)
    {
        for (ItemTier itemTier : ItemTier.values())
        {
            event.getRegistry().register(new ItemLumberAxe(itemTier, new Item.Properties().tab(ItemGroup.TAB_TOOLS)).setRegistryName(itemTier.name().toLowerCase() + "_lumberaxe"));
        }
    }

    public EventHandler()
    {
    }

    // Keeps track of the chopped blocks across multiple ticks, until there is no more left. Then gets cleared.
    private final HashMultimap<UUID, BlockPos> pointMap = HashMultimap.create();
    // Keeps track of what blocks to chop next tick.
    private final HashMultimap<UUID, BlockPos> nextMap = HashMultimap.create();

    /*
     * To avoid the server lagging to death for large tries.
     */
    @SubscribeEvent
    public void tickEvent(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.side != LogicalSide.SERVER) return;

        final UUID uuid = event.player.getUUID();

        // If there are no blocks to chop, return
        if (!nextMap.containsKey(uuid) || nextMap.get(uuid).isEmpty()) return;

        // Immutable and not an iterator because breakEvent can modify this list!
        int i = 0;
        for (BlockPos point : ImmutableSet.copyOf(nextMap.get(uuid)))
        {
            // This indirectly causes breakEvent to be invoked
            ((ServerPlayerEntity) event.player).gameMode.destroyBlock(point);
            // Remove the current point
            nextMap.remove(uuid, point);
            if (i++ > LumberjackConfig.GENERAL.tickLimit.get()) break;
        }
        // If more blocks then the total limit have been chopped, clear out the next list, thereby breaking the chain
        if (pointMap.get(uuid).size() > LumberjackConfig.GENERAL.totalLimit.get()) nextMap.removeAll(uuid);
        // If the next map does not reference this player anymore, we can get rid of the old data
        if (!nextMap.containsKey(uuid) || !nextMap.get(uuid).isEmpty()) pointMap.removeAll(uuid);
    }

    @SubscribeEvent
    public void breakEvent(BlockEvent.BreakEvent event)
    {
        final PlayerEntity player = event.getPlayer();
        if (player == null) return;

        // Only interact if  the item matches
        ItemStack itemStack = player.getMainHandItem();
        if (itemStack == ItemStack.EMPTY || !(itemStack.getItem() instanceof ItemLumberAxe)) return;

        ITag<Block> destroyConnectedTag = BlockTags.getAllTags().getTagOrEmpty(new ResourceLocation(Lumberjack.MOD_ID, "destroy_connected"));
        ITag<Block> ignoreConnectedTag = BlockTags.getAllTags().getTagOrEmpty(new ResourceLocation(Lumberjack.MOD_ID, "ignore_connected"));

        // Only interact if wood or leaves
        final UUID uuid = player.getUUID();
        final BlockState state = event.getState();

        if (!shalCut(state, destroyConnectedTag, ignoreConnectedTag))
            return;

        // We are chopping the current block, so save that info
        pointMap.put(uuid, event.getPos());

        // For each block in a 3x3x3 cube around this one
        for (int offsetX = -1; offsetX <= 1; offsetX++)
        {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            {
                for (int offsetY = -1; offsetY <= 1; offsetY++)
                {
                    BlockPos newPoint = event.getPos().offset(offsetX, offsetY, offsetZ);
                    // Avoid doing the same block more then once
                    if (nextMap.containsEntry(uuid, newPoint) || pointMap.containsEntry(uuid, newPoint)) continue;

                    BlockState newBlockState = event.getWorld().getBlockState(newPoint);
                    boolean isLeaves = LumberjackConfig.GENERAL.leaves.get() && newBlockState.getMaterial() == Material.LEAVES;

                    // Mode 0: leaves or same blocktype
                    // Mode 1: leaves or all wood
                    // (if block isn't ignored and is we in mode 0 and (leaves or matching state)) or (if mode 1 && (leaves or should be cut)) place in map.
                    if (!ignoreConnectedTag.contains(newBlockState.getBlock()) && LumberjackConfig.GENERAL.mode.get() == 0 && (isLeaves || newBlockState.getBlock() == state.getBlock())
                            || LumberjackConfig.GENERAL.mode.get() == 1 && (isLeaves || shalCut(newBlockState, destroyConnectedTag, ignoreConnectedTag)))
                        nextMap.put(uuid, newPoint); // Add the block for next tick
                }
            }
        }
    }

    private boolean shalCut(BlockState state, ITag<Block> destroyTag, ITag<Block> ignoreTag)
    {
        Block block = state.getBlock();

        if (ignoreTag.contains(block))
            return false;

        Material material = state.getMaterial();

        if (LumberjackConfig.GENERAL.useMaterials.get() && (material == Material.WOOD || material == Material.NETHER_WOOD))
            return true;

        if (LumberjackConfig.GENERAL.leaves.get() && (material == Material.LEAVES))
            return true;

        return destroyTag.contains(block);
    }
}
