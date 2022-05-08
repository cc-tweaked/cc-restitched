/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class RedstoneUtil
{
    private RedstoneUtil()
    {
    }

    /**
     * Gets the redstone input for an adjacent block.
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     * @see AbstractRedstoneGateBlock#getPower(World, BlockPos, BlockState)
     */
    public static int getRedstoneInput( World world, BlockPos pos, Direction side )
    {
        int power = world.getEmittedRedstonePower( pos, side );
        if( power >= 15 ) return power;

        BlockState neighbour = world.getBlockState( pos );
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max( power, neighbour.get( RedstoneWireBlock.POWER ) )
            : power;
    }

    public static void propagateRedstoneOutput( World world, BlockPos pos, Direction side )
    {
        // Propagate ordinary output. See BlockRedstoneDiode.notifyNeighbors
        BlockState block = world.getBlockState( pos );
        BlockPos neighbourPos = pos.offset( side );
        world.updateNeighbor( neighbourPos, block.getBlock(), pos );
        world.updateNeighborsExcept( neighbourPos, block.getBlock(), side.getOpposite() );
    }
}
