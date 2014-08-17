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

package net.doubledoordev.lumberjack.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static net.doubledoordev.lumberjack.util.Constants.MODID;

/**
 * @author Dries007
 */
public class ItemLumberAxe extends ItemAxe
{
    private static Field damageVsEntityField = ItemTool.class.getDeclaredFields()[2];
    static
    {
        damageVsEntityField.setAccessible(true);
    }

    public static ArrayList<String> toolMaterials   = new ArrayList<>();
    public static ArrayList<String> textureStrings  = new ArrayList<>();
    public static ArrayList<String> itemNames       = new ArrayList<>();
    public static ArrayList<String> craftingItems   = new ArrayList<>();

    public ItemLumberAxe(ToolMaterial toolMaterial)
    {
        super(toolMaterial);

        String name = toolMaterial.name().toLowerCase();
        if (toolMaterial == ToolMaterial.EMERALD) name = "diamond";

        //Fuck mods that do this: "modid_nameofmaterial"
        if (name.indexOf('_') != -1) name = name.substring(name.indexOf('_') + 1);

        setTextureName(MODID + ":" + name + "_lumberaxe");
        name = "lumberaxe" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        setUnlocalizedName(name);
        GameRegistry.registerItem(this, name);

        toolMaterials.add(toolMaterial.toString());
        textureStrings.add(iconString);
        itemNames.add(name);

        try
        {
            damageVsEntityField.set(this, damageVsEntityField.getFloat(this) + 1);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        String items = "";
        ItemStack craftingItem = new ItemStack(toolMaterial.func_150995_f());
        int[] ids = OreDictionary.getOreIDs(craftingItem);
        if (ids.length == 0)
        {
            GameRegistry.addRecipe(new ShapedOreRecipe(this, "XX", "SX", "SX", 'S', "stickWood", 'X', craftingItem).setMirrored(true));
            items = craftingItem.getUnlocalizedName();
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
    }

    @Override
    public boolean onBlockDestroyed(ItemStack itemStack, World world, Block block, int x, int y, int z, EntityLivingBase entityLivingBase)
    {
        return block.getMaterial() == Material.leaves || super.onBlockDestroyed(itemStack, world, block, x, y, z, entityLivingBase);
    }
}
