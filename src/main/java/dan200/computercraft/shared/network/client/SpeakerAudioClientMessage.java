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
import net.minecraft.network.PacketByteBuf;
import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Starts a sound on the client.
 *
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerAudioClientMessage implements NetworkMessage
{
    private final UUID source;
    private final SpeakerPosition.Message pos;
    private final ByteBuffer content;
    private final float volume;

    public SpeakerAudioClientMessage( UUID source, SpeakerPosition pos, float volume, ByteBuffer content )
    {
        this.source = source;
        this.pos = pos.asMessage();
        this.content = content;
        this.volume = volume;
    }

    public SpeakerAudioClientMessage( PacketByteBuf buf )
    {
        source = buf.readUuid();
        pos = SpeakerPosition.Message.read( buf );
        volume = buf.readFloat();

        SpeakerManager.getSound( source ).pushAudio( buf );
        content = null;
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeUuid( source );
        pos.write( buf );
        buf.writeFloat( volume );
        buf.writeBytes( content.duplicate() );
    }

    @Override
    @Environment( EnvType.CLIENT )
    public void handle( PacketContext context )
    {
        SpeakerManager.getSound( source ).playAudio( pos.reify(), volume );
    }
}
