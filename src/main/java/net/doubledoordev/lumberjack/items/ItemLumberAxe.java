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

import com.google.common.collect.ImmutableList;
import net.doubledoordev.lumberjack.Lumberjack;
import net.doubledoordev.lumberjack.util.Constants;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dries007
 */
public class ItemLumberAxe extends ItemAxe
{
    private static Field efficiencyOnProperMaterialField = ItemTool.class.getDeclaredFields()[1];
    private static Field damageVsEntityField = ItemTool.class.getDeclaredFields()[2];
    private static Field attackSpeedField = ItemTool.class.getDeclaredFields()[3];
    static
    {
        efficiencyOnProperMaterialField.setAccessible(true);
        damageVsEntityField.setAccessible(true);
        attackSpeedField.setAccessible(true);
    }

    private static List<ItemLumberAxe> lumberAxes = new ArrayList<>();
    private static List<String> toolMaterials = new ArrayList<>();

    public static List<ItemLumberAxe> getLumberAxes()
    {
        return ImmutableList.copyOf(lumberAxes);
    }

    /**
     * @return List of NORMALIZED toolmaterials that SHOULD have a lumberaxe item.
     *  Sometimes, when an error occurs while registering, this will not be the case, thus don't assume!
     */
    public static List<String> getUsedToolMaterials()
    {
        return ImmutableList.copyOf(toolMaterials);
    }

    public static boolean usedMaterial(ToolMaterial m)
    {
        return toolMaterials.contains(normalizeName(m));
    }

    /**
     * Wtf? I think this may be forge's fault...
     * Basically, the Enum is fucked up, so the call will fail when the fallback to getRepairItem() happens.
     */
    @Nullable
    public static ItemStack getRepairStack(ToolMaterial m)
    {
        try
        {
            return m.getRepairItemStack();
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            Lumberjack.getLogger().error("ArrayIndexOutOfBoundsException when registering a lumberaxe. This is a known issue, currently being investigated. {}", m);
        }
        return null;
    }

    /**
     * Because we do not want 7 bronze axes
     */
    public static String normalizeName(ToolMaterial toolMaterial)
    {
        String name = toolMaterial.name().toLowerCase().replaceAll("tools?|materials?|(battle)?(sword|axe|hoe|pick(axe)?|shovel|hammer)", "").replaceAll("[_|:]+", " ").trim();
        if (name.indexOf(' ') != -1) name = name.substring(name.indexOf(' ') + 1);
        return name.replaceAll(" ", "");
    }

    public final String materialName;
    public final boolean fromAxe;

    public ItemLumberAxe(ToolMaterial m, ItemAxe axe) throws IllegalAccessException
    {
        this(m, true);
        Lumberjack.getLogger().info("New LumberAxe {} ({}) From axe {}", toolMaterial, materialName, axe.getRegistryName());
        setProperty(efficiencyOnProperMaterialField, axe);
        setProperty(damageVsEntityField, axe);
        setProperty(attackSpeedField, axe);
    }

    public ItemLumberAxe(ToolMaterial m)
    {
        this(m, false);
        Lumberjack.getLogger().info("New LumberAxe {} ({}) From Material", toolMaterial, materialName);
    }

    /**
     * The extra constructor is required to force the possibility of a null itemstack, so the null check doesn't get optimized out.
     * Required since MCP added the package-info stuff with @MethodsReturnNonnullByDefault
     */
    private ItemLumberAxe(ToolMaterial toolMaterial, boolean fromAxe)
    {
        super(toolMaterial, 1F + 4F + (0.5F + 2F) * toolMaterial.getAttackDamage(), -0.1F -3.5F + 0.05F * toolMaterial.getEfficiency());
        this.fromAxe = fromAxe;
        materialName = normalizeName(toolMaterial);

        setUnlocalizedName("lumberaxe" + Character.toUpperCase(materialName.charAt(0)) + materialName.substring(1));

        toolMaterials.add(materialName);

        ItemStack repairStack = getRepairStack(toolMaterial);
        if (repairStack != ItemStack.EMPTY)
        {
            int[] ids = OreDictionary.getOreIDs(repairStack);
            if (ids.length == 0)
            {
            	IRecipe temp = new ShapedOreRecipe(null, this, "XX", "SX", "SX", 'S', "stickWood", 'X', repairStack).setMirrored(true).setRegistryName(new ResourceLocation(Constants.MODID, this.getToolMaterialName()));
            	if(temp != null)
            		Lumberjack.recipesList.add(temp);
            }
            else
            {
                for (int id : ids)
                {
                	IRecipe temp = new ShapedOreRecipe(null, this, "XX", "SX", "SX", 'S', "stickWood", 'X', OreDictionary.getOreName(id)).setMirrored(true).setRegistryName(new ResourceLocation(Constants.MODID, this.getToolMaterialName()));
                	if(temp != null)
                		Lumberjack.recipesList.add(temp);
                }
            }
        }
        else Lumberjack.getLogger().info("LumberAxe {} without recipe! Ask the mod author of {} for a ToolMaterial repairStack OR use D3Core's materials.json file to set it yourself.", materialName, toolMaterial);

        setRegistryName("lumberjack", materialName + "_lumberaxe");
        Lumberjack.registerItem(this, materialName + "_lumberaxe");

        lumberAxes.add(this);
    }

    private void setProperty(Field field, ItemAxe axe)
    {
        try
        {
            field.set(this, field.get(axe));
        }
        catch (Exception e)
        {
            Lumberjack.getLogger().error("Something went wrong trying to hack in the right damage/speed values of the " + this.toolMaterial + " axe.", e);
        }
    }

    @Override
    public boolean onBlockDestroyed(@Nullable ItemStack itemStack, @Nullable World world, IBlockState state, @Nullable BlockPos blockPos, @Nullable EntityLivingBase entityLivingBase)
    {
        return itemStack != ItemStack.EMPTY && world != null && blockPos != null && entityLivingBase != null &&
                (Material.LEAVES.equals(state.getMaterial()) || super.onBlockDestroyed(itemStack, world, state, blockPos, entityLivingBase));
    }
}
