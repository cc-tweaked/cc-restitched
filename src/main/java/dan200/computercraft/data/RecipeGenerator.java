/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.common.ColourableRecipe;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.media.recipes.DiskRecipe;
import dan200.computercraft.shared.media.recipes.PrintoutRecipe;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.pocket.recipes.PocketComputerUpgradeRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.turtle.recipes.TurtleUpgradeRecipe;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorRecipe;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.ComplexRecipeJsonFactory;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonFactory;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonFactory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.item.*;
import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

class RecipeGenerator extends FabricRecipeProvider
{
    RecipeGenerator( FabricDataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void generateRecipes( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        basicRecipes( add );
        diskColours( add );
        pocketUpgrades( add );
        turtleUpgrades( add );

        addSpecial( add, PrintoutRecipe.SERIALIZER );
        addSpecial( add, DiskRecipe.SERIALIZER );
        addSpecial( add, ColourableRecipe.SERIALIZER );
        addSpecial( add, TurtleUpgradeRecipe.SERIALIZER );
        addSpecial( add, PocketComputerUpgradeRecipe.SERIALIZER );
    }

    /**
     * Register a crafting recipe for a disk of every dye colour.
     *
     * @param add The callback to add recipes.
     */
    private void diskColours( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeJsonFactory
                .create( Registry.ModItems.DISK )
                .input( ConventionalItemTags.REDSTONE_DUSTS )
                .input( Items.PAPER )
                .input( DyeItem.byColor( ofColour( colour ) ) )
                .group( "computercraft:disk" )
                .criterion( "has_drive", inventoryChange( Registry.ModBlocks.DISK_DRIVE ) )
                .offerTo( RecipeWrapper.wrap(
                    ImpostorShapelessRecipe.SERIALIZER, add,
                    x -> x.putInt( IColouredItem.NBT_COLOUR, colour.getHex() )
                ), new Identifier( ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1) ) );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, -1, null );
                ShapedRecipeJsonFactory
                    .create( result.getItem() )
                    .group( String.format( "%s:turtle_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#T" )
                    .input( 'T', base.getItem() )
                    .input( '#', upgrade.getCraftingItem().getItem() )
                    .criterion( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .offerTo(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getNbt() ),
                        new Identifier( ComputerCraft.MOD_ID, String.format( "turtle_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    /**
     * Register a crafting recipe for each pocket upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void pocketUpgrades( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = PocketComputerItemFactory.create( -1, null, -1, family, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            PocketUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = PocketComputerItemFactory.create( -1, null, -1, family, upgrade );
                ShapedRecipeJsonFactory
                    .create( result.getItem() )
                    .group( String.format( "%s:pocket_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#" )
                    .pattern( "P" )
                    .input( 'P', base.getItem() )
                    .input( '#', upgrade.getCraftingItem().getItem() )
                    .criterion( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .offerTo(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getNbt() ),
                        new Identifier( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    private void basicRecipes( @Nonnull Consumer<RecipeJsonProvider> add )
    {
        ShapedRecipeJsonFactory
            .create( Registry.ModItems.CABLE, 6 )
            .pattern( " # " )
            .pattern( "#R#" )
            .pattern( " # " )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_NORMAL )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_redstone", inventoryChange( ConventionalItemTags.REDSTONE_DUSTS ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_ADVANCED )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', ConventionalItemTags.GOLD_INGOTS )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_components", inventoryChange( Items.REDSTONE, Items.GOLD_INGOT ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.COMPUTER_COMMAND )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .input( '#', ConventionalItemTags.GOLD_INGOTS )
            .input( 'R', Blocks.COMMAND_BLOCK )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_components", inventoryChange( Blocks.COMMAND_BLOCK ) )
            .offerTo( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.DISK_DRIVE )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#R#" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.MONITOR_NORMAL )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.MONITOR_ADVANCED, 4 )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .input( '#', ConventionalItemTags.GOLD_INGOTS )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.POCKET_COMPUTER_NORMAL )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'A', Items.GOLDEN_APPLE )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.POCKET_COMPUTER_ADVANCED )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .input( '#', ConventionalItemTags.GOLD_INGOTS )
            .input( 'A', Items.GOLDEN_APPLE )
            .input( 'G', ConventionalItemTags.GLASS_PANES )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.PRINTER )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#D#" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .input( 'D', ConventionalItemTags.DYES )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.SPEAKER )
            .pattern( "###" )
            .pattern( "#N#" )
            .pattern( "#R#" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'N', Blocks.NOTE_BLOCK )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModItems.WIRED_MODEM )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "###" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_cable", inventoryChange( Registry.ModItems.CABLE ) )
            .save( add );

        ShapelessRecipeJsonFactory
            .create( Registry.ModBlocks.WIRED_MODEM_FULL )
            .input( Registry.ModItems.WIRED_MODEM )
            .criterion( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new Identifier( ComputerCraft.MOD_ID, "wired_modem_full_from" ) );
        ShapelessRecipeJsonFactory
            .create( Registry.ModItems.WIRED_MODEM )
            .input( Registry.ModBlocks.WIRED_MODEM_FULL )
            .criterion( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new Identifier( ComputerCraft.MOD_ID, "wired_modem_full_to" ) );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.WIRELESS_MODEM_NORMAL )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .input( '#', ExtraConventionalItemTags.STONES )
            .input( 'E', ExtraConventionalItemTags.ENDER_PEARLS )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeJsonFactory
            .create( Registry.ModBlocks.WIRELESS_MODEM_ADVANCED )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .input( '#', ConventionalItemTags.GOLD_INGOTS )
            .input( 'E', Items.ENDER_EYE )
            .criterion( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_wireless", inventoryChange( Registry.ModBlocks.WIRELESS_MODEM_NORMAL ) )
            .save( add );

        ShapelessRecipeJsonFactory
            .create( Items.PLAYER_HEAD )
            .input( ExtraConventionalItemTags.SKULLS )
            .input( Registry.ModItems.MONITOR_NORMAL )
            .criterion( "has_monitor", inventoryChange( Registry.ModItems.MONITOR_NORMAL ) )
            .offerTo(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS, add, playerHead( "Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c" ) ),
                new Identifier( ComputerCraft.MOD_ID, "skull_cloudy" )
            );

        ShapelessRecipeJsonFactory
            .create( Items.PLAYER_HEAD )
            .input( ExtraConventionalItemTags.SKULLS )
            .input( Registry.ModItems.COMPUTER_NORMAL )
            .criterion( "has_computer", inventoryChange( Registry.ModItems.COMPUTER_NORMAL ) )
            .offerTo(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS, add, playerHead( "dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb" ) ),
                new Identifier( ComputerCraft.MOD_ID, "skull_dan200" )
            );

        ShapelessRecipeJsonFactory
            .create( Registry.ModItems.PRINTED_PAGES )
            .input( Registry.ModItems.PRINTED_PAGE, 2 )
            .input( Items.STRING )
            .criterion( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER ) )
            .offerTo( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );

        ShapelessRecipeJsonFactory
            .create( Registry.ModItems.PRINTED_BOOK )
            .input( Items.LEATHER )
            .input( Registry.ModItems.PRINTED_PAGE, 1 )
            .input( Items.STRING )
            .criterion( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER ) )
            .offerTo( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }

    private static InventoryChangedCriterion.Conditions inventoryChange( TagKey<Item> stack )
    {
        return InventoryChangedCriterion.Conditions.items( ItemPredicate.Builder.create().tag( stack ).build() );
    }

    private static InventoryChangedCriterion.Conditions inventoryChange( ItemConvertible... stack )
    {
        return InventoryChangedCriterion.Conditions.items( stack );
    }

    private static NbtCompound playerHead( String name, String uuid )
    {
        NbtCompound owner = new NbtCompound();
        owner.putString( "Name", name );
        owner.putString( "Id", uuid );

        NbtCompound tag = new NbtCompound();
        tag.put( "SkullOwner", owner );
        return tag;
    }

    private static void addSpecial( Consumer<RecipeJsonProvider> add, SpecialRecipeSerializer<?> special )
    {
        var key = net.minecraft.util.registry.Registry.RECIPE_SERIALIZER.getId( special );
        ComplexRecipeJsonFactory.create( special ).offerTo( add, key.toString() );
    }
}
