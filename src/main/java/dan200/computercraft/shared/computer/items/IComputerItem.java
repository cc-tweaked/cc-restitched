/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.items;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import javax.annotation.Nonnull;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public interface IComputerItem
{
    String NBT_ID = "ComputerId";

    default int getComputerID( @Nonnull ItemStack stack )
    {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains( NBT_ID ) ? nbt.getInt( NBT_ID ) : -1;
    }

    default String getLabel( @Nonnull ItemStack stack )
    {
        return stack.hasCustomName() ? stack.getName().getString() : null;
    }

    default boolean onEntityItemUpdate( ItemStack stack, ItemEntity entity )
    {
        return false;
    }

    ComputerFamily getFamily();

    ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family );
}
