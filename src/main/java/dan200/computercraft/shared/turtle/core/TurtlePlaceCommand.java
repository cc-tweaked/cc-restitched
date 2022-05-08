/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.GlassBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.LilyPadItem;
import net.minecraft.item.SignItem;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.item.*;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class TurtlePlaceCommand implements ITurtleCommand
{
    private final InteractDirection direction;
    private final Object[] extraArguments;

    public TurtlePlaceCommand( InteractDirection direction, Object[] arguments )
    {
        this.direction = direction;
        extraArguments = arguments;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get thing to place
        ItemStack stack = turtle.getInventory().getStack( turtle.getSelectedSlot() );
        if( stack.isEmpty() ) return TurtleCommandResult.failure( "No items to place" );

        // Remember old block
        Direction direction = this.direction.toWorldDir( turtle );
        BlockPos coordinates = turtle.getPosition().offset( direction );

        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, playerPosition, direction );

        // Do the deploying
        turtlePlayer.loadInventory( turtle );
        ErrorMessage message = new ErrorMessage();
        boolean result = deploy( stack, turtle, turtlePlayer, direction, extraArguments, message );
        turtlePlayer.unloadInventory( turtle );
        if( result )
        {
            // Animate and return success
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }
        else if( message.message != null )
        {
            return TurtleCommandResult.failure( message.message );
        }
        else
        {
            return TurtleCommandResult.failure( stack.getItem() instanceof BlockItem ? "Cannot place block here" : "Cannot place item here" );
        }
    }

    public static boolean deployCopiedItem( @Nonnull ItemStack stack, ITurtleAccess turtle, Direction direction, Object[] extraArguments, ErrorMessage outErrorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, playerPosition, direction );
        turtlePlayer.loadInventory( stack );
        boolean result = deploy( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
        turtlePlayer.getInventory().clear();
        return result;
    }

    private static boolean deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, ErrorMessage outErrorMessage )
    {
        // Deploy on an entity
        if( deployOnEntity( stack, turtle, turtlePlayer ) ) return true;

        BlockPos position = turtle.getPosition();
        BlockPos newPosition = position.offset( direction );

        // Try to deploy against a block. Tries the following options:
        //     Deploy on the block immediately in front
        return deployOnBlock( stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, outErrorMessage )
            // Deploy on the block one block away
            || deployOnBlock( stack, turtle, turtlePlayer, newPosition.offset( direction ), direction.getOpposite(), extraArguments, false, outErrorMessage )
            // Deploy down on the block in front
            || (direction.getAxis() != Direction.Axis.Y && deployOnBlock( stack, turtle, turtlePlayer, newPosition.down(), Direction.UP, extraArguments, false, outErrorMessage ))
            // Deploy back onto the turtle
            || deployOnBlock( stack, turtle, turtlePlayer, position, direction, extraArguments, false, outErrorMessage );
    }

    private static boolean deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer )
    {
        // See if there is an entity present
        final World world = turtle.getLevel();
        final BlockPos position = turtle.getPosition();
        Vec3d turtlePos = turtlePlayer.getPos();
        Vec3d rayDir = turtlePlayer.getRotationVec( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit == null ) return false;

        // Start claiming entity drops
        Entity hitEntity = hit.getKey();
        Vec3d hitPos = hit.getValue();

        ItemStorage itemHandler = ItemStorage.wrap( turtlePlayer.getInventory() );
        DropConsumer.set( hitEntity, drop -> InventoryUtil.storeItems( drop, itemHandler, 1 ) );

        boolean placed = doDeployOnEntity( stack, turtlePlayer, hitEntity, hitPos );

        DropConsumer.clearAndDrop( world, position, turtle.getDirection().getOpposite() );
        return placed;
    }

    /**
     * Place a block onto an entity. For instance, feeding cows.
     *
     * @param stack        The stack we're placing.
     * @param turtlePlayer The player of the turtle we're placing.
     * @param hitEntity    The entity we're interacting with.
     * @param hitPos       The position our ray trace hit the entity.
     * @return If this item was deployed.
     * @see net.minecraft.server.network.ServerPlayNetworkHandler#handleInteract(ServerboundInteractPacket)
     * @see net.minecraft.entity.player.PlayerEntity#interact(Entity, Hand)
     */
    private static boolean doDeployOnEntity( @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, @Nonnull Entity hitEntity, @Nonnull Vec3d hitPos )
    {
        // Placing "onto" a block follows two flows. First we try to interactAt. If that doesn't succeed, then we try to
        // call the normal interact path. Cancelling an interactAt *does not* cancel a normal interact path.

        ActionResult interactAt = hitEntity.interactAt( turtlePlayer, hitPos, Hand.MAIN_HAND );
        if( interactAt.isAccepted() ) return true;

        if( hitEntity.interact( turtlePlayer, Hand.MAIN_HAND ).isAccepted() ) return true;
        if( hitEntity instanceof LivingEntity hitLiving )
        {
            return stack.useOnEntity( turtlePlayer, hitLiving, Hand.MAIN_HAND ).isAccepted();
        }

        return false;
    }

    private static boolean canDeployOnBlock(
        @Nonnull ItemPlacementContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position,
        Direction side, boolean allowReplaceable, ErrorMessage outErrorMessage
    )
    {
        World world = turtle.getLevel();
        if( !world.isInBuildLimit( position ) || world.isAir( position ) ||
            (context.getStack().getItem() instanceof BlockItem && WorldUtil.isLiquidBlock( world, position )) )
        {
            return false;
        }

        BlockState state = world.getBlockState( position );

        boolean replaceable = state.canReplace( context );
        if( !allowReplaceable && replaceable ) return false;

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            boolean editable = replaceable
                ? TurtlePermissions.isBlockEditable( world, position, player )
                : TurtlePermissions.isBlockEditable( world, position.offset( side ), player );
            if( !editable )
            {
                if( outErrorMessage != null ) outErrorMessage.message = "Cannot place in protected area";
                return false;
            }
        }

        return true;
    }

    private static boolean deployOnBlock(
        @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side,
        Object[] extraArguments, boolean allowReplace, ErrorMessage outErrorMessage
    )
    {
        // Re-orient the fake player
        Direction playerDir = side.getOpposite();
        BlockPos playerPosition = position.offset( side );
        turtlePlayer.setPosition( turtle, playerPosition, playerDir );

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getOffsetX() * 0.5f;
        float hitY = 0.5f + side.getOffsetY() * 0.5f;
        float hitZ = 0.5f + side.getOffsetZ() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f ) hitY = 0.45f;

        // Check if there's something suitable to place onto
        BlockHitResult hit = new BlockHitResult( new Vec3d( hitX, hitY, hitZ ), side, position, false );
        ItemUsageContext context = new ItemUsageContext( turtlePlayer, Hand.MAIN_HAND, hit );
        if( !canDeployOnBlock( new ItemPlacementContext( context ), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return false;
        }

        Item item = stack.getItem();
        BlockEntity existingTile = turtle.getLevel().getBlockEntity( position );

        boolean placed = doDeployOnBlock( stack, turtlePlayer, position, context, hit ).isAccepted();

        // Set text on signs
        if( placed && item instanceof SignItem && extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String message )
        {
            World world = turtle.getLevel();
            BlockEntity tile = world.getBlockEntity( position );
            if( tile == null || tile == existingTile )
            {
                tile = world.getBlockEntity( position.offset( side ) );
            }

            if( tile instanceof SignBlockEntity ) setSignText( world, tile, message );
        }

        return placed;
    }

    /**
     * Attempt to place an item into the world. Returns true/false if an item was placed.
     *
     * @param stack        The stack the player is using.
     * @param turtlePlayer The player which represents the turtle
     * @param position     The block we're deploying against's position.
     * @param context      The context of this place action.
     * @param hit          Where the block we're placing against was clicked.
     * @return If this item was deployed.
     * @see net.minecraft.server.network.ServerPlayerInteractionManager#interactBlock  For the original implementation.
     */
    private static ActionResult doDeployOnBlock(
        @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, BlockPos position, ItemUsageContext context, BlockHitResult hit
    )
    {
        ActionResult useResult = stack.useOnBlock( context );
        if( useResult != ActionResult.PASS ) return useResult;

        Item item = stack.getItem();
        if( item instanceof BucketItem || item instanceof BoatItem || item instanceof LilyPadItem || item instanceof GlassBottleItem )
        {
            TypedActionResult<ItemStack> result = stack.use( context.getWorld(), turtlePlayer, Hand.MAIN_HAND );
            if( result.getResult().isAccepted() && !ItemStack.areEqual( stack, result.getValue() ) )
            {
                turtlePlayer.setStackInHand( Hand.MAIN_HAND, result.getValue() );
                return result.getResult();
            }
        }

        return ActionResult.PASS;
    }

    private static void setSignText( World world, BlockEntity tile, String message )
    {
        SignBlockEntity signTile = (SignBlockEntity) tile;
        String[] split = message.split( "\n" );
        int firstLine = split.length <= 2 ? 1 : 0;
        for( int i = 0; i < 4; i++ )
        {
            if( i >= firstLine && i < firstLine + split.length )
            {
                String line = split[i - firstLine];
                signTile.setTextOnRow( i, line.length() > 15
                    ? new LiteralText( line.substring( 0, 15 ) )
                    : new LiteralText( line )
                );
            }
            else
            {
                signTile.setTextOnRow( i, new LiteralText( "" ) );
            }
        }
        signTile.markDirty();
        world.updateListeners( tile.getPos(), tile.getCachedState(), tile.getCachedState(), Block.NOTIFY_ALL );
    }

    private static class ErrorMessage
    {
        String message;
    }
}
