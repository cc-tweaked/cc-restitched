/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.blocks;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.blocks.ComputerProxy;
import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.apis.TurtleAPI;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.Collections;

public class TileTurtle extends TileComputerBase implements ITurtleTile, DefaultInventory
{
    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_WIDTH = 4;
    public static final int INVENTORY_HEIGHT = 4;

    enum MoveState
    {
        NOT_MOVED,
        IN_PROGRESS,
        MOVED
    }

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private final DefaultedList<ItemStack> previousInventory = DefaultedList.ofSize( INVENTORY_SIZE, ItemStack.EMPTY );
    private boolean inventoryChanged = false;
    private TurtleBrain brain = new TurtleBrain( this );
    private MoveState moveState = MoveState.NOT_MOVED;

    public TileTurtle( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state, family );
    }

    private boolean hasMoved()
    {
        return moveState == MoveState.MOVED;
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ServerComputer computer = new ServerComputer(
            getWorld(), id, label, instanceID, getFamily(),
            ComputerCraft.turtleTermWidth, ComputerCraft.turtleTermHeight
        );
        computer.setPosition( getPos() );
        computer.addAPI( new TurtleAPI( computer.getAPIEnvironment(), getAccess() ) );
        brain.setupComputer( computer );
        return computer;
    }

    public ComputerProxy createProxy()
    {
        return brain.getProxy();
    }

    @Override
    public void destroy()
    {
        if( !hasMoved() )
        {
            // Stop computer
            super.destroy();

            // Drop contents
            if( !getWorld().isClient )
            {
                int size = size();
                for( int i = 0; i < size; i++ )
                {
                    ItemStack stack = getStack( i );
                    if( !stack.isEmpty() )
                    {
                        WorldUtil.dropItemStack( stack, getWorld(), getPos() );
                    }
                }
            }
        }
        else
        {
            // Just turn off any redstone we had on
            for( Direction dir : DirectionUtil.FACINGS )
            {
                RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
            }
        }
    }

    @Override
    protected void unload()
    {
        if( !hasMoved() )
        {
            super.unload();
        }
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        // Apply dye
        ItemStack currentItem = player.getStackInHand( hand );
        if( !currentItem.isEmpty() )
        {
            if( currentItem.getItem() instanceof DyeItem dyeItem )
            {
                // Dye to change turtle colour
                if( !getWorld().isClient )
                {
                    DyeColor dye = dyeItem.getColor();
                    if( brain.getDyeColour() != dye )
                    {
                        brain.setDyeColour( dye );
                        if( !player.isCreative() )
                        {
                            currentItem.decrement( 1 );
                        }
                    }
                }
                return ActionResult.SUCCESS;
            }
            else if( currentItem.getItem() == Items.WATER_BUCKET && brain.getColour() != -1 )
            {
                // Water to remove turtle colour
                if( !getWorld().isClient )
                {
                    if( brain.getColour() != -1 )
                    {
                        brain.setColour( -1 );
                        if( !player.isCreative() )
                        {
                            player.setStackInHand( hand, new ItemStack( Items.BUCKET ) );
                            player.getInventory().markDirty();
                        }
                    }
                }
                return ActionResult.SUCCESS;
            }
        }

        // Open GUI or whatever
        return super.onActivate( player, hand, hit );
    }

    @Override
    protected boolean canNameWithTag( PlayerEntity player )
    {
        return true;
    }

    @Override
    protected double getInteractRange( PlayerEntity player )
    {
        return 12.0;
    }

    @Override
    protected void serverTick()
    {
        super.serverTick();
        brain.update();
        if( inventoryChanged )
        {
            ServerComputer computer = getServerComputer();
            if( computer != null ) computer.queueEvent( "turtle_inventory" );

            inventoryChanged = false;
            for( int n = 0; n < size(); n++ )
            {
                previousInventory.set( n, getStack( n ).copy() );
            }
        }
    }

    protected void clientTick()
    {
        brain.update();
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED ) super.onNeighbourChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        if( moveState == MoveState.NOT_MOVED ) super.onNeighbourTileEntityChange( neighbour );
    }

    public void notifyMoveStart()
    {
        if( moveState == MoveState.NOT_MOVED ) moveState = MoveState.IN_PROGRESS;
    }

    public void notifyMoveEnd()
    {
        // MoveState.MOVED is final
        if( moveState == MoveState.IN_PROGRESS ) moveState = MoveState.NOT_MOVED;
    }

    @Override
    public void readNbt( @Nonnull NbtCompound nbt )
    {
        super.readNbt( nbt );

        // Read inventory
        NbtList nbttaglist = nbt.getList( "Items", NbtElement.COMPOUND_TYPE );
        inventory.clear();
        previousInventory.clear();
        for( int i = 0; i < nbttaglist.size(); i++ )
        {
            NbtCompound tag = nbttaglist.getCompound( i );
            int slot = tag.getByte( "Slot" ) & 0xff;
            if( slot < size() )
            {
                inventory.set( slot, ItemStack.fromNbt( tag ) );
                previousInventory.set( slot, inventory.get( slot ).copy() );
            }
        }

        // Read state
        brain.readFromNBT( nbt );
        brain.readDescription( nbt );
    }

    //    @Override
    //    public void handleUpdateTag( @Nonnull CompoundTag nbt )
    //    {
    //        super.handleUpdateTag( nbt );
    //        brain.readDescription( nbt );
    //    }

    @Override
    public void writeNbt( @Nonnull NbtCompound nbt )
    {
        // Write inventory
        NbtList nbttaglist = new NbtList();
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                NbtCompound tag = new NbtCompound();
                tag.putByte( "Slot", (byte) i );
                inventory.get( i ).writeNbt( tag );
                nbttaglist.add( tag );
            }
        }
        nbt.put( "Items", nbttaglist );

        // Write brain
        nbt = brain.writeToNBT( nbt );

        super.writeNbt( nbt );
    }

    @Override
    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return hasPeripheralUpgradeOnSide( localSide );
    }

    // IDirectionalTile

    @Override
    public Direction getDirection()
    {
        return getCachedState().get( BlockTurtle.FACING );
    }

    public void setDirection( Direction dir )
    {
        if( dir.getAxis() == Direction.Axis.Y ) dir = Direction.NORTH;
        world.setBlockState( pos, getCachedState().with( BlockTurtle.FACING, dir ) );

        updateOutput();
        updateInputsImmediately();

        onTileEntityChange();
    }

    // ITurtleTile

    @Override
    public ITurtleUpgrade getUpgrade( TurtleSide side )
    {
        return brain.getUpgrade( side );
    }

    @Override
    public int getColour()
    {
        return brain.getColour();
    }

    @Override
    public Identifier getOverlay()
    {
        return brain.getOverlay();
    }

    @Override
    public ITurtleAccess getAccess()
    {
        return brain;
    }

    @Override
    public Vec3d getRenderOffset( float f )
    {
        return brain.getRenderOffset( f );
    }

    @Override
    public float getRenderYaw( float f )
    {
        return brain.getVisualYaw( f );
    }

    @Override
    public float getToolRenderAngle( TurtleSide side, float f )
    {
        return brain.getToolRenderAngle( side, f );
    }

    void setOwningPlayer( GameProfile player )
    {
        brain.setOwningPlayer( player );
        markDirty();
    }

    // IInventory

    @Override
    public int size()
    {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty()
    {
        for( ItemStack stack : inventory )
        {
            if( !stack.isEmpty() ) return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public ItemStack getStack( int slot )
    {
        return slot >= 0 && slot < INVENTORY_SIZE ? inventory.get( slot ) : ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = getStack( slot );
        setStack( slot, ItemStack.EMPTY );
        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        if( count == 0 ) return ItemStack.EMPTY;

        ItemStack stack = getStack( slot );
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        if( stack.getCount() <= count )
        {
            setStack( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        onInventoryDefinitelyChanged();
        return part;
    }

    @Override
    public void setStack( int i, @Nonnull ItemStack stack )
    {
        if ( i >= 0 && i < INVENTORY_SIZE )
        {
            inventory.set( i, stack );
            if ( !InventoryUtil.areItemsEqual( stack, inventory.get( i ) ) )
            {
                onInventoryDefinitelyChanged();
            }
        }
    }

    @Override
    public void clear()
    {
        boolean changed = false;
        for( int i = 0; i < INVENTORY_SIZE; i++ )
        {
            if( !inventory.get( i ).isEmpty() )
            {
                inventory.set( i, ItemStack.EMPTY );
                changed = true;
            }
        }

        if( changed ) onInventoryDefinitelyChanged();
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
        if( !inventoryChanged )
        {
            for( int n = 0; n < size(); n++ )
            {
                if( !ItemStack.areEqual( getStack( n ), previousInventory.get( n ) ) )
                {
                    inventoryChanged = true;
                    break;
                }
            }
        }
    }

    @Override
    public boolean canPlayerUse( @Nonnull PlayerEntity player )
    {
        return isUsable( player, false );
    }

    private void onInventoryDefinitelyChanged()
    {
        super.markDirty();
        inventoryChanged = true;
    }

    public void onTileEntityChange()
    {
        super.markDirty();
    }

    // Networking stuff

    @Nonnull
    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        NbtCompound nbt = super.toInitialChunkDataNbt();
        brain.writeDescription( nbt );
        return nbt;
    }

    @Nonnull
    @Override
    protected String getPeripheralName()
    {
        return "turtle";
    }

    // Privates

    private boolean hasPeripheralUpgradeOnSide( ComputerSide side )
    {
        ITurtleUpgrade upgrade;
        switch( side )
        {
            case RIGHT:
                upgrade = getUpgrade( TurtleSide.RIGHT );
                break;
            case LEFT:
                upgrade = getUpgrade( TurtleSide.LEFT );
                break;
            default:
                return false;
        }
        return upgrade != null && upgrade.getType().isPeripheral();
    }

    public void transferStateFrom( TileTurtle copy )
    {
        super.transferStateFrom( copy );
        Collections.copy( inventory, copy.inventory );
        Collections.copy( previousInventory, copy.previousInventory );
        inventoryChanged = copy.inventoryChanged;
        brain = copy.brain;
        brain.setOwner( this );

        // Mark the other turtle as having moved, and so its peripheral is dead.
        copy.moveState = MoveState.MOVED;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerTurtle( id, inventory, brain );
    }
}
