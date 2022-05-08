/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import javax.annotation.Nonnull;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;


public class RenderTypes
{
    public static final Identifier FONT = new Identifier( "computercraft", "textures/gui/term_font.png" );
    public static final Identifier PRINTOUT_BACKGROUND = new Identifier( "computercraft", "textures/gui/printout.png" );
    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    public static final RenderLayer MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderLayer MONITOR = RenderLayer.getTextIntensity( FONT );

    public static final RenderLayer ITEM_POCKET_TERMINAL = RenderLayer.getTextIntensity( FONT );
    public static final RenderLayer ITEM_POCKET_LIGHT = RenderLayer.getTextIntensity( FONT );
    public static final RenderLayer ITEM_PRINTOUT_BACKGROUND = RenderLayer.getEntityCutout( PRINTOUT_BACKGROUND );
    public static final RenderLayer ITEM_PRINTOUT_TEXT = RenderLayer.getEntityCutout( FONT );

    public static final RenderLayer GUI_TERMINAL = RenderLayer.getText( FONT );
    public static final RenderLayer GUI_PRINTOUT_BACKGROUND = RenderLayer.getText( PRINTOUT_BACKGROUND );
    public static final RenderLayer GUI_PRINTOUT_TEXT = RenderLayer.getText( FONT );

    public static Shader getMonitorShader()
    {
        return GameRenderer.getRenderTypeTextIntensityShader();
    }

    public static RenderLayer itemPocketBorder( Identifier location )
    {
        return RenderLayer.getEntityCutout( location );
    }

    public static RenderLayer guiComputerBorder( Identifier location )
    {
        return RenderLayer.getText( location );
    }

    public static MonitorTextureBufferShader monitorTboShader;

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    private static final class Types extends RenderPhase
    {
        private static final RenderPhase.Texture TERM_FONT_TEXTURE = new Texture(
            FONT,
            false, false // blur, minimap
        );

        static final RenderLayer MONITOR_TBO = RenderLayer.of(
            "monitor_tbo", VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.TRIANGLE_STRIP, 128,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( TERM_FONT_TEXTURE )
                .shader( new net.minecraft.client.render.RenderPhase.Shader( RenderTypes::getMonitorTextureBufferShader ) )
                .build( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
