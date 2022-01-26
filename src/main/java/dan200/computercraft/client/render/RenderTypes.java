/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT;

public class RenderTypes
{
    public static final int FULL_BRIGHT_LIGHTMAP = (0xF << 4) | (0xF << 20);

    public static MonitorTextureBufferShader monitorTboShader;

    private static final RenderType ENTITY_CUTOUT_PRINTOUT = RenderType.entityCutout( new ResourceLocation( "computercraft", "textures/gui/printout.png" ) );
    private static final RenderType ENTITY_CUTOUT_FONT = RenderType.entityCutout( FONT );

    public static final RenderType MONITOR_TBO = Types.MONITOR_TBO;
    public static final RenderType MONITOR = ENTITY_CUTOUT_FONT;

    public static final RenderType ITEM_POCKET = ENTITY_CUTOUT_FONT;
    public static final RenderType ITEM_POCKET_LIGHT = RenderType.text( FONT );

    public static final RenderType ITEM_PRINTOUT_BACKGROUND = ENTITY_CUTOUT_PRINTOUT;
    public static final RenderType ITEM_PRINTOUT_TEXT = ENTITY_CUTOUT_FONT;

    public static final RenderType POSITION_COLOR = Types.POSITION_COLOR;

    @Nonnull
    static MonitorTextureBufferShader getMonitorTextureBufferShader()
    {
        if( monitorTboShader == null ) throw new NullPointerException( "MonitorTboShader has not been registered" );
        return monitorTboShader;
    }

    private static final class Types extends RenderStateShard
    {
        private static final RenderStateShard.TextureStateShard TERM_FONT_TEXTURE = new TextureStateShard(
            FONT,
            false, false // blur, minimap
        );

        static final RenderType MONITOR_TBO = RenderType.create(
            "monitor_tbo", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_STRIP, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setTextureState( TERM_FONT_TEXTURE )
                .setShaderState( new ShaderStateShard( RenderTypes::getMonitorTextureBufferShader ) )
                .setWriteMaskState( COLOR_WRITE )
                .createCompositeState( false )
        );

        static final RenderType POSITION_COLOR = RenderType.create(
            "position_color", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 128,
            false, false, // useDelegate, needsSorting
            RenderType.CompositeState.builder()
                .setShaderState( POSITION_COLOR_SHADER )
                .createCompositeState( false )
        );

        private Types( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
