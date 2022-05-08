/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.Peripherals;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import joptsimple.internal.Strings;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class TileComputerBase extends TileGeneric implements IComputerTile, IPeripheralTile, Nameable, ExtendedScreenHandlerFactory
{
    private static final String NBT_ID = "ComputerId";
    private static final String NBT_LABEL = "Label";
    private static final String NBT_ON = "On";

    private int instanceID = -1;
    private int computerID = -1;
    protected String label = null;
    private boolean on = false;
    boolean startOn = false;
    private boolean fresh = false;

    private int invalidSides = 0;

    private final ComputerFamily family;

    private ComputerProxy proxy;

    public TileComputerBase( BlockEntityType<? extends TileGeneric> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state );
        this.family = family;
    }

    protected void unload()
    {
        if( instanceID >= 0 )
        {
            if( !getWorld().isClient ) ComputerCraft.serverComputerRegistry.remove( instanceID );
            instanceID = -1;
        }
    }

    @Override
    public void destroy()
    {
        unload();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        unload();
    }

    @Override
    public void markRemoved()
    {
        unload();
        super.markRemoved();
    }

    protected boolean canNameWithTag( PlayerEntity player )
    {
        return false;
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        ItemStack currentItem = player.getStackInHand( hand );
        if( !currentItem.isEmpty() && currentItem.getItem() == Items.NAME_TAG && canNameWithTag( player ) && currentItem.hasCustomName() )
        {
            // Label to rename computer
            if( !getWorld().isClient )
            {
                setLabel( currentItem.getName().getString() );
                currentItem.decrement( 1 );
            }
            return ActionResult.SUCCESS;
        }
        else if( !player.isInSneakingPose() )
        {
            // Regular right click to activate computer
            if( !getWorld().isClient && isUsable( player, false ) )
            {
                createServerComputer().turnOn();
                createServerComputer().sendTerminalState( player );
                new ComputerContainerData( createServerComputer() ).open( player, this );
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        updateInputAt( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        updateInputAt( neighbour );
    }

    protected void serverTick()
    {
        ServerComputer computer = createServerComputer();

        if( invalidSides != 0 )
        {
            for( Direction direction : DirectionUtil.FACINGS )
            {
                if( (invalidSides & (1 << direction.ordinal())) != 0 ) refreshPeripheral( computer, direction );
            }
        }

        // If the computer isn't on and should be, then turn it on
        if( startOn || (fresh && on) )
        {
            computer.turnOn();
            startOn = false;
        }

        computer.keepAlive();

        fresh = false;
        computerID = computer.getID();
        label = computer.getLabel();
        on = computer.isOn();

        // Update the block state if needed. We don't fire a block update intentionally,
        // as this only really is needed on the client side.
        updateBlockState( computer.getState() );

        // TODO: This should ideally be split up into label/id/on (which should save NBT and sync to client) and
        //  redstone (which should update outputs)
        if( computer.hasOutputChanged() ) updateOutput();
    }

    protected abstract void updateBlockState( ComputerState newState );

    @Override
    public void writeNbt( @Nonnull NbtCompound nbt )
    {
        // Save ID, label and power state
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        if( label != null ) nbt.putString( NBT_LABEL, label );
        nbt.putBoolean( NBT_ON, on );

        super.writeNbt( nbt );
    }

    @Override
    public void readNbt( @Nonnull NbtCompound nbt )
    {
        super.readNbt( nbt );

        // Load ID, label and power state
        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
        on = startOn = nbt.getBoolean( NBT_ON );
    }

    //    @Override
    //    public void handleUpdateTag( @Nonnull CompoundTag nbt )
    //    {
    //        label = nbt.contains( NBT_LABEL ) ? nbt.getString( NBT_LABEL ) : null;
    //        computerID = nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    //    }

    protected boolean isPeripheralBlockedOnSide( ComputerSide localSide )
    {
        return false;
    }

    protected abstract Direction getDirection();

    protected ComputerSide remapToLocalSide( Direction globalSide )
    {
        return remapLocalSide( DirectionUtil.toLocal( getDirection(), globalSide ) );
    }

    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        return localSide;
    }

    private void updateRedstoneInput( @Nonnull ServerComputer computer, Direction dir, BlockPos targetPos )
    {
        Direction offsetSide = dir.getOpposite();
        ComputerSide localDir = remapToLocalSide( dir );

        computer.setRedstoneInput( localDir, RedstoneUtil.getRedstoneInput( world, targetPos, dir ) );
        computer.setBundledRedstoneInput( localDir, BundledRedstone.getOutput( getWorld(), targetPos, offsetSide ) );
    }

    private void refreshPeripheral( @Nonnull ServerComputer computer, Direction dir )
    {
        invalidSides &= ~(1 << dir.ordinal());

        ComputerSide localDir = remapToLocalSide( dir );
        if( isPeripheralBlockedOnSide( localDir ) ) return;

        Direction offsetSide = dir.getOpposite();
        IPeripheral peripheral = Peripherals.getPeripheral( getWorld(), getPos().offset( dir ), offsetSide );
        computer.setPeripheral( localDir, peripheral );
    }

    public void updateInputsImmediately()
    {
        ServerComputer computer = getServerComputer();
        if( computer != null ) updateInputsImmediately( computer );
    }

    /**
     * Update all redstone and peripherals.
     *
     * This should only be really be called when the computer is being ticked (though there are some cases where it
     * won't be), as peripheral scanning requires adjacent tiles to be in a "correct" state - which may not be the case
     * if they're still updating!
     *
     * @param computer The current computer instance.
     */
    private void updateInputsImmediately( @Nonnull ServerComputer computer )
    {
        BlockPos pos = getPos();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            updateRedstoneInput( computer, dir, pos.offset( dir ) );
            refreshPeripheral( computer, dir );
        }
    }

    private void updateInputAt( @Nonnull BlockPos neighbour )
    {
        ServerComputer computer = getServerComputer();
        if( computer == null ) return;

        for( Direction dir : DirectionUtil.FACINGS )
        {
            BlockPos offset = getPos().offset( dir );
            if( offset.equals( neighbour ) )
            {
                updateRedstoneInput( computer, dir, offset );
                invalidSides |= 1 << dir.ordinal();
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs. This is pretty terrible, but some redstone mods
        // handle this incorrectly.
        BlockPos pos = getPos();
        for( Direction dir : DirectionUtil.FACINGS ) updateRedstoneInput( computer, dir, pos.offset( dir ) );
        invalidSides = (1 << 6) - 1; // Mark all peripherals as dirty.
    }

    /**
     * Update the block's state and propagate redstone output.
     */
    public void updateOutput()
    {
        updateBlock();
        for( Direction dir : DirectionUtil.FACINGS )
        {
            RedstoneUtil.propagateRedstoneOutput( getWorld(), getPos(), dir );
        }
    }

    protected abstract ServerComputer createComputer( int instanceID, int id );

    @Override
    public final int getComputerID()
    {
        return computerID;
    }

    @Override
    public final String getLabel()
    {
        return label;
    }

    @Override
    public final void setComputerID( int id )
    {
        if( getWorld().isClient || computerID == id ) return;

        computerID = id;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setID( computerID );
        markDirty();
    }

    @Override
    public final void setLabel( String label )
    {
        if( getWorld().isClient || Objects.equals( this.label, label ) ) return;

        this.label = label;
        ServerComputer computer = getServerComputer();
        if( computer != null ) computer.setLabel( label );
        markDirty();
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Nonnull
    public ServerComputer createServerComputer()
    {
        if( getWorld().isClient ) throw new IllegalStateException( "Cannot access server computer on the client." );

        boolean changed = false;
        if( instanceID < 0 )
        {
            instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
            changed = true;
        }

        ServerComputer computer = ComputerCraft.serverComputerRegistry.get( instanceID );
        if( computer == null )
        {
            computer = createComputer( instanceID, computerID );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            fresh = true;
            changed = true;
        }

        if( changed ) updateInputsImmediately( computer );
        return computer;
    }

    @Nullable
    public ServerComputer getServerComputer()
    {
        return getWorld().isClient ? null : ComputerCraft.serverComputerRegistry.get( instanceID );
    }

    // Networking stuff

    @Nonnull
    @Override
    public final BlockEntityUpdateS2CPacket toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create( this );
    }

    @Nonnull
    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        // We need this for pick block on the client side.
        NbtCompound nbt = super.toInitialChunkDataNbt();
        if( label != null ) nbt.putString( NBT_LABEL, label );
        if( computerID >= 0 ) nbt.putInt( NBT_ID, computerID );
        return nbt;
    }

    protected void transferStateFrom( TileComputerBase copy )
    {
        if( copy.computerID != computerID || copy.instanceID != instanceID )
        {
            unload();
            instanceID = copy.instanceID;
            computerID = copy.computerID;
            label = copy.label;
            on = copy.on;
            startOn = copy.startOn;
            updateBlock();
        }
        copy.instanceID = -1;
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        if( proxy == null ) proxy = new ComputerProxy( () -> this );
        return new ComputerPeripheral( getPeripheralName(), proxy );
    }

    @Nonnull
    protected String getPeripheralName()
    {
        return "computer";
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return hasCustomName()
            ? new LiteralText( label )
            : new TranslatableText( getCachedState().getBlock().getTranslationKey() );
    }

    @Override
    public boolean hasCustomName()
    {
        return !Strings.isNullOrEmpty( label );
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return hasCustomName() ? new LiteralText( label ) : null;
    }

    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Override
    public void writeScreenOpeningData( ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf )
    {
        packetByteBuf.writeInt( getServerComputer().getInstanceID() );
        packetByteBuf.writeEnumConstant( getServerComputer().getFamily() );
    }
}
