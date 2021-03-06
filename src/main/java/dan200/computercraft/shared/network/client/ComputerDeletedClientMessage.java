/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;

public class ComputerDeletedClientMessage extends ComputerClientMessage
{
    public ComputerDeletedClientMessage( int instanceId )
    {
        super( instanceId );
    }

    public ComputerDeletedClientMessage( FriendlyByteBuf buffer )
    {
        super( buffer );
    }

    @Override
    public void handle( PacketContext context )
    {
        ComputerCraft.clientComputerRegistry.remove( getInstanceId() );
    }
}
