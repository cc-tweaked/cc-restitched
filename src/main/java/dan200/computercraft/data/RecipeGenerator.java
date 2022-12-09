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
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.function.Consumer;

import static dan200.computercraft.api.ComputerCraftTags.Items.COMPUTER;
import static dan200.computercraft.api.ComputerCraftTags.Items.WIRED_MODEM;

class RecipeGenerator extends FabricRecipeProvider
{

    RecipeGenerator( FabricDataOutput output )
    {
        super( output );
    }

    @Override
    public void buildRecipes( @Nonnull Consumer<FinishedRecipe> add )
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
    private void diskColours( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( Colour colour : Colour.VALUES )
        {
            ShapelessRecipeBuilder
                .shapeless( RecipeCategory.REDSTONE, Registry.ModItems.DISK )
                .requires( ConventionalItemTags.REDSTONE_DUSTS )
                .requires( Items.PAPER )
                .requires( DyeItem.byColor( ofColour( colour ) ) )
                .group( "computercraft:disk" )
                .unlockedBy( "has_drive", inventoryChange( Registry.ModBlocks.DISK_DRIVE ) )
                .save( RecipeWrapper.wrap(
                    ImpostorShapelessRecipe.SERIALIZER, add,
                    x -> x.putInt( IColouredItem.NBT_COLOUR, colour.getHex() )
                ), new ResourceLocation( ComputerCraft.MOD_ID, "disk_" + (colour.ordinal() + 1) ) );
        }
    }

