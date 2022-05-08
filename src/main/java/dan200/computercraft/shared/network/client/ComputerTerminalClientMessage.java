/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;


import dan200.computercraft.shared.network.PacketContext;
import javax.annotation.Nonnull;
import net.minecraft.network.PacketByteBuf;

public class ComputerTerminalClientMessage extends ComputerClientMessage
{
    private final TerminalState state;

    public ComputerTerminalClientMessage( int instanceId, TerminalState state )
    {
        super( instanceId );
        this.state = state;
    }

    public ComputerTerminalClientMessage( @Nonnull PacketByteBuf buf )
    {
        super( buf );
        state = new TerminalState( buf );
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        super.toBytes( buf );
        state.write( buf );
    }

    @Override
    public void handle( PacketContext context )
    {
        getComputer().read( state );
    }
}
