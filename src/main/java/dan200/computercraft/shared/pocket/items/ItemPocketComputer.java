/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.network.container.ComputerContainerData;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import dan200.computercraft.shared.pocket.inventory.PocketComputerMenuProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem
{
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_LIGHT = "Light";

    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public ItemPocketComputer( Settings settings, ComputerFamily family )
    {
        super( settings );
        this.family = family;
    }

    public ItemStack create( int id, String label, int colour, IPocketUpgrade upgrade )
    {
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateNbt().putInt( NBT_ID, id );
        if( label != null ) result.setCustomName( new LiteralText( label ) );
        if( upgrade != null ) result.getOrCreateNbt().putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        if( colour != -1 ) result.getOrCreateNbt().putInt( NBT_COLOUR, colour );
        return result;
    }

    @Override
    public void appendStacks( @Nonnull ItemGroup group, @Nonnull DefaultedList<ItemStack> stacks )
    {
        if( !isIn( group ) ) return;
        stacks.add( create( -1, null, -1, null ) );
        PocketUpgrades.getVanillaUpgrades().map( x -> create( -1, null, -1, x ) ).forEach( stacks::add );
    }

    private boolean tick( @Nonnull ItemStack stack, @Nonnull World world, @Nonnull Entity entity, @Nonnull PocketServerComputer computer )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );

        computer.setLevel( world );
        computer.updateValues( entity, stack, upgrade );

        boolean changed = false;

        // Sync ID
        int id = computer.getID();
        if( id != getComputerID( stack ) )
        {
            changed = true;
            setComputerID( stack, id );
        }

        // Sync label
        String label = computer.getLabel();
        if( !Objects.equal( label, getLabel( stack ) ) )
        {
            changed = true;
            setLabel( stack, label );
        }

        // Update pocket upgrade
        if( upgrade != null ) upgrade.update( computer, computer.getPeripheral( ComputerSide.BACK ) );

        return changed;
    }

    @Override
    public void inventoryTick( @Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slotNum, boolean selected )
    {
        if( !world.isClient )
        {
            Inventory inventory = entity instanceof PlayerEntity player ? player.getInventory() : null;
            PocketServerComputer computer = createServerComputer( world, inventory, entity, stack );
            computer.keepAlive();

            boolean changed = tick( stack, world, entity, computer );
            if( changed && inventory != null ) inventory.markDirty();
        }
        else
        {
            createClientComputer( stack );
        }
    }

    @Override
    public boolean onEntityItemUpdate( ItemStack stack, ItemEntity entity )
    {
        if( entity.world.isClient ) return false;

        PocketServerComputer computer = getServerComputer( stack );
        if( computer != null && tick( stack, entity.world, entity, computer ) ) entity.setStack( stack.copy() );
        return false;
    }

    @Nonnull
    @Override
    public TypedActionResult<ItemStack> use( World world, PlayerEntity player, @Nonnull Hand hand )
    {
        ItemStack stack = player.getStackInHand( hand );
        if( !world.isClient )
        {
            PocketServerComputer computer = createServerComputer( world, player.getInventory(), player, stack );
            computer.turnOn();

            boolean stop = false;
            IPocketUpgrade upgrade = getUpgrade( stack );
            if( upgrade != null )
            {
                computer.updateValues( player, stack, upgrade );
                stop = upgrade.onRightClick( world, computer, computer.getPeripheral( ComputerSide.BACK ) );
            }

            if( !stop )
            {
                boolean isTypingOnly = hand == Hand.OFF_HAND;
                new ComputerContainerData( computer ).open( player, new PocketComputerMenuProvider( computer, stack, this, hand, isTypingOnly ) );
            }
        }
        return new TypedActionResult<>( ActionResult.SUCCESS, stack );
    }

    @Nonnull
    @Override
    public Text getName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            return new TranslatableText( baseString + ".upgraded",
                new TranslatableText( upgrade.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return super.getName( stack );
        }
    }


    @Override
    public void appendTooltip( @Nonnull ItemStack stack, @Nullable World world, @Nonnull List<Text> list, TooltipContext flag )
    {
        if( flag.isAdvanced() || getLabel( stack ) == null )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                list.add( new TranslatableText( "gui.computercraft.tooltip.computer_id", id )
                    .formatted( Formatting.GRAY ) );
            }
        }
    }

    public PocketServerComputer createServerComputer( final World world, Inventory inventory, Entity entity, @Nonnull ItemStack stack )
    {
        if( world.isClient ) throw new IllegalStateException( "Cannot call createServerComputer on the client" );

        PocketServerComputer computer;
        int instanceID = getInstanceID( stack );
        int sessionID = getSessionID( stack );
        int correctSessionID = ComputerCraft.serverComputerRegistry.getSessionID();

        if( instanceID >= 0 && sessionID == correctSessionID && ComputerCraft.serverComputerRegistry.contains( instanceID ) )
        {
            computer = (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID );
        }
        else
        {
            if( instanceID < 0 || sessionID != correctSessionID )
            {
                instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                setInstanceID( stack, instanceID );
                setSessionID( stack, correctSessionID );
            }
            int computerID = getComputerID( stack );
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer" );
                setComputerID( stack, computerID );
            }
            computer = new PocketServerComputer( world, computerID, getLabel( stack ), instanceID, getFamily() );
            computer.updateValues( entity, stack, getUpgrade( stack ) );
            computer.addAPI( new PocketAPI( computer ) );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            if( inventory != null ) inventory.markDirty();
        }
        computer.setLevel( world );
        return computer;
    }

    @Nullable
    public static PocketServerComputer getServerComputer( @Nonnull ItemStack stack )
    {
        int session = getSessionID( stack );
        if( session != ComputerCraft.serverComputerRegistry.getSessionID() ) return null;

        int instanceID = getInstanceID( stack );
        return instanceID >= 0 ? (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID ) : null;
    }

    @Nullable
    public static ClientComputer createClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        if( instanceID >= 0 )
        {
            if( !ComputerCraft.clientComputerRegistry.contains( instanceID ) )
            {
                ComputerCraft.clientComputerRegistry.add( instanceID, new ClientComputer( instanceID ) );
            }
            return ComputerCraft.clientComputerRegistry.get( instanceID );
        }
        return null;
    }

    @Nullable
    private static ClientComputer getClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        return instanceID >= 0 ? ComputerCraft.clientComputerRegistry.get( instanceID ) : null;
    }

    // IComputerItem implementation

    private static void setComputerID( @Nonnull ItemStack stack, int computerID )
    {
        stack.getOrCreateNbt().putInt( NBT_ID, computerID );
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return PocketComputerItemFactory.create(
            getComputerID( stack ), getLabel( stack ), getColour( stack ),
            family, getUpgrade( stack )
        );
    }

    // IMedia

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setCustomName( new LiteralText( label ) );
        }
        else
        {
            stack.removeCustomName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
        }
        return null;
    }

    private static int getInstanceID( @Nonnull ItemStack stack )
    {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains( NBT_INSTANCE ) ? nbt.getInt( NBT_INSTANCE ) : -1;
    }

    private static void setInstanceID( @Nonnull ItemStack stack, int instanceID )
    {
        stack.getOrCreateNbt().putInt( NBT_INSTANCE, instanceID );
    }

    private static int getSessionID( @Nonnull ItemStack stack )
    {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains( NBT_SESSION ) ? nbt.getInt( NBT_SESSION ) : -1;
    }

    private static void setSessionID( @Nonnull ItemStack stack, int sessionID )
    {
        stack.getOrCreateNbt().putInt( NBT_SESSION, sessionID );
    }

    public static ComputerState getState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        return computer == null ? ComputerState.OFF : computer.getState();
    }

    public static int getLightState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        if( computer != null && computer.isOn() )
        {
            NbtCompound computerNBT = computer.getUserData();
            if( computerNBT != null && computerNBT.contains( NBT_LIGHT ) )
            {
                return computerNBT.getInt( NBT_LIGHT );
            }
        }
        return -1;
    }

    public static IPocketUpgrade getUpgrade( @Nonnull ItemStack stack )
    {
        NbtCompound compound = stack.getNbt();
        return compound != null && compound.contains( NBT_UPGRADE )
            ? PocketUpgrades.get( compound.getString( NBT_UPGRADE ) ) : null;
    }

    public static void setUpgrade( @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        NbtCompound compound = stack.getOrCreateNbt();

        if( upgrade == null )
        {
            compound.remove( NBT_UPGRADE );
        }
        else
        {
            compound.putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        }

        compound.remove( NBT_UPGRADE_INFO );
    }

    public static NbtCompound getUpgradeInfo( @Nonnull ItemStack stack )
    {
        return stack.getOrCreateSubNbt( NBT_UPGRADE_INFO );
    }
}
