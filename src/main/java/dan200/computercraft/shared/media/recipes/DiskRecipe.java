/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.media.recipes;

import dan200.computercraft.shared.media.items.ItemDisk;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ColourTracker;
import dan200.computercraft.shared.util.ColourUtils;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import javax.annotation.Nonnull;

public class DiskRecipe extends SpecialCraftingRecipe
{
    private final Ingredient paper = Ingredient.ofItems( Items.PAPER );
    private final Ingredient redstone = Ingredient.fromTag( ConventionalItemTags.REDSTONE_DUSTS );

    public DiskRecipe( Identifier id )
    {
        super( id );
    }

    @Override
    public boolean matches( @Nonnull CraftingInventory inv, @Nonnull World world )
    {
        boolean paperFound = false;
        boolean redstoneFound = false;

        for( int i = 0; i < inv.size(); i++ )
        {
            ItemStack stack = inv.getStack( i );

            if( !stack.isEmpty() )
            {
                if( paper.test( stack ) )
                {
                    if( paperFound ) return false;
                    paperFound = true;
                }
                else if( redstone.test( stack ) )
                {
                    if( redstoneFound ) return false;
                    redstoneFound = true;
                }
                else if( ColourUtils.getStackColour( stack ) == null )
                {
                    return false;
                }
            }
        }

        return redstoneFound && paperFound;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingInventory inv )
    {
        ColourTracker tracker = new ColourTracker();

        for( int i = 0; i < inv.size(); i++ )
        {
            ItemStack stack = inv.getStack( i );

            if( stack.isEmpty() ) continue;

            if( !paper.test( stack ) && !redstone.test( stack ) )
            {
                DyeColor dye = ColourUtils.getStackColour( stack );
                if( dye != null ) tracker.addColour( dye );
            }
        }

        return ItemDisk.createFromIDAndColour( -1, null, tracker.hasColour() ? tracker.getColour() : Colour.BLUE.getHex() );
    }

    @Override
    public boolean fits( int x, int y )
    {
        return x >= 2 && y >= 2;
    }

    @Nonnull
    @Override
    public ItemStack getOutput()
    {
        return ItemDisk.createFromIDAndColour( -1, null, Colour.BLUE.getHex() );
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final SpecialRecipeSerializer<DiskRecipe> SERIALIZER = new SpecialRecipeSerializer<>( DiskRecipe::new );
}
