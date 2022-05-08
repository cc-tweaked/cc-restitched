/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import javax.annotation.Nonnull;

public class TurtleCraftingTable extends AbstractTurtleUpgrade
{
    @Environment( EnvType.CLIENT )
    private static class Models
    {
        private static final ModelIdentifier leftModel = new ModelIdentifier( "computercraft:turtle_crafting_table_left", "inventory" );
        private static final ModelIdentifier rightModel = new ModelIdentifier( "computercraft:turtle_crafting_table_right", "inventory" );
    }

    public TurtleCraftingTable( Identifier id, ItemStack stack )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, "upgrade.minecraft.crafting_table.adjective", stack );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new CraftingTablePeripheral( turtle );
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( side == TurtleSide.LEFT ? Models.leftModel : Models.rightModel );
    }
}
