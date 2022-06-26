/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class GameInstanceUtils
{
    private static MinecraftServer server = null;

    public static MinecraftServer getServer()
    {
        return server;
    }

    public static void init()
    {
        ServerLifecycleEvents.SERVER_STARTING.register( serverx -> server = serverx );
        ServerLifecycleEvents.SERVER_STOPPED.register( serverx -> server = null );
    }
}
