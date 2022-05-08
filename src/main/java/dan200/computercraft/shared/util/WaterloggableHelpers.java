/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

/**
 * Helpers for working with waterlogged blocks.
 */
public final class WaterloggableHelpers
{
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private WaterloggableHelpers()
    {
    }

    /**
     * Call from {@link net.minecraft.block.Block#getFluidState(BlockState)}.
     *
     * @param state The current state
     * @return This waterlogged block's current fluid
     */
    public static FluidState getFluidState( BlockState state )
    {
        return state.get( WATERLOGGED ) ? Fluids.WATER.getStill( false ) : Fluids.EMPTY.getDefaultState();
    }

    /**
     * Call from {@link net.minecraft.block.Block#getStateForNeighborUpdate(BlockState, Direction, BlockState, WorldAccess, BlockPos, BlockPos)}.
     *
     * @param state The current state
     * @param world The position of this block
     * @param pos   The world this block exists in
     */
    public static void updateShape( BlockState state, WorldAccess world, BlockPos pos )
    {
        if( state.get( WATERLOGGED ) )
        {
            world.createAndScheduleFluidTick( pos, Fluids.WATER, Fluids.WATER.getTickRate( world ) );
        }
    }

    public static boolean getFluidStateForPlacement( ItemPlacementContext context )
    {
        return context.getWorld().getFluidState( context.getBlockPos() ).getFluid() == Fluids.WATER;
    }
}
