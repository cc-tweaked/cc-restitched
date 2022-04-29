/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.fabricmc.fabric.impl.tag.convention.TagRegistration;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Additional conventional item tags not built in to Fabric.
 *
 * @see ConventionalItemTags
 */
public class ExtraConventionalItemTags
{
    public static final TagKey<Item> ENDER_PEARLS = register( "ender_pearls" );
    public static final TagKey<Item> GOLD_BLOCKS = register( "gold_blocks" );
    public static final TagKey<Item> SKULLS = register( "skulls" );
    public static final TagKey<Item> STONES = register( "stones" );
    public static final TagKey<Item> WOODEN_CHESTS = register( "wooden_chests" );

    private static TagKey<Item> register( String tagID )
    {
        return TagRegistration.ITEM_TAG_REGISTRATION.registerCommon( tagID );
    }
}
