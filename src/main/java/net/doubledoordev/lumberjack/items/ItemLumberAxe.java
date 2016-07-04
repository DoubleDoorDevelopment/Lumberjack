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

import com.google.common.collect.Multimap;
import net.doubledoordev.lumberjack.Lumberjack;
import net.doubledoordev.lumberjack.util.Constants;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.doubledoordev.lumberjack.util.Constants.makeTable;

/**
 * @author Dries007
 */
public class ItemLumberAxe extends ItemAxe
{
    public static final UUID ATTRIBUTE_MODIFIER_DAMAGE = UUID.fromString("ee56eb2e-71df-43df-b956-e06e4c587599");
    public static final UUID ATTRIBUTE_MODIFIER_SPEED = UUID.fromString("fca3c43b-40f9-4037-b269-56f726268b7c");

    public static List<ItemLumberAxe> lumberAxes = new ArrayList<>();
    private static List<String> toolMaterials = new ArrayList<>();
    private static List<String> textureStrings = new ArrayList<>();
    private static List<String> itemNames = new ArrayList<>();
    private static List<String> craftingItems = new ArrayList<>();
    public final String materialName;

    public ItemLumberAxe(ToolMaterial toolMaterial, ItemStack repairStack)
    {
        super(toolMaterial);

        String name = toolMaterial.name().toLowerCase();

        //Fuck mods that do this: "modid_nameofmaterial"
        if (name.indexOf('_') != -1) name = name.substring(name.indexOf('_') + 1);
        if (name.indexOf('|') != -1) name = name.substring(name.indexOf('|') + 1);
        if (name.indexOf(':') != -1) name = name.substring(name.indexOf(':') + 1);

        this.materialName = name;

        setUnlocalizedName("lumberaxe" + Character.toUpperCase(name.charAt(0)) + name.substring(1));

        toolMaterials.add(toolMaterial.toString());
        itemNames.add(name);

        this.setRegistryName("lumberjack", materialName + "_lumberaxe");
        GameRegistry.register(this);

        String items = "";
        int[] ids = OreDictionary.getOreIDs(repairStack);
        if (ids.length == 0)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(this, "XX", "SX", "SX", 'S', "stickWood", 'X', repairStack).setMirrored(true));
            items = repairStack.getUnlocalizedName();
        }
        else
        {
            for (int id : ids)
            {
                GameRegistry.addRecipe(new ShapedOreRecipe(this, "XX", "SX", "SX", 'S', "stickWood", 'X', OreDictionary.getOreName(id)).setMirrored(true));
                items += OreDictionary.getOreName(id) + ", ";
            }
        }
        craftingItems.add(items);
        lumberAxes.add(this);
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack)
    {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND)
        {
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getAttributeUnlocalizedName(), new AttributeModifier(ATTRIBUTE_MODIFIER_DAMAGE, "Weapon modifier", 0.75, 1));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getAttributeUnlocalizedName(), new AttributeModifier(ATTRIBUTE_MODIFIER_SPEED, "Weapon modifier", -0.75, 1));
        }

        return multimap;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onBlockDestroyed(ItemStack itemStack, World world, IBlockState state, BlockPos blockPos, EntityLivingBase entityLivingBase)
    {
        return Material.LEAVES.equals(state.getMaterial()) || super.onBlockDestroyed(itemStack, world, state, blockPos, entityLivingBase);
    }

    public static void debug()
    {
        Lumberjack.getLogger().info("Table of materials");
        Lumberjack.getLogger().info(makeTable(
                new Constants.TableData("Tool Material", ItemLumberAxe.toolMaterials),
                new Constants.TableData("Texture string", ItemLumberAxe.textureStrings),
                new Constants.TableData("Item name", ItemLumberAxe.itemNames),
                new Constants.TableData("Crafting Items", ItemLumberAxe.craftingItems)
        ));
    }
}
