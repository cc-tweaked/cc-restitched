/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraft.block.Block;
import net.minecraft.core.Holder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
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
        Collection<Identifier> tags = BlockTags.getAllTags().getMatchingTags( block );
        return getTags( tags );
    }

    @Nonnull
    static Map<String, Boolean> getTags( @Nonnull Item item )
    {
        Collection<Identifier> tags = ItemTags.getAllTags().getMatchingTags( item );
        return getTags( tags );
    }

    @Nullable
    public static String getId( @Nonnull Block block )
    {
        Identifier id = Registry.BLOCK.getId( block );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Item item )
    {
        Identifier id = Registry.ITEM.getId( item );
        return id == null ? null : id.toString();
    }

    @Nullable
    public static String getId( @Nonnull Enchantment enchantment )
    {
        Identifier id = Registry.ENCHANTMENT.getId( enchantment );
        return id == null ? null : id.toString();
    }
}
