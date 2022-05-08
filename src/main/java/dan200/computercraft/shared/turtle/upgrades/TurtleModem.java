/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import javax.annotation.Nonnull;

public class TurtleModem extends AbstractTurtleUpgrade
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private final ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle, boolean advanced )
        {
            super( new ModemState(), advanced );
            this.turtle = turtle;
        }

        @Nonnull
        @Override
        public World getLevel()
        {
            return turtle.getLevel();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos turtlePos = turtle.getPosition();
            return new Vec3d(
                turtlePos.getX(),
                turtlePos.getY(),
                turtlePos.getZ()
            );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral modem && modem.turtle == turtle);
        }
    }

    @Environment( EnvType.CLIENT )
    private class Models
    {
        private final ModelIdentifier leftOffModel = advanced ?
            new ModelIdentifier( "computercraft:turtle_modem_advanced_off_left", "inventory" ) :
            new ModelIdentifier( "computercraft:turtle_modem_normal_off_left", "inventory" );

        private final ModelIdentifier rightOffModel = advanced ?
            new ModelIdentifier( "computercraft:turtle_modem_advanced_off_right", "inventory" ) :
            new ModelIdentifier( "computercraft:turtle_modem_normal_off_right", "inventory" );

        private final ModelIdentifier leftOnModel = advanced ?
            new ModelIdentifier( "computercraft:turtle_modem_advanced_on_left", "inventory" ) :
            new ModelIdentifier( "computercraft:turtle_modem_normal_on_left", "inventory" );

        private final ModelIdentifier rightOnModel = advanced ?
            new ModelIdentifier( "computercraft:turtle_modem_advanced_on_right", "inventory" ) :
            new ModelIdentifier( "computercraft:turtle_modem_normal_on_right", "inventory" );
    }

    private final boolean advanced;

    @Environment( EnvType.CLIENT )
    private Models models;

    public TurtleModem( Identifier id, ItemStack stack, boolean advanced )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, advanced ? WirelessModemPeripheral.ADVANCED_ADJECTIVE : WirelessModemPeripheral.NORMAL_ADJECTIVE, stack );
        this.advanced = advanced;
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new Peripheral( turtle, advanced );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction dir )
    {
        return TurtleCommandResult.failure();
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        if( models == null ) models = new Models();

        boolean active = false;
        if( turtle != null )
        {
            NbtCompound turtleNBT = turtle.getUpgradeNBTData( side );
            active = turtleNBT.contains( "active" ) && turtleNBT.getBoolean( "active" );
        }

        return side == TurtleSide.LEFT
            ? TransformedModel.of( active ? models.leftOnModel : models.leftOffModel )
            : TransformedModel.of( active ? models.rightOnModel : models.rightOffModel );
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        // Advance the modem
        if( !turtle.getLevel().isClient )
        {
            IPeripheral peripheral = turtle.getPeripheral( side );
            if( peripheral instanceof Peripheral modem )
            {
                ModemState state = modem.getModemState();
                if( state.pollChanged() )
                {
                    turtle.getUpgradeNBTData( side ).putBoolean( "active", state.isOpen() );
                    turtle.updateUpgradeNBTData( side );
                }
            }
        }
    }
}
