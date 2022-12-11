/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A side on a computer. This is relative to the direction the computer is facing.
 */
public enum ComputerSide {
    BOTTOM("bottom"),
    TOP("top"),
    BACK("back"),
    FRONT("front"),
    RIGHT("right"),
    LEFT("left");

    public static final List<String> NAMES = List.of("bottom", "top", "back", "front", "right", "left");

    public static final int COUNT = 6;

    private static final ComputerSide[] VALUES = values();

    private final String name;

    ComputerSide(String name) {
        this.name = name;
    }

    public static ComputerSide valueOf(int side) {
        return VALUES[side];
    }

    @Nullable
    public static ComputerSide valueOfInsensitive(String name) {
        for (var side : VALUES) {
            if (side.name.equalsIgnoreCase(name)) return side;
        }

        return null;
    }

    public String getName() {
        return name;
    }
}
