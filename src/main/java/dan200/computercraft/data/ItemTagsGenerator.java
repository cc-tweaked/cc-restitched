/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.shared.Registry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

import static dan200.computercraft.api.ComputerCraftTags.Items.*;

class ItemTagsGenerator extends FabricTagProvider.ItemTagProvider
{

    ItemTagsGenerator( FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture )
    {
        super( output, completableFuture );
    }

    @Override
    protected void addTags( HolderLookup.Provider arg )
    {
        copy( Blocks.COMPUTER, COMPUTER );
        copy( Blocks.TURTLE, TURTLE );
        tag( WIRED_MODEM ).add( reverseLookup( Registry.ModItems.WIRED_MODEM ), reverseLookup( Registry.ModItems.WIRED_MODEM_FULL ) );
        copy( Blocks.MONITOR, MONITOR );

        tag( ItemTags.PIGLIN_LOVED ).add(
            reverseLookup( Registry.ModItems.COMPUTER_ADVANCED ), reverseLookup( Registry.ModItems.TURTLE_ADVANCED ),
            reverseLookup( Registry.ModItems.WIRELESS_MODEM_ADVANCED ), reverseLookup( Registry.ModItems.POCKET_COMPUTER_ADVANCED ),
            reverseLookup( Registry.ModItems.MONITOR_ADVANCED )
        );

        tag( ExtraConventionalItemTags.ENDER_PEARLS ).add( reverseLookup( Items.ENDER_PEARL ) );
        tag( ExtraConventionalItemTags.GOLD_BLOCKS ).add( reverseLookup( Items.GOLD_BLOCK ) );
        tag( ExtraConventionalItemTags.SKULLS ).add(
            reverseLookup( Items.CREEPER_HEAD ),
            reverseLookup( Items.DRAGON_HEAD ),
            reverseLookup( Items.PLAYER_HEAD ),
            reverseLookup( Items.SKELETON_SKULL ),
            reverseLookup( Items.WITHER_SKELETON_SKULL ),
            reverseLookup( Items.ZOMBIE_HEAD )
        );
        tag( ExtraConventionalItemTags.STONES ).add(
            reverseLookup( Items.ANDESITE ),
            reverseLookup( Items.DIORITE ),
            reverseLookup( Items.GRANITE ),
            reverseLookup( Items.INFESTED_STONE ),
            reverseLookup( Items.STONE ),
            reverseLookup( Items.POLISHED_ANDESITE ),
            reverseLookup( Items.POLISHED_DIORITE ),
            reverseLookup( Items.POLISHED_GRANITE ),
            reverseLookup( Items.DEEPSLATE ),
            reverseLookup( Items.POLISHED_DEEPSLATE ),
            reverseLookup( Items.INFESTED_DEEPSLATE ),
            reverseLookup( Items.TUFF )
        );
        tag( ExtraConventionalItemTags.WOODEN_CHESTS ).add(
            reverseLookup( Items.CHEST ),
            reverseLookup( Items.TRAPPED_CHEST )
        );
    }
}
