/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftTags.Blocks;
import dan200.computercraft.shared.Registry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import static dan200.computercraft.api.ComputerCraftTags.Items.*;

class ItemTagsGenerator extends FabricTagProvider.ItemTagProvider
{
    ItemTagsGenerator( FabricDataGenerator generator, BlockTagsGenerator blockTags )
    {
        super( generator, blockTags );
    }

    @Override
    protected void generateTags()
    {
        copy( Blocks.COMPUTER, COMPUTER );
        copy( Blocks.TURTLE, TURTLE );
        tag( WIRED_MODEM ).add( Registry.ModItems.WIRED_MODEM, Registry.ModItems.WIRED_MODEM_FULL );
        copy( Blocks.MONITOR, MONITOR );

        tag( ItemTags.PIGLIN_LOVED ).add(
            Registry.ModItems.COMPUTER_ADVANCED, Registry.ModItems.TURTLE_ADVANCED,
            Registry.ModItems.WIRELESS_MODEM_ADVANCED, Registry.ModItems.POCKET_COMPUTER_ADVANCED,
            Registry.ModItems.MONITOR_ADVANCED
        );

        tag( ExtraConventionalItemTags.ENDER_PEARLS ).add( Items.ENDER_PEARL );
        tag( ExtraConventionalItemTags.GOLD_BLOCKS ).add( Items.GOLD_BLOCK );
        tag( ExtraConventionalItemTags.SKULLS ).add(
            Items.CREEPER_HEAD,
            Items.DRAGON_HEAD,
            Items.PLAYER_HEAD,
            Items.SKELETON_SKULL,
            Items.WITHER_SKELETON_SKULL,
            Items.ZOMBIE_HEAD
        );
        tag( ExtraConventionalItemTags.STONES ).add(
            Items.ANDESITE,
            Items.DIORITE,
            Items.GRANITE,
            Items.INFESTED_STONE,
            Items.STONE,
            Items.POLISHED_ANDESITE,
            Items.POLISHED_DIORITE,
            Items.POLISHED_GRANITE,
            Items.DEEPSLATE,
            Items.POLISHED_DEEPSLATE,
            Items.INFESTED_DEEPSLATE,
            Items.TUFF
        );
        tag( ExtraConventionalItemTags.WOODEN_CHESTS ).add(
            Items.CHEST,
            Items.TRAPPED_CHEST
        );
    }
}
