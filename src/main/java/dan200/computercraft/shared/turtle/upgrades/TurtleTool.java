/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftTags;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.fabric.mixininterface.IMatrix4f;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.tag.Tag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

import static net.minecraft.nbt.NbtElement.COMPOUND_TYPE;
import static net.minecraft.nbt.NbtElement.LIST_TYPE;

public class TurtleTool extends AbstractTurtleUpgrade
{
    protected static final TurtleCommandResult UNBREAKABLE = TurtleCommandResult.failure( "Cannot break unbreakable block" );
    protected static final TurtleCommandResult INEFFECTIVE = TurtleCommandResult.failure( "Cannot break block with this tool" );

    final ItemStack item;
    final float damageMulitiplier;
    @Nullable
    final Tag<Block> breakable;

    public TurtleTool( Identifier id, Item item, float damageMulitiplier, @Nullable Tag<Block> breakable )
    {
        super( id, TurtleUpgradeType.TOOL, new ItemStack( item ) );
        this.item = new ItemStack( item );
        this.damageMulitiplier = damageMulitiplier;
        this.breakable = breakable;
    }

    @Override
    public boolean isItemSuitable( @Nonnull ItemStack stack )
    {
        NbtCompound tag = stack.getNbt();
        if( tag == null || tag.isEmpty() ) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if( stack.isDamaged() || stack.hasEnchantments() || stack.hasCustomName() ) return false;
        if( tag.contains( "AttributeModifiers", LIST_TYPE ) &&
            !tag.getList( "AttributeModifiers", COMPOUND_TYPE ).isEmpty() )
        {
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( getCraftingItem(), side == TurtleSide.LEFT ? Transforms.leftTransform : Transforms.rightTransform );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        switch( verb )
        {
            case ATTACK:
                return attack( turtle, direction );
            case DIG:
                return dig( turtle, direction );
            default:
                return TurtleCommandResult.failure( "Unsupported action" );
        }
    }

    protected TurtleCommandResult checkBlockBreakable( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        if( state.isAir() || block == Blocks.BEDROCK
            || state.calcBlockBreakingDelta( player, world, pos ) <= 0 )
        {
            return UNBREAKABLE;
        }

        return breakable == null || breakable.contains( state.getBlock() ) || isTriviallyBreakable( world, pos, state )
            ? TurtleCommandResult.success() : INEFFECTIVE;
    }

    private TurtleCommandResult attack( ITurtleAccess turtle, Direction direction )
    {
        // Create a fake player, and orient it appropriately
        World world = turtle.getLevel();
        BlockPos position = turtle.getPosition();
        BlockEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( position );
        if( turtleTile == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        final TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, position, direction );

        // See if there is an entity present
        Vec3d turtlePos = turtlePlayer.getPos();
        Vec3d rayDir = turtlePlayer.getRotationVec( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventory
            ItemStack stackCopy = item.copy();
            turtlePlayer.loadInventory( stackCopy );

            Entity hitEntity = hit.getKey();

            // Fire several events to ensure we have permissions.
            if( AttackEntityCallback.EVENT.invoker().interact( turtlePlayer, world, Hand.MAIN_HAND, hitEntity, null ) == ActionResult.FAIL
                || !hitEntity.isAttackable() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtleTile, turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.handleAttack( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttributeValue( EntityAttributes.GENERIC_ATTACK_DAMAGE ) * damageMulitiplier;
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.player( turtlePlayer );
                    if( hitEntity instanceof ArmorStandEntity )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.damage( source, damage );
                        if( hitEntity.isAlive() ) hitEntity.damage( source, damage );
                        attacked = true;
                    }
                    else
                    {
                        if( hitEntity.damage( source, damage ) ) attacked = true;
                    }
                }
            }

            // Stop claiming drops
            stopConsuming( turtleTile, turtle );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.getInventory().clear();
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }

    private TurtleCommandResult dig( ITurtleAccess turtle, Direction direction )
    {
        // TODO: HOE_TILL really, if it's ever implemented
        if( item.getItem() == Items.DIAMOND_SHOVEL || item.getItem() == Items.DIAMOND_HOE )
        {
            if( TurtlePlaceCommand.deployCopiedItem( item.copy(), turtle, direction, null, null ) )
            {
                return TurtleCommandResult.success();
            }
        }

        // Get ready to dig
        World world = turtle.getLevel();
        BlockPos turtlePosition = turtle.getPosition();
        BlockEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( turtlePosition );
        if( turtleTile == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        BlockPos blockPosition = turtlePosition.offset( direction );
        if( world.isAir( blockPosition ) || WorldUtil.isLiquidBlock( world, blockPosition ) )
        {
            return TurtleCommandResult.failure( "Nothing to dig here" );
        }

        BlockState state = world.getBlockState( blockPosition );
        FluidState fluidState = world.getFluidState( blockPosition );

        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, turtlePosition, direction );
        turtlePlayer.loadInventory( item.copy() );

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            if( !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak( world, turtlePlayer, blockPosition, state, null ) )
            {
                return TurtleCommandResult.failure( "Cannot break protected block" );
            }

            if( !TurtlePermissions.isBlockEditable( world, blockPosition, turtlePlayer ) )
            {
                return TurtleCommandResult.failure( "Cannot break protected block" );
            }
        }

        // Check if we can break the block
        TurtleCommandResult breakable = checkBlockBreakable( state, world, blockPosition, turtlePlayer );
        if( !breakable.isSuccess() ) return breakable;

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtleTile, turtle ) );

        BlockEntity tile = world.getBlockEntity( blockPosition );

        // Much of this logic comes from MultiPlayerGameModer#destroyBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.syncWorldEvent( 2001, blockPosition, Block.getRawIdFromState( state ) );

        // Destroy the block
        boolean canHarvest = turtlePlayer.canHarvest( state );
        state.getBlock().onBreak( world, blockPosition, state, turtlePlayer );
        boolean canBreak = world.setBlockState( blockPosition, fluidState.getBlockState(), 11 );
        if( canBreak ) state.getBlock().onBroken( world, blockPosition, state );
        if( canHarvest && canBreak )
        {
            state.getBlock().afterBreak( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandStack() );
        }

        stopConsuming( turtleTile, turtle );

        return TurtleCommandResult.success();

    }

    private static Function<ItemStack, ItemStack> turtleDropConsumer( BlockEntity tile, ITurtleAccess turtle )
    {
        return drop -> tile.isRemoved() ? drop : InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
    }

    private static void stopConsuming( BlockEntity tile, ITurtleAccess turtle )
    {
        Direction direction = tile.isRemoved() ? null : turtle.getDirection().getOpposite();
        DropConsumer.clearAndDrop( turtle.getLevel(), turtle.getPosition(), direction );
    }

    private static class Transforms
    {
        static final AffineTransformation leftTransform = getMatrixFor( -0.40625f );
        static final AffineTransformation rightTransform = getMatrixFor( 0.40625f );

        private static AffineTransformation getMatrixFor( float offset )
        {
            Matrix4f matrix = new Matrix4f();
            ((IMatrix4f) (Object) matrix).setFloatArray( new float[] {
                0.0f, 0.0f, -1.0f, 1.0f + offset,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, -1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
            } );
            return new AffineTransformation( matrix );
        }
    }

    protected boolean isTriviallyBreakable( BlockView reader, BlockPos pos, BlockState state )
    {
        return state.isIn( ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE )
            // Allow breaking any "instabreak" block.
            || state.getHardness( reader, pos ) == 0;
    }
}
