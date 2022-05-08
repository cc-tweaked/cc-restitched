/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class DropConsumer
{
    private DropConsumer()
    {
    }

    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static World dropWorld;
    private static BlockPos dropPos;
    private static Box dropBounds;
    private static Entity dropEntity;

    public static void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = entity;
        dropWorld = entity.world;
        dropPos = null;
        dropBounds = new Box( entity.getBlockPos() ).expand( 2, 2, 2 );
    }

    public static void set( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>( 2 );
        dropEntity = null;
        dropWorld = world;
        dropBounds = new Box( pos ).expand( 2, 2, 2 );
    }

    public static List<ItemStack> clear()
    {
        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    public static void clearAndDrop( World world, BlockPos pos, Direction direction )
    {
        List<ItemStack> remainingDrops = clear();
        for( ItemStack remaining : remainingDrops ) WorldUtil.dropItemStack( remaining, world, pos, direction );
    }

    private static void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    public static boolean onEntitySpawn( Entity entity )
    {
        // Capture any nearby item spawns
        if( dropWorld != null && dropWorld == entity.getEntityWorld() && entity instanceof ItemEntity
            && dropBounds.contains( entity.getPos() ) )
        {
            handleDrops( ((ItemEntity) entity).getStack() );
            return true;
        }
        return false;
    }

    public static boolean onLivingDrops( Entity entity, ItemStack stack )
    {
        if( dropEntity == null || entity != dropEntity ) return false;

        handleDrops( stack );
        return true;
    }

    public static boolean onHarvestDrops( World world, BlockPos pos, ItemStack stack )
    {
        if( dropWorld != null && dropWorld == world && dropPos != null && dropPos.equals( pos ) )
        {
            handleDrops( stack );
            return true;
        }
        return false;
    }
}
