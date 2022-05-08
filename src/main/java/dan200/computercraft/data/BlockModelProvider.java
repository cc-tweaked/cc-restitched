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
import net.minecraft.data.client.ItemModelGenerator;
import net.minecraft.data.client.model.BlockStateModelGenerator;
import net.minecraft.data.client.model.BlockStateVariant;
import net.minecraft.data.client.model.BlockStateVariantMap;
import net.minecraft.data.client.model.BlockStateVariantMap.DoubleProperty;
import net.minecraft.data.client.model.BlockStateVariantMap.TripleProperty;
import net.minecraft.data.client.model.Model;
import net.minecraft.data.client.model.Models;
import net.minecraft.data.client.model.Texture;
import net.minecraft.data.client.model.TextureKey;
import net.minecraft.data.client.model.TexturedModel;
import net.minecraft.data.client.model.VariantSettings;
import net.minecraft.data.client.model.VariantsBlockStateSupplier;
import net.minecraft.data.models.model.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import java.util.Optional;

import static net.minecraft.data.client.model.ModelIds.getItemSubModelId;
import static net.minecraft.data.client.model.Texture.getSubId;

public class BlockModelProvider extends FabricModelProvider
{
    private static final Model MONITOR_BASE = new Model(
        Optional.of( new Identifier( ComputerCraft.MOD_ID, "block/monitor_base" ) ),
        Optional.empty(),
        TextureKey.FRONT, TextureKey.SIDE, TextureKey.TOP, TextureKey.BACK
    );
    private static final Model MODEM = new Model(
        Optional.of( new Identifier( ComputerCraft.MOD_ID, "block/modem" ) ),
        Optional.empty(),
        TextureKey.FRONT, TextureKey.BACK
    );

    public BlockModelProvider( FabricDataGenerator dataGenerator )
    {
        super( dataGenerator );
    }

