package net.doubledoordev.lumberjack.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.doubledoordev.lumberjack.Lumberjack;

public class TagKeys
{
    public static final TagKey<Block> DESTROY_CONNECTED = BlockTags.create(new ResourceLocation(Lumberjack.MOD_ID, "destroy_connected"));
    public static final TagKey<Block> IGNORE_CONNECTED = BlockTags.create(new ResourceLocation(Lumberjack.MOD_ID, "ignore_connected"));
}
