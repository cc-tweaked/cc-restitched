/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.BlockGeneric;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BlockSpeaker extends BlockGeneric
{
    private static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final BlockEntityTicker<TileSpeaker> serverTicker = ( level, pos, state, drive ) -> drive.serverTick();

    public BlockSpeaker( Settings settings )
    {
        super( settings, () -> Registry.ModBlockEntities.SPEAKER );
        setDefaultState( getStateManager().getDefaultState()
            .with( FACING, Direction.NORTH ) );
    }

    @Override
    protected void appendProperties( StateManager.Builder<Block, BlockState> properties )
    {
        properties.add( FACING );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState().with( FACING, placement.getPlayerFacing().getOpposite() );
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker( @Nonnull World level, @Nonnull BlockState state, @Nonnull BlockEntityType<U> type )
    {
        return level.isClient ? null : BlockWithEntity.checkType( type, Registry.ModBlockEntities.SPEAKER, serverTicker );
    }
}
