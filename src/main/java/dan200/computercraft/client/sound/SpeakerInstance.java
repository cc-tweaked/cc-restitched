/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;

/**
 * An instance of a speaker, which is either playing a {@link DfpwmStream} stream or a normal sound.
 */
public class SpeakerInstance
{
    public static final Identifier DFPWM_STREAM = new Identifier( ComputerCraft.MOD_ID, "speaker.dfpwm_fake_audio_should_not_be_played" );

    private DfpwmStream currentStream;
    private SpeakerSound sound;

    SpeakerInstance()
    {
    }

    public synchronized void pushAudio( ByteBuf buffer )
    {
        SpeakerSound sound = this.sound;

        DfpwmStream stream = currentStream;
        if( stream == null ) stream = currentStream = new DfpwmStream();
        boolean exhausted = stream.isEmpty();
        currentStream.push( buffer );

        // If we've got nothing left in the buffer, enqueue an additional one just in case.
        if( exhausted && sound != null && sound.stream == stream && sound.channel != null )
        {
            sound.executor.execute( () -> {
                if( !sound.channel.isStopped() ) sound.channel.read( 1 );
            } );
        }
    }

    public void playAudio( SpeakerPosition position, float volume )
    {
        var soundManager = MinecraftClient.getInstance().getSoundManager();

        if( sound != null && sound.stream != currentStream )
        {
            soundManager.stop( sound );
            sound = null;
        }

        if( sound != null && !soundManager.isPlaying( sound ) ) sound = null;

        if( sound == null && currentStream != null )
        {
            sound = new SpeakerSound( DFPWM_STREAM, currentStream, position, volume, 1.0f );
            soundManager.play( sound );
        }
    }

    public void playSound( SpeakerPosition position, Identifier location, float volume, float pitch )
    {
        var soundManager = MinecraftClient.getInstance().getSoundManager();
        currentStream = null;

        if( sound != null )
        {
            soundManager.stop( sound );
            sound = null;
        }

        sound = new SpeakerSound( location, null, position, volume, pitch );
        soundManager.play( sound );
    }

    void setPosition( SpeakerPosition position )
    {
        if( sound != null ) sound.setPosition( position );
    }

    void stop()
    {
        if( sound != null ) MinecraftClient.getInstance().getSoundManager().stop( sound );

        currentStream = null;
        sound = null;
    }
}
