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
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.AlternativeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

class LootTableGenerator extends LootTableProvider
{

    LootTableGenerator( FabricDataOutput output )
    {
        super( output );
    }

    @Override
    protected void registerLoot( BiConsumer<ResourceLocation, LootTable> add )
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

        add.accept( Registry.ModBlocks.CABLE.getLootTable(), LootTable
            .lootTable()
            .setParamSet( LootContextParamSets.BLOCK )
            .withPool( LootPool.lootPool()
                .setRolls( ConstantValue.exactly( 1 ) )
                .add( LootItem.lootTableItem( Registry.ModItems.CABLE ) )
                .when( ExplosionCondition.survivesExplosion() )
                .when( LootItemBlockStatePropertyCondition.hasBlockStateProperties( Registry.ModBlocks.CABLE )
                    .setProperties( StatePropertiesPredicate.Builder.properties().hasProperty( BlockCable.CABLE, true ) )
                )
            )
            .withPool( LootPool.lootPool()
                .setRolls( ConstantValue.exactly( 1 ) )
                .add( LootItem.lootTableItem( Registry.ModItems.WIRED_MODEM ) )
                .when( ExplosionCondition.survivesExplosion() )
                .when( LootItemBlockStatePropertyCondition.hasBlockStateProperties( Registry.ModBlocks.CABLE )
                    .setProperties( StatePropertiesPredicate.Builder.properties().hasProperty( BlockCable.MODEM, CableModemVariant.None ) )
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

    private static void basicDrop( BiConsumer<ResourceLocation, LootTable> add, Block block )
    {
        add.accept( block.getLootTable(), LootTable
            .lootTable()
            .setParamSet( LootContextParamSets.BLOCK )
            .withPool( LootPool.lootPool()
                .setRolls( ConstantValue.exactly( 1 ) )
                .add( LootItem.lootTableItem( block ) )
                .when( ExplosionCondition.survivesExplosion() )
            ).build() );
    }

    private static void computerDrop( BiConsumer<ResourceLocation, LootTable> add, Block block )
    {
        add.accept( block.getLootTable(), LootTable
            .lootTable()
            .setParamSet( LootContextParamSets.BLOCK )
            .withPool( LootPool.lootPool()
                .setRolls( ConstantValue.exactly( 1 ) )
                .add( DynamicLoot.dynamicEntry( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ) )
                .when( AlternativeLootItemCondition.alternative(
                    BlockNamedEntityLootCondition.BUILDER,
                    HasComputerIdLootCondition.BUILDER,
                    PlayerCreativeLootCondition.BUILDER.invert()
                ) )
            ).build() );
    }
}