    /**
     * Register a crafting recipe for each turtle upgrade.
     *
     * @param add The callback to add recipes.
     */
    private void turtleUpgrades( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = TurtleItemFactory.create( -1, null, -1, family, null, null, 0, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            TurtleUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = TurtleItemFactory.create( -1, null, -1, family, null, upgrade, -1, null );
                ShapedRecipeBuilder
                    .shaped( RecipeCategory.REDSTONE, result.getItem() )
                    .group( String.format( "%s:turtle_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#T" )
                    .define( 'T', base.getItem() )
                    .define( '#', upgrade.getCraftingItem().getItem() )
                    .unlockedBy( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .save(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "turtle_%s/%s/%s",
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
    private void pocketUpgrades( @Nonnull Consumer<FinishedRecipe> add )
    {
        for( ComputerFamily family : ComputerFamily.values() )
        {
            ItemStack base = PocketComputerItemFactory.create( -1, null, -1, family, null );
            if( base.isEmpty() ) continue;

            String nameId = family.name().toLowerCase( Locale.ROOT );

            PocketUpgrades.getVanillaUpgrades().forEach( upgrade -> {
                ItemStack result = PocketComputerItemFactory.create( -1, null, -1, family, upgrade );
                ShapedRecipeBuilder
                    .shaped( RecipeCategory.REDSTONE, result.getItem() )
                    .group( String.format( "%s:pocket_%s", ComputerCraft.MOD_ID, nameId ) )
                    .pattern( "#" )
                    .pattern( "P" )
                    .define( 'P', base.getItem() )
                    .define( '#', upgrade.getCraftingItem().getItem() )
                    .unlockedBy( "has_items",
                        inventoryChange( base.getItem(), upgrade.getCraftingItem().getItem() ) )
                    .save(
                        RecipeWrapper.wrap( ImpostorRecipe.SERIALIZER, add, result.getTag() ),
                        new ResourceLocation( ComputerCraft.MOD_ID, String.format( "pocket_%s/%s/%s",
                            nameId, upgrade.getUpgradeID().getNamespace(), upgrade.getUpgradeID().getPath()
                        ) )
                    );
            } );
        }
    }

    private void basicRecipes( @Nonnull Consumer<FinishedRecipe> add )
    {
        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModItems.CABLE, 6 )
            .pattern( " # " )
            .pattern( "#R#" )
            .pattern( " # " )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.COMPUTER_NORMAL )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_redstone", inventoryChange( ConventionalItemTags.REDSTONE_DUSTS ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.COMPUTER_ADVANCED )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', ConventionalItemTags.GOLD_INGOTS )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_components", inventoryChange( Items.REDSTONE, Items.GOLD_INGOT ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.COMPUTER_COMMAND )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#G#" )
            .define( '#', ConventionalItemTags.GOLD_INGOTS )
            .define( 'R', Blocks.COMMAND_BLOCK )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_components", inventoryChange( Blocks.COMMAND_BLOCK ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.DISK_DRIVE )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#R#" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.MONITOR_NORMAL )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.MONITOR_ADVANCED, 4 )
            .pattern( "###" )
            .pattern( "#G#" )
            .pattern( "###" )
            .define( '#', ConventionalItemTags.GOLD_INGOTS )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModItems.POCKET_COMPUTER_NORMAL )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'A', Items.GOLDEN_APPLE )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModItems.POCKET_COMPUTER_ADVANCED )
            .pattern( "###" )
            .pattern( "#A#" )
            .pattern( "#G#" )
            .define( '#', ConventionalItemTags.GOLD_INGOTS )
            .define( 'A', Items.GOLDEN_APPLE )
            .define( 'G', ConventionalItemTags.GLASS_PANES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_apple", inventoryChange( Items.GOLDEN_APPLE ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.PRINTER )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "#D#" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .define( 'D', ConventionalItemTags.DYES )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.SPEAKER )
            .pattern( "###" )
            .pattern( "#N#" )
            .pattern( "#R#" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'N', Blocks.NOTE_BLOCK )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModItems.WIRED_MODEM )
            .pattern( "###" )
            .pattern( "#R#" )
            .pattern( "###" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'R', ConventionalItemTags.REDSTONE_DUSTS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_cable", inventoryChange( Registry.ModItems.CABLE ) )
            .save( add );

        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Registry.ModBlocks.WIRED_MODEM_FULL )
            .requires( Registry.ModItems.WIRED_MODEM )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_from" ) );
        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Registry.ModItems.WIRED_MODEM )
            .requires( Registry.ModBlocks.WIRED_MODEM_FULL )
            .unlockedBy( "has_modem", inventoryChange( WIRED_MODEM ) )
            .save( add, new ResourceLocation( ComputerCraft.MOD_ID, "wired_modem_full_to" ) );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.WIRELESS_MODEM_NORMAL )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .define( '#', ExtraConventionalItemTags.STONES )
            .define( 'E', ExtraConventionalItemTags.ENDER_PEARLS )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .save( add );

        ShapedRecipeBuilder
            .shaped( RecipeCategory.REDSTONE, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED )
            .pattern( "###" )
            .pattern( "#E#" )
            .pattern( "###" )
            .define( '#', ConventionalItemTags.GOLD_INGOTS )
            .define( 'E', Items.ENDER_EYE )
            .unlockedBy( "has_computer", inventoryChange( COMPUTER ) )
            .unlockedBy( "has_wireless", inventoryChange( Registry.ModBlocks.WIRELESS_MODEM_NORMAL ) )
            .save( add );

        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Items.PLAYER_HEAD )
            .requires( ExtraConventionalItemTags.SKULLS )
            .requires( Registry.ModItems.MONITOR_NORMAL )
            .unlockedBy( "has_monitor", inventoryChange( Registry.ModItems.MONITOR_NORMAL ) )
            .save(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS_RECIPE, add, playerHead( "Cloudhunter", "6d074736-b1e9-4378-a99b-bd8777821c9c" ) ),
                new ResourceLocation( ComputerCraft.MOD_ID, "skull_cloudy" )
            );

        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Items.PLAYER_HEAD )
            .requires( ExtraConventionalItemTags.SKULLS )
            .requires( Registry.ModItems.COMPUTER_NORMAL )
            .unlockedBy( "has_computer", inventoryChange( Registry.ModItems.COMPUTER_NORMAL ) )
            .save(
                RecipeWrapper.wrap( RecipeSerializer.SHAPELESS_RECIPE, add, playerHead( "dan200", "f3c8d69b-0776-4512-8434-d1b2165909eb" ) ),
                new ResourceLocation( ComputerCraft.MOD_ID, "skull_dan200" )
            );

        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Registry.ModItems.PRINTED_PAGES )
            .requires( Registry.ModItems.PRINTED_PAGE, 2 )
            .requires( Items.STRING )
            .unlockedBy( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER ) )
            .save( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );

        ShapelessRecipeBuilder
            .shapeless( RecipeCategory.REDSTONE, Registry.ModItems.PRINTED_BOOK )
            .requires( Items.LEATHER )
            .requires( Registry.ModItems.PRINTED_PAGE, 1 )
            .requires( Items.STRING )
            .unlockedBy( "has_printer", inventoryChange( Registry.ModBlocks.PRINTER ) )
            .save( RecipeWrapper.wrap( ImpostorShapelessRecipe.SERIALIZER, add ) );
    }

    private static DyeColor ofColour( Colour colour )
    {
        return DyeColor.byId( 15 - colour.ordinal() );
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange( TagKey<Item> stack )
    {
        return InventoryChangeTrigger.TriggerInstance.hasItems( ItemPredicate.Builder.item().of( stack ).build() );
    }

    private static InventoryChangeTrigger.TriggerInstance inventoryChange( ItemLike... stack )
    {
        return InventoryChangeTrigger.TriggerInstance.hasItems( stack );
    }

    private static CompoundTag playerHead( String name, String uuid )
    {
        CompoundTag owner = new CompoundTag();
        owner.putString( "Name", name );
        owner.putString( "Id", uuid );

        CompoundTag tag = new CompoundTag();
        tag.put( "SkullOwner", owner );
        return tag;
    }

    private static void addSpecial( Consumer<FinishedRecipe> add, SimpleCraftingRecipeSerializer<?> special )
    {
        var key = BuiltInRegistries.RECIPE_SERIALIZER.getKey( special );
        SpecialRecipeBuilder.special( special ).save( add, key.toString() );
    }
}
