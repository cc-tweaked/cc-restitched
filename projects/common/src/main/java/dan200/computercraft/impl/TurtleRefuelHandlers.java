/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleRefuelHandler;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of {@link TurtleRefuelHandler}s.
 */
public final class TurtleRefuelHandlers {
    private static final List<TurtleRefuelHandler> handlers = new CopyOnWriteArrayList<>();

    private TurtleRefuelHandlers() {
    }

    public static synchronized void register(TurtleRefuelHandler handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        handlers.add(handler);
    }

    public static OptionalInt refuel(ITurtleAccess turtle, ItemStack stack, int slot, int limit) {
        for (var handler : handlers) {
            var fuel = handler.refuel(turtle, stack, slot, limit);
            if (fuel.isPresent()) {
                var refuelled = fuel.getAsInt();
                if (refuelled < 0) throw new IllegalStateException(handler + " returned a negative value");
                if (limit == 0 && refuelled != 0) {
                    throw new IllegalStateException(handler + " refuelled despite given a limit of 0");
                }

                return fuel;
            }
        }

        return OptionalInt.empty();
    }
}
