/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.sound;

import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import java.util.concurrent.Executor;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.Source;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class SpeakerSound extends AbstractSoundInstance implements TickableSoundInstance
{
    Source channel;
    Executor executor;
    DfpwmStream stream;

    private Entity entity;

    private boolean stopped = false;

    SpeakerSound( Identifier sound, DfpwmStream stream, SpeakerPosition position, float volume, float pitch )
    {
        super( sound, SoundCategory.RECORDS );
        setPosition( position );
        this.stream = stream;
        this.volume = volume;
        this.pitch = pitch;
        attenuationType = AttenuationType.LINEAR;
    }

    void setPosition( SpeakerPosition position )
    {
        x = position.position().x;
        y = position.position().y;
        z = position.position().z;
        entity = position.entity();
    }

    @Override
    public boolean isDone()
    {
        return stopped;
    }

    @Override
    public void tick()
    {
        if( entity == null ) return;
        if( !entity.isAlive() )
        {
            stopped = true;
            repeat = false;
        }
        else
        {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
        }
    }
}
