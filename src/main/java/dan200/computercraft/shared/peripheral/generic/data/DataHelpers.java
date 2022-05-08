/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DataHelpers
{
    private DataHelpers()
    {}

    public static <T> Map<String, Boolean> getTags( Holder.Reference<T> object )
    {
        return getTags( object.tags() );
    }

    @Nonnull
    public static <T> Map<String, Boolean> getTags( @Nonnull Stream<TagKey<T>> tags )
    {
        return tags.collect( Collectors.toMap( x -> x.location().toString(), x -> true ) );
    }

    @Nullable
    public static String getId( @Nonnull Block block )
    {
        ResourceLocation id = Registry.BLOCK.getKey( block );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Item item )
    {
        ResourceLocation id = Registry.ITEM.getKey( item );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Enchantment enchantment )
    {
        ResourceLocation id = Registry.ENCHANTMENT.getKey( enchantment );
        return id == null ? null : id.toString();
    }
}
