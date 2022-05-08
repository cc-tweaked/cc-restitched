/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockWirelessModem;
import dan200.computercraft.shared.peripheral.monitor.BlockMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorEdgeState;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static net.minecraft.data.models.model.ModelLocationUtils.getModelLocation;
import static net.minecraft.data.models.model.TextureMapping.getBlockTexture;

public class BlockModelProvider extends FabricModelProvider
{
    private static final ModelTemplate MONITOR_BASE = new ModelTemplate(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/monitor_base" ) ),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.SIDE, TextureSlot.TOP, TextureSlot.BACK
    );
    private static final ModelTemplate MODEM = new ModelTemplate(
        Optional.of( new ResourceLocation( ComputerCraft.MOD_ID, "block/modem" ) ),
        Optional.empty(),
        TextureSlot.FRONT, TextureSlot.BACK
    );

    public BlockModelProvider( FabricDataGenerator dataGenerator )
    {
        super( dataGenerator );
    }

    @Override
    public void generateBlockStateModels( BlockModelGenerators generators )
    {
        registerComputer( generators, Registry.ModBlocks.COMPUTER_NORMAL );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_ADVANCED );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_COMMAND );

        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_NORMAL );
        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED );

        registerWiredModems( generators );

        registerMonitors( generators, Registry.ModBlocks.MONITOR_NORMAL );
        registerMonitors( generators, Registry.ModBlocks.MONITOR_ADVANCED );

        generators.createHorizontallyRotatedBlock( Registry.ModBlocks.SPEAKER, TexturedModel.ORIENTABLE_ONLY_TOP );
        generators.delegateItemModel( Registry.ModBlocks.SPEAKER, getModelLocation( Registry.ModBlocks.SPEAKER ) );
    }

    @Override
    public void generateItemModels( ItemModelGenerators itemModelGenerator )
    {

    }

    private void registerComputer( BlockModelGenerators generators, BlockComputer<?> block )
    {
        var generator = PropertyDispatch.properties( BlockComputer.STATE, BlockComputer.FACING );
        for( ComputerState state : BlockComputer.STATE.getPossibleValues() )
        {
            var model = ModelTemplates.CUBE_ORIENTABLE.create(
                getModelLocation( block, "_" + state ),
                new TextureMapping()
                    .put( TextureSlot.SIDE, getBlockTexture( block, "_side" ) )
                    .put( TextureSlot.FRONT, getBlockTexture( block, "_front" + state.getTexture() ) )
                    .put( TextureSlot.TOP, getBlockTexture( block, "_top" ) ),
                generators.modelOutput
            );

            for( Direction facing : BlockComputer.FACING.getPossibleValues() )
            {
                generator.select( state, facing, Variant.variant()
                    .with( VariantProperties.Y_ROT, toYAngle( facing ) )
                    .with( VariantProperties.MODEL, model )
                );
            }
        }

        generators.blockStateOutput.accept( MultiVariantGenerator.multiVariant( block ).with( generator ) );
        generators.delegateItemModel( block, getModelLocation( block, "_blinking" ) );
    }

    private void registerWirelessModem( BlockModelGenerators generators, BlockWirelessModem block )
    {
        var generator = PropertyDispatch.properties( BlockWirelessModem.FACING, BlockWirelessModem.ON );

        for( boolean on : BlockWirelessModem.ON.getPossibleValues() )
        {
            var model = modemModel( generators, getModelLocation( block, on ? "_on" : "_off" ), getBlockTexture( block, "_face" + (on ? "_on" : "") ) );

            for( Direction facing : BlockWirelessModem.FACING.getPossibleValues() )
            {
                generator.select( facing, on, Variant.variant()
                    .with( VariantProperties.X_ROT, toXAngle( facing ) )
                    .with( VariantProperties.Y_ROT, toYAngle( facing ) )
                    .with( VariantProperties.MODEL, model )
                );
            }
        }

        generators.blockStateOutput.accept( MultiVariantGenerator.multiVariant( block ).with( generator ) );
        generators.delegateItemModel( block, getModelLocation( block, "_off" ) );
    }

    private void registerWiredModems( BlockModelGenerators generators )
    {
        BlockWiredModemFull fullBlock = Registry.ModBlocks.WIRED_MODEM_FULL;
        var fullBlockGenerator = PropertyDispatch.properties( BlockWiredModemFull.MODEM_ON, BlockWiredModemFull.PERIPHERAL_ON );
        for( boolean on : BlockWiredModemFull.MODEM_ON.getPossibleValues() )
        {
            for( boolean peripheral : BlockWiredModemFull.PERIPHERAL_ON.getPossibleValues() )
            {
                String suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                ResourceLocation faceTexture = new ResourceLocation(
                    ComputerCraft.MOD_ID,
                    "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : "")
                );
                var fullBlockModel = ModelTemplates.CUBE_ALL.create(
                    getModelLocation( fullBlock, suffix ),
                    new TextureMapping().put( TextureSlot.ALL, faceTexture ),
                    generators.modelOutput
                );

                fullBlockGenerator.select( on, peripheral, Variant.variant().with( VariantProperties.MODEL, fullBlockModel ) );

                modemModel( generators, new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem" + suffix ), faceTexture );
            }
        }


        generators.blockStateOutput.accept( MultiVariantGenerator.multiVariant( fullBlock ).with( fullBlockGenerator ) );
        generators.delegateItemModel( fullBlock, getModelLocation( fullBlock, "_off" ) );
        generators.delegateItemModel( Registry.ModItems.WIRED_MODEM, new ResourceLocation( ComputerCraft.MOD_ID, "block/wired_modem_off" ) );
    }

    private ResourceLocation modemModel( BlockModelGenerators generators, ResourceLocation name, ResourceLocation texture )
    {
        return MODEM.create(
            name,
            new TextureMapping()
                .put( TextureSlot.FRONT, texture )
                .put( TextureSlot.BACK, new ResourceLocation( ComputerCraft.MOD_ID, "block/modem_back" ) ),
            generators.modelOutput
        );
    }

    private void registerMonitors( BlockModelGenerators generators, BlockMonitor block )
    {
        monitorModel( generators, block, "", 16, 4, 0, 32 );
        monitorModel( generators, block, "_d", 20, 7, 0, 36 );
        monitorModel( generators, block, "_l", 19, 4, 1, 33 );
        monitorModel( generators, block, "_ld", 31, 7, 1, 45 );
        monitorModel( generators, block, "_lr", 18, 4, 2, 34 );
        monitorModel( generators, block, "_lrd", 30, 7, 2, 46 );
        monitorModel( generators, block, "_lru", 24, 5, 2, 40 );
        monitorModel( generators, block, "_lrud", 27, 6, 2, 43 );
        monitorModel( generators, block, "_lu", 25, 5, 1, 39 );
        monitorModel( generators, block, "_lud", 28, 6, 1, 42 );
        monitorModel( generators, block, "_r", 17, 4, 3, 35 );
        monitorModel( generators, block, "_rd", 29, 7, 3, 47 );
        monitorModel( generators, block, "_ru", 23, 5, 3, 41 );
        monitorModel( generators, block, "_rud", 26, 6, 3, 44 );
        monitorModel( generators, block, "_u", 22, 5, 0, 38 );
        monitorModel( generators, block, "_ud", 21, 6, 0, 37 );

        var generator = PropertyDispatch.properties( BlockMonitor.STATE, BlockMonitor.FACING, BlockMonitor.ORIENTATION );
        for( MonitorEdgeState edge : BlockMonitor.STATE.getPossibleValues() )
        {
            String suffix = edge == MonitorEdgeState.NONE ? "" : "_" + edge.getSerializedName();
            var model = getModelLocation( block, suffix );

            for( Direction facing : BlockMonitor.FACING.getPossibleValues() )
            {
                for( Direction orientation : BlockMonitor.ORIENTATION.getPossibleValues() )
                {
                    generator.select( edge, facing, orientation, Variant.variant()
                        .with( VariantProperties.MODEL, model )
                        .with( VariantProperties.X_ROT, toXAngle( orientation ) )
                        .with( VariantProperties.Y_ROT, toYAngle( facing ) )
                    );
                }
            }
        }

        generators.blockStateOutput.accept( MultiVariantGenerator.multiVariant( block ).with( generator ) );
        generators.delegateItemModel( block, monitorModel( generators, block, "_item", 15, 4, 0, 32 ) );
    }

    private ResourceLocation monitorModel( BlockModelGenerators generators, BlockMonitor block, String corners, int front, int side, int top, int back )
    {
        return MONITOR_BASE.create(
            getModelLocation( block, corners ),
            new TextureMapping()
                .put( TextureSlot.FRONT, getBlockTexture( block, "_" + front ) )
                .put( TextureSlot.SIDE, getBlockTexture( block, "_" + side ) )
                .put( TextureSlot.TOP, getBlockTexture( block, "_" + top ) )
                .put( TextureSlot.BACK, getBlockTexture( block, "_" + back ) ),
            generators.modelOutput
        );
    }

    private static VariantProperties.Rotation toXAngle( Direction direction )
    {
        switch( direction )
        {
            default:
                return VariantProperties.Rotation.R0;
            case UP:
                return VariantProperties.Rotation.R270;
            case DOWN:
                return VariantProperties.Rotation.R90;
        }
    }

    private static VariantProperties.Rotation toYAngle( Direction direction )
    {
        switch( direction )
        {
            default:
            case NORTH:
                return VariantProperties.Rotation.R0;
            case SOUTH:
                return VariantProperties.Rotation.R180;
            case EAST:
                return VariantProperties.Rotation.R90;
            case WEST:
                return VariantProperties.Rotation.R270;
        }
    }
}
