/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.util.ColourUtils;
import dan200.computercraft.shared.util.DefaultSidedInventory;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TilePrinter extends TileGeneric implements IPeripheralTile, DefaultSidedInventory, Nameable, NamedScreenHandlerFactory
{
    private static final String NBT_NAME = "CustomName";
    private static final String NBT_PRINTING = "Printing";
    private static final String NBT_PAGE_TITLE = "PageTitle";

    static final int SLOTS = 13;

    private static final int[] BOTTOM_SLOTS = new int[] { 7, 8, 9, 10, 11, 12 };
    private static final int[] TOP_SLOTS = new int[] { 1, 2, 3, 4, 5, 6 };
    private static final int[] SIDE_SLOTS = new int[] { 0 };

    Text customName;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize( SLOTS, ItemStack.EMPTY );
    private PrinterPeripheral peripheral;

    private final Terminal page = new Terminal( ItemPrintout.LINE_MAX_LENGTH, ItemPrintout.LINES_PER_PAGE );
    private String pageTitle = "";
    private boolean printing = false;

    public TilePrinter( BlockEntityType<TilePrinter> type, BlockPos pos, BlockState state )
    {
        super( type, pos, state );
    }

    @Override
    public void destroy()
    {
        ejectContents();
    }

    @Nonnull
    @Override
    public ActionResult onActivate( PlayerEntity player, Hand hand, BlockHitResult hit )
    {
        if( player.isInSneakingPose() ) return ActionResult.PASS;

        if( !getWorld().isClient ) player.openHandledScreen( this );
        return ActionResult.SUCCESS;
    }

    @Override
    public void readNbt( @Nonnull NbtCompound nbt )
    {
        super.readNbt( nbt );

        customName = nbt.contains( NBT_NAME ) ? Text.Serializer.fromJson( nbt.getString( NBT_NAME ) ) : null;

        // Read page
        synchronized( page )
        {
            printing = nbt.getBoolean( NBT_PRINTING );
            pageTitle = nbt.getString( NBT_PAGE_TITLE );
            page.readFromNBT( nbt );
        }

        // Read inventory
        Inventories.readNbt( nbt, inventory );
    }

    @Override
    public void writeNbt( @Nonnull NbtCompound nbt )
    {
        if( customName != null ) nbt.putString( NBT_NAME, Text.Serializer.toJson( customName ) );

        // Write page
        synchronized( page )
        {
            nbt.putBoolean( NBT_PRINTING, printing );
            nbt.putString( NBT_PAGE_TITLE, pageTitle );
            page.writeToNBT( nbt );
        }

        // Write inventory
        Inventories.writeNbt( nbt, inventory );

        super.writeNbt( nbt );
    }

    boolean isPrinting()
    {
        return printing;
    }

    // IInventory implementation
    @Override
    public int size()
    {
        return inventory.size();
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
        return inventory.get( slot );
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot )
    {
        ItemStack result = inventory.get( slot );
        inventory.set( slot, ItemStack.EMPTY );
        markDirty();
        updateBlockState();
        return result;
    }

    @Nonnull
    @Override
    public ItemStack removeStack( int slot, int count )
    {
        ItemStack stack = inventory.get( slot );
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        if( stack.getCount() <= count )
        {
            setStack( slot, ItemStack.EMPTY );
            return stack;
        }

        ItemStack part = stack.split( count );
        if( inventory.get( slot ).isEmpty() )
        {
            inventory.set( slot, ItemStack.EMPTY );
            updateBlockState();
        }
        markDirty();
        return part;
    }

    @Override
    public void setStack( int slot, @Nonnull ItemStack stack )
    {
        inventory.set( slot, stack );
        markDirty();
        updateBlockState();
    }

    @Override
    public void clear()
    {
        for( int i = 0; i < inventory.size(); i++ ) inventory.set( i, ItemStack.EMPTY );
        markDirty();
        updateBlockState();
    }

    @Override
    public boolean isValid( int slot, @Nonnull ItemStack stack )
    {
        if( slot == 0 )
        {
            return isInk( stack );
        }
        else if( slot >= TOP_SLOTS[0] && slot <= TOP_SLOTS[TOP_SLOTS.length - 1] )
        {
            return isPaper( stack );
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean canPlayerUse( @Nonnull PlayerEntity playerEntity )
    {
        return isUsable( playerEntity, false );
    }

    // ISidedInventory implementation

    @Nonnull
    @Override
    public int[] getAvailableSlots( @Nonnull Direction side )
    {
        switch( side )
        {
            case DOWN: // Bottom (Out tray)
                return BOTTOM_SLOTS;
            case UP: // Top (In tray)
                return TOP_SLOTS;
            default: // Sides (Ink)
                return SIDE_SLOTS;
        }
    }

    @Nullable
    Terminal getCurrentPage()
    {
        synchronized( page )
        {
            return printing ? page : null;
        }
    }

    boolean startNewPage()
    {
        synchronized( page )
        {
            if( !canInputPage() ) return false;
            if( printing && !outputPage() ) return false;
            return inputPage();
        }
    }

    boolean endCurrentPage()
    {
        synchronized( page )
        {
            return printing && outputPage();
        }
    }

    int getInkLevel()
    {
        ItemStack inkStack = inventory.get( 0 );
        return isInk( inkStack ) ? inkStack.getCount() : 0;
    }

    int getPaperLevel()
    {
        int count = 0;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = inventory.get( i );
            if( isPaper( paperStack ) ) count += paperStack.getCount();
        }
        return count;
    }

    void setPageTitle( String title )
    {
        synchronized( page )
        {
            if( printing ) pageTitle = title;
        }
    }

    static boolean isInk( @Nonnull ItemStack stack )
    {
        return ColourUtils.getStackColour( stack ) != null;
    }

    private static boolean isPaper( @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        return item == Items.PAPER
            || (item instanceof ItemPrintout printout && printout.getType() == ItemPrintout.Type.PAGE);
    }

    private boolean canInputPage()
    {
        ItemStack inkStack = inventory.get( 0 );
        return !inkStack.isEmpty() && isInk( inkStack ) && getPaperLevel() > 0;
    }

    private boolean inputPage()
    {
        ItemStack inkStack = inventory.get( 0 );
        DyeColor dye = ColourUtils.getStackColour( inkStack );
        if( dye == null ) return false;

        for( int i = 1; i < 7; i++ )
        {
            ItemStack paperStack = inventory.get( i );
            if( paperStack.isEmpty() || !isPaper( paperStack ) ) continue;

            // Setup the new page
            page.setTextColour( dye.getId() );

            page.clear();
            if( paperStack.getItem() instanceof ItemPrintout )
            {
                pageTitle = ItemPrintout.getTitle( paperStack );
                String[] text = ItemPrintout.getText( paperStack );
                String[] textColour = ItemPrintout.getColours( paperStack );
                for( int y = 0; y < page.getHeight(); y++ )
                {
                    page.setLine( y, text[y], textColour[y], "" );
                }
            }
            else
            {
                pageTitle = "";
            }
            page.setCursorPos( 0, 0 );

            // Decrement ink
            inkStack.decrement( 1 );
            if( inkStack.isEmpty() ) inventory.set( 0, ItemStack.EMPTY );

            // Decrement paper
            paperStack.decrement( 1 );
            if( paperStack.isEmpty() )
            {
                inventory.set( i, ItemStack.EMPTY );
                updateBlockState();
            }

            markDirty();
            printing = true;
            return true;
        }
        return false;
    }

    private boolean outputPage()
    {
        int height = page.getHeight();
        String[] lines = new String[height];
        String[] colours = new String[height];
        for( int i = 0; i < height; i++ )
        {
            lines[i] = page.getLine( i ).toString();
            colours[i] = page.getTextColourLine( i ).toString();
        }

        ItemStack stack = ItemPrintout.createSingleFromTitleAndText( pageTitle, lines, colours );
        for( int slot : BOTTOM_SLOTS )
        {
            if( inventory.get( slot ).isEmpty() )
            {
                setStack( slot, stack );
                printing = false;
                return true;
            }
        }
        return false;
    }

    private void ejectContents()
    {
        for( int i = 0; i < 13; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() )
            {
                // Remove the stack from the inventory
                setStack( i, ItemStack.EMPTY );

                // Spawn the item in the world
                WorldUtil.dropItemStack( stack, getWorld(), Vec3d.of( getPos() ).add( 0.5, 0.75, 0.5 ) );
            }
        }
    }

    private void updateBlockState()
    {
        boolean top = false, bottom = false;
        for( int i = 1; i < 7; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                top = true;
                break;
            }
        }
        for( int i = 7; i < 13; i++ )
        {
            ItemStack stack = inventory.get( i );
            if( !stack.isEmpty() && isPaper( stack ) )
            {
                bottom = true;
                break;
            }
        }

        updateBlockState( top, bottom );
    }

    private void updateBlockState( boolean top, boolean bottom )
    {
        if( removed || world == null ) return;

        BlockState state = getCachedState();
        if( state.get( BlockPrinter.TOP ) == top & state.get( BlockPrinter.BOTTOM ) == bottom ) return;

        getWorld().setBlockState( getPos(), state.with( BlockPrinter.TOP, top ).with( BlockPrinter.BOTTOM, bottom ) );
    }

    @Nonnull
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        if( peripheral == null ) peripheral = new PrinterPeripheral( this );
        return peripheral;
    }

    @Override
    public boolean hasCustomName()
    {
        return customName != null;
    }

    @Nullable
    @Override
    public Text getCustomName()
    {
        return customName;
    }

    @Nonnull
    @Override
    public Text getName()
    {
        return customName != null ? customName : new TranslatableText( getCachedState().getBlock().getTranslationKey() );
    }

    @Nonnull
    @Override
    public Text getDisplayName()
    {
        return Nameable.super.getDisplayName();
    }

    @Nonnull
    @Override
    public ScreenHandler createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
    {
        return new ContainerPrinter( id, inventory, this );
    }
}
