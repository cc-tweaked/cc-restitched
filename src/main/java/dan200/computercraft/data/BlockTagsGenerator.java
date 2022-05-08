/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.shared.Registry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import static dan200.computercraft.api.ComputerCraftTags.Blocks.*;

class BlockTagsGenerator extends FabricTagProvider.BlockTagProvider
{
    BlockTagsGenerator( FabricDataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void generateTags()
    {
        // Items
        tag( COMPUTER ).add(
            Registry.ModBlocks.COMPUTER_NORMAL,
            Registry.ModBlocks.COMPUTER_ADVANCED,
            Registry.ModBlocks.COMPUTER_COMMAND
        );
        tag( TURTLE ).add( Registry.ModBlocks.TURTLE_NORMAL, Registry.ModBlocks.TURTLE_ADVANCED );
        tag( WIRED_MODEM ).add( Registry.ModBlocks.CABLE, Registry.ModBlocks.WIRED_MODEM_FULL );
        tag( MONITOR ).add( Registry.ModBlocks.MONITOR_NORMAL, Registry.ModBlocks.MONITOR_ADVANCED );

        tag( TURTLE_ALWAYS_BREAKABLE ).forceAddTag( BlockTags.LEAVES ).add(
            Blocks.BAMBOO, Blocks.BAMBOO_SAPLING // Bamboo isn't instabreak for some odd reason.
        );

        tag( TURTLE_SHOVEL_BREAKABLE ).forceAddTag( BlockTags.MINEABLE_WITH_SHOVEL ).add(
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tag( TURTLE_HOE_BREAKABLE ).forceAddTag( BlockTags.CROPS ).forceAddTag( BlockTags.MINEABLE_WITH_HOE ).add(
            Blocks.CACTUS,
            Blocks.MELON,
            Blocks.PUMPKIN,
            Blocks.CARVED_PUMPKIN,
            Blocks.JACK_O_LANTERN
        );

        tag( TURTLE_SWORD_BREAKABLE ).forceAddTag( BlockTags.WOOL ).add( Blocks.COBWEB );

        // Make all blocks aside from command computer mineable.
        tag( BlockTags.MINEABLE_WITH_PICKAXE ).add(
            Registry.ModBlocks.COMPUTER_NORMAL,
            Registry.ModBlocks.COMPUTER_ADVANCED,
            Registry.ModBlocks.TURTLE_NORMAL,
            Registry.ModBlocks.TURTLE_ADVANCED,
            Registry.ModBlocks.SPEAKER,
            Registry.ModBlocks.DISK_DRIVE,
            Registry.ModBlocks.PRINTER,
            Registry.ModBlocks.MONITOR_NORMAL,
            Registry.ModBlocks.MONITOR_ADVANCED,
            Registry.ModBlocks.WIRELESS_MODEM_NORMAL,
            Registry.ModBlocks.WIRELESS_MODEM_ADVANCED,
            Registry.ModBlocks.WIRED_MODEM_FULL,
            Registry.ModBlocks.CABLE
        );
    }

    @Override
    protected FabricTagProvider<Block>.FabricTagBuilder<Block> tag( @NotNull TagKey<Block> tagKey )
    {
        // Fun mapping weirdness here!
        return super.getOrCreateTagBuilder( tagKey );
    }
}
