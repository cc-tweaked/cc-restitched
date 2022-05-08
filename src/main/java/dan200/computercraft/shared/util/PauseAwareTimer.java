/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

/**
 * A monotonically increasing clock which accounts for the game being paused.
 */
public final class PauseAwareTimer
{
    private static boolean paused;
    private static long pauseTime;
    private static long pauseOffset;

    private PauseAwareTimer()
    {
    }

    public static long getTime()
    {
        return (paused ? pauseTime : Util.getMeasuringTimeNano()) - pauseOffset;
    }

    public static void tick()
    {
        boolean isPaused = MinecraftClient.getInstance().isPaused();
        if( isPaused == paused ) return;

        if( isPaused )
        {
            pauseTime = Util.getMeasuringTimeNano();
            paused = true;
        }
        else
        {
            pauseOffset += Util.getMeasuringTimeNano() - pauseTime;
            paused = false;
        }
    }
}
