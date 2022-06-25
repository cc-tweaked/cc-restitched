/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.util;

import eu.pb4.common.protection.api.CommonProtection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CommonProtectionApiHooks
{
    public static final boolean IS_LOADED = FabricLoader.getInstance().isModLoaded( "common-protection-api" );

    public static boolean isBlockProtected( Level world, BlockPos pos, Player player )
    {
        if ( IS_LOADED )
        {
            return CommonProtection.canBreakBlock( world, pos, player.getGameProfile(), null ) && CommonProtection.canPlaceBlock( world, pos, player.getGameProfile(), null );
        }

        return true;
    }

    public static boolean isEntityAttackable( Level world, Entity entity, Player player )
    {
        if ( IS_LOADED )
        {
            return CommonProtection.canDamageEntity( world, entity, player.getGameProfile(), null );
        }

        return true;
    }
}
