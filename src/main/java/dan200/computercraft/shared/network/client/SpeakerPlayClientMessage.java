/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.PacketContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Starts a sound on the client.
 *
 * Used by speakers to play sounds.
 *
 * @see dan200.computercraft.shared.peripheral.speaker.TileSpeaker
 */
public class SpeakerPlayClientMessage implements NetworkMessage
{
    private final UUID source;
    private final SpeakerPosition.Message pos;
    private final Identifier sound;
    private final float volume;
    private final float pitch;

    public SpeakerPlayClientMessage( UUID source, SpeakerPosition pos, Identifier event, float volume, float pitch )
    {
        this.source = source;
        this.pos = pos.asMessage();
        sound = event;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SpeakerPlayClientMessage( PacketByteBuf buf )
    {
        source = buf.readUuid();
        pos = SpeakerPosition.Message.read( buf );
        sound = buf.readIdentifier();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void toBytes( @Nonnull PacketByteBuf buf )
    {
        buf.writeUuid( source );
        pos.write( buf );
        buf.writeIdentifier( sound );
        buf.writeFloat( volume );
        buf.writeFloat( pitch );
    }

    @Override
    @Environment( EnvType.CLIENT )
    public void handle( PacketContext context )
    {
        SpeakerManager.getSound( source ).playSound( pos.reify(), sound, volume, pitch );
    }
}
