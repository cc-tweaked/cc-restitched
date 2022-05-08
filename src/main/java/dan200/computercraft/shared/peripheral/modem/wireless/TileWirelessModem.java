/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TileWirelessModem extends TileGeneric implements IPeripheralTile
{
    private static class Peripheral extends WirelessModemPeripheral
    {
        private final TileWirelessModem entity;

        Peripheral( TileWirelessModem entity )
        {
            super( new ModemState( () -> TickScheduler.schedule( entity ) ), entity.advanced );
            this.entity = entity;
        }

        @Nonnull
        @Override
        public World getLevel()
        {
            return entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            return Vec3d.of( entity.getPos().offset( entity.getDirection() ) );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }

        @Nonnull
        @Override
        public Object getTarget()
        {
            return entity;
        }
    }

    private final boolean advanced;

    private final ModemPeripheral modem;
    private boolean destroyed = false;

    public TileWirelessModem( BlockEntityType<? extends TileWirelessModem> type, BlockPos pos, BlockState state, boolean advanced )
    {
        super( type, pos, state );
        this.advanced = advanced;
        modem = new Peripheral( this );
    }

    @Override
    public void cancelRemoval()
    {
        super.cancelRemoval(); // TODO: Replace with onLoad
        TickScheduler.schedule( this );
    }

    @Override
    public void destroy()
    {
        if( !destroyed )
        {
            modem.destroy();
            destroyed = true;
        }
    }

    @Override
    public void blockTick()
    {
        if( modem.getModemState().pollChanged() ) updateBlockState();
    }

    @Nonnull
    private Direction getDirection()
    {
        return getCachedState().get( BlockWirelessModem.FACING );
    }

    private void updateBlockState()
    {
        boolean on = modem.getModemState().isOpen();
        BlockState state = getCachedState();
        if( state.get( BlockWirelessModem.ON ) != on )
        {
            getWorld().setBlockState( getPos(), state.with( BlockWirelessModem.ON, on ) );
        }
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral( Direction side )
    {
        if( side != null && getDirection() != side ) return null;
        return modem;
    }
}
