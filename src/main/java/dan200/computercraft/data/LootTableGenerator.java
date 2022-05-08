/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.AlternativeLootCondition;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.DynamicEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.util.Identifier;
import java.util.function.BiConsumer;

class LootTableGenerator extends LootTableProvider
{
    LootTableGenerator( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void registerLoot( BiConsumer<Identifier, LootTable> add )
    {
        basicDrop( add, Registry.ModBlocks.DISK_DRIVE );
        basicDrop( add, Registry.ModBlocks.MONITOR_NORMAL );
        basicDrop( add, Registry.ModBlocks.MONITOR_ADVANCED );
        basicDrop( add, Registry.ModBlocks.PRINTER );
        basicDrop( add, Registry.ModBlocks.SPEAKER );
        basicDrop( add, Registry.ModBlocks.WIRED_MODEM_FULL );
        basicDrop( add, Registry.ModBlocks.WIRELESS_MODEM_NORMAL );
        basicDrop( add, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED );

        computerDrop( add, Registry.ModBlocks.COMPUTER_NORMAL );
        computerDrop( add, Registry.ModBlocks.COMPUTER_ADVANCED );
        computerDrop( add, Registry.ModBlocks.COMPUTER_COMMAND );
        computerDrop( add, Registry.ModBlocks.TURTLE_NORMAL );
        computerDrop( add, Registry.ModBlocks.TURTLE_ADVANCED );

        add.accept( Registry.ModBlocks.CABLE.getLootTableId(), LootTable
            .builder()
            .type( LootContextTypes.BLOCK )
            .pool( LootPool.builder()
                .rolls( ConstantLootNumberProvider.create( 1 ) )
                .with( ItemEntry.builder( Registry.ModItems.CABLE ) )
                .conditionally( SurvivesExplosionLootCondition.builder() )
                .conditionally( BlockStatePropertyLootCondition.builder( Registry.ModBlocks.CABLE )
                    .properties( StatePredicate.Builder.create().exactMatch( BlockCable.CABLE, true ) )
                )
            )
            .pool( LootPool.builder()
                .rolls( ConstantLootNumberProvider.create( 1 ) )
                .with( ItemEntry.builder( Registry.ModItems.WIRED_MODEM ) )
                .conditionally( SurvivesExplosionLootCondition.builder() )
                .conditionally( BlockStatePropertyLootCondition.builder( Registry.ModBlocks.CABLE )
                    .properties( StatePredicate.Builder.create().exactMatch( BlockCable.MODEM, CableModemVariant.None ) )
                    .invert()
                )
            )
            .build() );

        // TODO: LOOT_TREASURE_DISK
        /*add.accept( CommonHooks.LOOT_TREASURE_DISK, LootTable
            .lootTable()
            .setParamSet( LootContextParamSets.ALL_PARAMS )
            .build() );*/
    }

    private static void basicDrop( BiConsumer<Identifier, LootTable> add, Block block )
    {
        add.accept( block.getLootTableId(), LootTable
            .builder()
            .type( LootContextTypes.BLOCK )
            .pool( LootPool.builder()
                .rolls( ConstantLootNumberProvider.create( 1 ) )
                .with( ItemEntry.builder( block ) )
                .conditionally( SurvivesExplosionLootCondition.builder() )
            ).build() );
    }

    private static void computerDrop( BiConsumer<Identifier, LootTable> add, Block block )
    {
        add.accept( block.getLootTableId(), LootTable
            .builder()
            .type( LootContextTypes.BLOCK )
            .pool( LootPool.builder()
                .rolls( ConstantLootNumberProvider.create( 1 ) )
                .with( DynamicEntry.builder( new Identifier( ComputerCraft.MOD_ID, "computer" ) ) )
                .conditionally( AlternativeLootCondition.builder(
                    BlockNamedEntityLootCondition.BUILDER,
                    HasComputerIdLootCondition.BUILDER,
                    PlayerCreativeLootCondition.BUILDER.invert()
                ) )
            ).build() );
    }
}