    @Override
    public void generateBlockStateModels( BlockStateModelGenerator generators )
    {
        registerComputer( generators, Registry.ModBlocks.COMPUTER_NORMAL );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_ADVANCED );
        registerComputer( generators, Registry.ModBlocks.COMPUTER_COMMAND );

        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_NORMAL );
        registerWirelessModem( generators, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED );

        registerWiredModems( generators );

        registerMonitors( generators, Registry.ModBlocks.MONITOR_NORMAL );
        registerMonitors( generators, Registry.ModBlocks.MONITOR_ADVANCED );

        generators.registerNorthDefaultHorizontalRotated( Registry.ModBlocks.SPEAKER, TexturedModel.ORIENTABLE );
        generators.registerParentedItemModel( Registry.ModBlocks.SPEAKER, getBlockModelId( Registry.ModBlocks.SPEAKER ) );
    }

    @Override
    public void generateItemModels( ItemModelGenerator itemModelGenerator )
    {

    }

    private void registerComputer( BlockStateModelGenerator generators, BlockComputer<?> block )
    {
        var generator = BlockStateVariantMap.create( BlockComputer.STATE, BlockComputer.FACING );
        for( ComputerState state : BlockComputer.STATE.getValues() )
        {
            var model = Models.ORIENTABLE.upload(
                getBlockSubModelId( block, "_" + state ),
                new Texture()
                    .put( TextureKey.SIDE, getSubId( block, "_side" ) )
                    .put( TextureKey.FRONT, getSubId( block, "_front" + state.getTexture() ) )
                    .put( TextureKey.TOP, getSubId( block, "_top" ) ),
                generators.modelCollector
            );

            for( Direction facing : BlockComputer.FACING.getValues() )
            {
                generator.register( state, facing, BlockStateVariant.create()
                    .put( VariantSettings.Y, toYAngle( facing ) )
                    .put( VariantSettings.MODEL, model )
                );
            }
        }

        generators.blockStateCollector.accept( VariantsBlockStateSupplier.create( block ).coordinate( generator ) );
        generators.registerParentedItemModel( block, getBlockSubModelId( block, "_blinking" ) );
    }

    private void registerWirelessModem( BlockStateModelGenerator generators, BlockWirelessModem block )
    {
        var generator = BlockStateVariantMap.create( BlockWirelessModem.FACING, BlockWirelessModem.ON );

        for( boolean on : BlockWirelessModem.ON.getValues() )
        {
            var model = modemModel( generators, getBlockSubModelId( block, on ? "_on" : "_off" ), getSubId( block, "_face" + (on ? "_on" : "") ) );

            for( Direction facing : BlockWirelessModem.FACING.getValues() )
            {
                generator.register( facing, on, BlockStateVariant.create()
                    .put( VariantSettings.X, toXAngle( facing ) )
                    .put( VariantSettings.Y, toYAngle( facing ) )
                    .put( VariantSettings.MODEL, model )
                );
            }
        }

        generators.blockStateCollector.accept( VariantsBlockStateSupplier.create( block ).coordinate( generator ) );
        generators.registerParentedItemModel( block, getBlockSubModelId( block, "_off" ) );
    }

    private void registerWiredModems( BlockStateModelGenerator generators )
    {
        BlockWiredModemFull fullBlock = Registry.ModBlocks.WIRED_MODEM_FULL;
        var fullBlockGenerator = BlockStateVariantMap.create( BlockWiredModemFull.MODEM_ON, BlockWiredModemFull.PERIPHERAL_ON );
        for( boolean on : BlockWiredModemFull.MODEM_ON.getValues() )
        {
            for( boolean peripheral : BlockWiredModemFull.PERIPHERAL_ON.getValues() )
            {
                String suffix = (on ? "_on" : "_off") + (peripheral ? "_peripheral" : "");
                Identifier faceTexture = new Identifier(
                    ComputerCraft.MOD_ID,
                    "block/wired_modem_face" + (peripheral ? "_peripheral" : "") + (on ? "_on" : "")
                );
                var fullBlockModel = Models.CUBE_ALL.upload(
                    getBlockSubModelId( fullBlock, suffix ),
                    new Texture().put( TextureKey.ALL, faceTexture ),
                    generators.modelCollector
                );

                fullBlockGenerator.register( on, peripheral, BlockStateVariant.create().put( VariantSettings.MODEL, fullBlockModel ) );

                modemModel( generators, new Identifier( ComputerCraft.MOD_ID, "block/wired_modem" + suffix ), faceTexture );
            }
        }


        generators.blockStateCollector.accept( VariantsBlockStateSupplier.create( fullBlock ).coordinate( fullBlockGenerator ) );
        generators.registerParentedItemModel( fullBlock, getBlockSubModelId( fullBlock, "_off" ) );
        generators.registerParentedItemModel( Registry.ModItems.WIRED_MODEM, new Identifier( ComputerCraft.MOD_ID, "block/wired_modem_off" ) );
    }

    private Identifier modemModel( BlockStateModelGenerator generators, Identifier name, Identifier texture )
    {
        return MODEM.upload(
            name,
            new Texture()
                .put( TextureKey.FRONT, texture )
                .put( TextureKey.BACK, new Identifier( ComputerCraft.MOD_ID, "block/modem_back" ) ),
            generators.modelCollector
        );
    }

    private void registerMonitors( BlockStateModelGenerator generators, BlockMonitor block )
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

        var generator = BlockStateVariantMap.create( BlockMonitor.STATE, BlockMonitor.FACING, BlockMonitor.ORIENTATION );
        for( MonitorEdgeState edge : BlockMonitor.STATE.getValues() )
        {
            String suffix = edge == MonitorEdgeState.NONE ? "" : "_" + edge.asString();
            var model = getBlockSubModelId( block, suffix );

            for( Direction facing : BlockMonitor.FACING.getValues() )
            {
                for( Direction orientation : BlockMonitor.ORIENTATION.getValues() )
                {
                    generator.register( edge, facing, orientation, BlockStateVariant.create()
                        .put( VariantSettings.MODEL, model )
                        .put( VariantSettings.X, toXAngle( orientation ) )
                        .put( VariantSettings.Y, toYAngle( facing ) )
                    );
                }
            }
        }

        generators.blockStateCollector.accept( VariantsBlockStateSupplier.create( block ).coordinate( generator ) );
        generators.registerParentedItemModel( block, monitorModel( generators, block, "_item", 15, 4, 0, 32 ) );
    }

    private Identifier monitorModel( BlockStateModelGenerator generators, BlockMonitor block, String corners, int front, int side, int top, int back )
    {
        return MONITOR_BASE.upload(
            getBlockSubModelId( block, corners ),
            new Texture()
                .put( TextureKey.FRONT, getSubId( block, "_" + front ) )
                .put( TextureKey.SIDE, getSubId( block, "_" + side ) )
                .put( TextureKey.TOP, getSubId( block, "_" + top ) )
                .put( TextureKey.BACK, getSubId( block, "_" + back ) ),
            generators.modelCollector
        );
    }

    private static VariantSettings.Rotation toXAngle( Direction direction )
    {
        switch( direction )
        {
            default:
                return VariantSettings.Rotation.R0;
            case UP:
                return VariantSettings.Rotation.R270;
            case DOWN:
                return VariantSettings.Rotation.R90;
        }
    }

    private static VariantSettings.Rotation toYAngle( Direction direction )
    {
        switch( direction )
        {
            default:
            case NORTH:
                return VariantSettings.Rotation.R0;
            case SOUTH:
                return VariantSettings.Rotation.R180;
            case EAST:
                return VariantSettings.Rotation.R90;
            case WEST:
                return VariantSettings.Rotation.R270;
        }
    }
}
