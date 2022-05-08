/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.Random;
import java.util.function.Supplier;

public abstract class BlockGeneric extends BlockWithEntity
{
    private final Supplier<? extends BlockEntityType<? extends TileGeneric>> type;

    public BlockGeneric( Settings settings, Supplier<? extends BlockEntityType<? extends TileGeneric>> type )
    {
        super( settings );
        this.type = type;
    }

    @Override
    @Deprecated
    public final void onStateReplaced( @Nonnull BlockState block, @Nonnull World world, @Nonnull BlockPos pos, BlockState replace, boolean bool )
    {
        if( block.getBlock() == replace.getBlock() ) return;

        BlockEntity tile = world.getBlockEntity( pos );
        super.onStateReplaced( block, world, pos, replace, bool );
        world.removeBlockEntity( pos );
        if( tile instanceof TileGeneric generic ) generic.destroy();
    }

    @Nonnull
    @Override
    @Deprecated
    public final ActionResult onUse( @Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand, @Nonnull BlockHitResult hit )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        return tile instanceof TileGeneric generic ? generic.onActivate( player, hand, hit ) : ActionResult.PASS;
    }

    @Override
    @Deprecated
    public final void neighborUpdate( @Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull Block neighbourBlock, @Nonnull BlockPos neighbourPos, boolean isMoving )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile instanceof TileGeneric generic ) generic.onNeighbourChange( neighbourPos );
    }

    @Override
    @Deprecated
    public void scheduledTick( @Nonnull BlockState state, ServerWorld world, @Nonnull BlockPos pos, @Nonnull Random rand )
    {
        BlockEntity te = world.getBlockEntity( pos );
        if( te instanceof TileGeneric generic ) generic.blockTick();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity( @Nonnull BlockPos pos, @Nonnull BlockState state )
    {
        return type.get().instantiate( pos, state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderType( @Nonnull BlockState state )
    {
        return BlockRenderType.MODEL;
    }
}
