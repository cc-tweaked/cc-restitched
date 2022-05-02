/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.network.PacketContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Starts a sound on the client.
 *
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerMoveClientMessage implements NetworkMessage
{
    private final UUID source;
    private final SpeakerPosition.Message pos;

    public SpeakerMoveClientMessage( UUID source, SpeakerPosition pos )
    {
        this.source = source;
        this.pos = pos.asMessage();
    }

    public SpeakerMoveClientMessage( FriendlyByteBuf buf )
    {
        source = buf.readUUID();
        pos = SpeakerPosition.Message.read( buf );
    }

    @Override
    public void toBytes( @Nonnull FriendlyByteBuf buf )
    {
        buf.writeUUID( source );
        pos.write( buf );
    }

    @Override
    @Environment( EnvType.CLIENT )
    public void handle( PacketContext context )
    {
        SpeakerManager.moveSound( source, pos.reify() );
    }
}
