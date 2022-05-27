/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat
{
    public static final IrisCompat INSTANCE = FabricLoader.getInstance().isModLoaded( "iris" ) ? new Impl() : new IrisCompat();

    public boolean isRenderingShadowPass()
    {
        return false;
    }

    private static class Impl extends IrisCompat
    {
        @Override
        public boolean isRenderingShadowPass()
        {
            return IrisApi.getInstance().isRenderingShadowPass();
        }
    }
}
