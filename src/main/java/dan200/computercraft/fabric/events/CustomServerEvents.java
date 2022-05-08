/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

public final class CustomServerEvents
{

    public static final Event<ServerPlayerLoadedChunk> SERVER_PLAYER_LOADED_CHUNK_EVENT = EventFactory.createArrayBacked( ServerPlayerLoadedChunk.class,
        callbacks -> ( serverPlayer, chunkPos ) -> {
            for( ServerPlayerLoadedChunk callback : callbacks )
            {
                callback.onServerPlayerLoadedChunk( serverPlayer, chunkPos );
            }
        } );


    @FunctionalInterface
    public interface ServerPlayerLoadedChunk
    {
        void onServerPlayerLoadedChunk( ServerPlayerEntity player, ChunkPos chunkPos );
    }
}
