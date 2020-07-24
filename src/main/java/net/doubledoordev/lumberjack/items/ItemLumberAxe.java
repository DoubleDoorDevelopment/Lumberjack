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

package net.doubledoordev.lumberjack.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.doubledoordev.lumberjack.LumberjackConfig;


public class ItemLumberAxe extends AxeItem
{
    private final Multimap<Attribute, AttributeModifier> attributeMap;

    int baseDurability;

    public ItemLumberAxe(ItemTier itemTier, Item.Properties builder)
    {
        super(itemTier,
                (itemTier.getAttackDamage()),
                (itemTier.getEfficiency()),
                builder.addToolType(net.minecraftforge.common.ToolType.AXE,
                        itemTier.getHarvestLevel())
        );
        baseDurability = itemTier.getMaxUses();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> attibuteBuilder = ImmutableMultimap.builder();
        attibuteBuilder.put(Attributes.field_233823_f_, new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", LumberjackConfig.GENERAL.damageMultiplier.get() * itemTier.getAttackDamage(), AttributeModifier.Operation.ADDITION));
        attibuteBuilder.put(Attributes.field_233825_h_, new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", LumberjackConfig.GENERAL.speed.get(), AttributeModifier.Operation.ADDITION));
        attributeMap = attibuteBuilder.build();

        //builder.maxDamage(Math.max((int) (itemTier.getMaxUses() * LumberjackConfig.GENERAL.durabilityMultiplier.get()), 1));

//        Ingredient repairStack = itemTier.getRepairMaterial();
//        if (repairStack != ItemStack.EMPTY)
//        {
//            int[] ids = OreDictionary.getOreIDs(repairStack);
//            if (ids.length == 0)
//            {
//            	IRecipe temp = new ShapedOreRecipe(null, this, "XX", "SX", "SX", 'S', "stickWood", 'X', repairStack).setMirrored(true).setRegistryName(new ResourceLocation(Constants.MODID, this.getToolMaterialName()));
//            	if(temp != null)
//            		Lumberjack.recipesList.add(temp);
//            }
//            else
//            {
//                for (int id : ids)
//                {
//                	IRecipe temp = new Shaped(null, this, "XX", "SX", "SX", 'S', "stickWood", 'X', OreDictionary.getOreName(id)).setMirrored(true).setRegistryName(new ResourceLocation(Constants.MODID, this.getToolMaterialName()));
//                	if(temp != null)
//                		Lumberjack.recipesList.add(temp);
//                }
//            }
//        }
//        else Lumberjack.getLogger().info("LumberAxe {} without recipe! Ask the mod author of {} for a ToolMaterial repairStack.", materialName, toolMaterial);

    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        return (int) (baseDurability * LumberjackConfig.GENERAL.durabilityMultiplier.get());
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving)
    {
        return stack != ItemStack.EMPTY && (Material.LEAVES.equals(state.getMaterial()) || super.onBlockDestroyed(stack, worldIn, state, pos, entityLiving));
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot)
    {
        return equipmentSlot == EquipmentSlotType.MAINHAND ? attributeMap : super.getAttributeModifiers(equipmentSlot);
    }
}
