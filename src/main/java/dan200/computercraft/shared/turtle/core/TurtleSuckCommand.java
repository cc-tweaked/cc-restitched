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
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtleSuckCommand implements ITurtleCommand
{
    private final InteractDirection direction;
    private final int quantity;

    public TurtleSuckCommand( InteractDirection direction, int quantity )
    {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Sucking nothing is easy
        if( quantity == 0 )
        {
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        Direction direction = this.direction.toWorldDir( turtle );

        // Get inventory for thing in front
        Level world = turtle.getLevel();
        BlockPos turtlePosition = turtle.getPosition();
        BlockPos blockPosition = turtlePosition.relative( direction );
        Direction side = direction.getOpposite();

        Container inventoryContainer = InventoryUtil.getInventory( world, blockPosition, side );

        if( inventoryContainer != null )
        {
            ItemStorage inventory = ItemStorage.wrap( inventoryContainer );

            // Simulate extracting from inventory of thing in front
            ItemStack simulatedExtraction = InventoryUtil.takeItems( quantity, inventory, true );
            if( simulatedExtraction.isEmpty() ) return TurtleCommandResult.failure( "No items to take" );

            // Simulate inserting result into turtle's inventory
            ItemStack simulatedRemainder = InventoryUtil.storeItems( simulatedExtraction, turtle.getItemHandler(), turtle.getSelectedSlot(), true );
            if( simulatedRemainder == simulatedExtraction ) return TurtleCommandResult.failure( "No space for items" );

            // Calculate how many items successfully transferred
            int transferCount = simulatedExtraction.getCount() - simulatedRemainder.getCount();

            // Execute the transaction
            ItemStack stack = InventoryUtil.takeItems( transferCount, inventory, false );
            ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot(), false );

            if( !remainder.isEmpty() )
            {
                ComputerCraft.log.error( "Items were lost during a TurtleSuckCommand!" );
                ComputerCraft.log.error( String.format( "from=%s quantity=%d, direction=%s", inventory, quantity, direction ) );
                ComputerCraft.log.error( "Please report this at https://github.com/cc-tweaked/cc-restitched/issues" );
            }

            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }
        else
        {
            // Suck up loose items off the ground
            AABB aabb = new AABB(
                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(),
                blockPosition.getX() + 1.0, blockPosition.getY() + 1.0, blockPosition.getZ() + 1.0
            );
            List<ItemEntity> list = world.getEntitiesOfClass( ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE );
            if( list.isEmpty() ) return TurtleCommandResult.failure( "No items to take" );

            for( ItemEntity entity : list )
            {
                // Suck up the item
                ItemStack stack = entity.getItem().copy();

                ItemStack storeStack;
                ItemStack leaveStack;
                if( stack.getCount() > quantity )
                {
                    storeStack = stack.split( quantity );
                    leaveStack = stack;
                }
                else
                {
                    storeStack = stack;
                    leaveStack = ItemStack.EMPTY;
                }

                ItemStack remainder = InventoryUtil.storeItems( storeStack, turtle.getItemHandler(), turtle.getSelectedSlot(), false );

                if( remainder != storeStack )
                {
                    if( remainder.isEmpty() && leaveStack.isEmpty() )
                    {
                        entity.discard();
                    }
                    else if( remainder.isEmpty() )
                    {
                        entity.setItem( leaveStack );
                    }
                    else if( leaveStack.isEmpty() )
                    {
                        entity.setItem( remainder );
                    }
                    else
                    {
                        leaveStack.grow( remainder.getCount() );
                        entity.setItem( leaveStack );
                    }

                    // Play fx
                    world.globalLevelEvent( 1000, turtlePosition, 0 ); // BLOCK_DISPENSER_DISPENSE
                    turtle.playAnimation( TurtleAnimation.WAIT );
                    return TurtleCommandResult.success();
                }
            }


            return TurtleCommandResult.failure( "No space for items" );
        }
    }
}
