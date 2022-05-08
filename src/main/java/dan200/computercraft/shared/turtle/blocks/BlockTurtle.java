/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.blocks.BlockComputerBase;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.items.ITurtleItem;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import dan200.computercraft.shared.util.WaterloggableHelpers;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import java.util.function.Supplier;

import static dan200.computercraft.shared.util.WaterloggableHelpers.WATERLOGGED;
import static dan200.computercraft.shared.util.WaterloggableHelpers.getFluidStateForPlacement;

public class BlockTurtle extends BlockComputerBase<TileTurtle> implements Waterloggable
{
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape DEFAULT_SHAPE = VoxelShapes.cuboid(
        0.125, 0.125, 0.125,
        0.875, 0.875, 0.875
    );

    private final BlockEntityTicker<TileTurtle> clientTicker = ( level, pos, state, computer ) -> computer.clientTick();

    public BlockTurtle( Settings settings, ComputerFamily family, Supplier<BlockEntityType<TileTurtle>> type )
    {
        super( settings, family, type );
        setDefaultState( getStateManager().getDefaultState()
            .with( FACING, Direction.NORTH )
            .with( WATERLOGGED, false )
        );
    }

    @Override
    protected void appendProperties( StateManager.Builder<Block, BlockState> builder )
    {
        builder.add( FACING, WATERLOGGED );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockRenderType getRenderType( @Nonnull BlockState state )
    {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getOutlineShape( @Nonnull BlockState state, BlockView world, @Nonnull BlockPos pos, @Nonnull ShapeContext context )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        Vec3d offset = tile instanceof TileTurtle turtle ? turtle.getRenderOffset( 1.0f ) : Vec3d.ZERO;
        return offset.equals( Vec3d.ZERO ) ? DEFAULT_SHAPE : DEFAULT_SHAPE.offset( offset.x, offset.y, offset.z );
    }

    @Nullable
    @Override
    public BlockState getPlacementState( ItemPlacementContext placement )
    {
        return getDefaultState()
            .with( FACING, placement.getPlayerFacing() )
            .with( WATERLOGGED, getFluidStateForPlacement( placement ) );
    }

    @Nonnull
    @Override
    @Deprecated
    public FluidState getFluidState( @Nonnull BlockState state )
    {
        return WaterloggableHelpers.getFluidState( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockState getStateForNeighborUpdate( @Nonnull BlockState state, @Nonnull Direction side, @Nonnull BlockState otherState, @Nonnull WorldAccess world, @Nonnull BlockPos pos, @Nonnull BlockPos otherPos )
    {
        WaterloggableHelpers.updateShape( state, world, pos );
        return state;
    }

    @Override
    public void onPlaced( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity entity, @Nonnull ItemStack stack )
    {
        super.onPlaced( world, pos, state, entity, stack );

        BlockEntity tile = world.getBlockEntity( pos );
        if( !world.isClient && tile instanceof TileTurtle turtle )
        {
            if( entity instanceof PlayerEntity player ) turtle.setOwningPlayer( player.getGameProfile() );

            if( stack.getItem() instanceof ITurtleItem item )
            {
                // Set Upgrades
                for( TurtleSide side : TurtleSide.values() )
                {
                    turtle.getAccess().setUpgrade( side, item.getUpgrade( stack, side ) );
                }

                turtle.getAccess().setFuelLevel( item.getFuelLevel( stack ) );

                // Set colour
                int colour = item.getColour( stack );
                if( colour != -1 ) turtle.getAccess().setColour( colour );

                // Set overlay
                Identifier overlay = item.getOverlay( stack );
                if( overlay != null ) ((TurtleBrain) turtle.getAccess()).setOverlay( overlay );
            }
        }
    }

    @Override
    public float getBlastResistance()
    {
        // TODO Implement below functionality
        return 2000;
    }

    //    @Override
    //    public float getExplosionResistance( BlockState state, BlockGetter world, BlockPos pos, Explosion explosion )
    //    {
    //        Entity exploder = explosion.getExploder();
    //        if( getFamily() == ComputerFamily.ADVANCED || exploder instanceof LivingEntity || exploder instanceof AbstractHurtingProjectile)
    //        {
    //            return 2000;
    //        }
    //
    //        return super.getExplosionResistance( state, world, pos, explosion );
    //    }

    @Nonnull
    @Override
    protected ItemStack getItem( TileComputerBase tile )
    {
        return tile instanceof TileTurtle turtle ? TurtleItemFactory.create( turtle ) : ItemStack.EMPTY;
    }

    @Override
    @Nullable
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker( @Nonnull World level, @Nonnull BlockState state, @Nonnull BlockEntityType<U> type )
    {
        return level.isClient ? BlockWithEntity.checkType( type, this.type.get(), clientTicker ) : super.getTicker( level, state, type );
    }
}
