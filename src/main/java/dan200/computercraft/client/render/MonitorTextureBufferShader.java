/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nullable;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import java.io.IOException;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.getColour;

public class MonitorTextureBufferShader extends Shader
{
    public static final int UNIFORM_SIZE = 4 * 4 * 16 + 4 + 4 + 2 * 4 + 4;

    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final Logger LOGGER = LogManager.getLogger();

    private final int monitorData;
    private int uniformBuffer = 0;

    private final GlUniform cursorBlink;

    public MonitorTextureBufferShader( ResourceFactory provider, String name, VertexFormat format ) throws IOException
    {
        super( provider, name, format );
        monitorData = GL31.glGetUniformBlockIndex( getProgramRef(), "MonitorData" );
        if( monitorData == -1 ) throw new IllegalStateException( "Could not find MonitorData uniform." );

        cursorBlink = getUniformChecked( "CursorBlink" );

        GlUniform tbo = getUniformChecked( "Tbo" );
        if( tbo != null ) tbo.set( TEXTURE_INDEX - GL13.GL_TEXTURE0 );
    }

    public void setupUniform( int buffer )
    {
        uniformBuffer = buffer;

        int cursorAlpha = FrameInfo.getGlobalCursorBlink() ? 1 : 0;
        if( cursorBlink != null && cursorBlink.getIntData().get( 0 ) != cursorAlpha ) cursorBlink.set( cursorAlpha );
    }

    @Override
    public void bind()
    {
        super.bind();
        GL31.glBindBufferBase( GL31.GL_UNIFORM_BUFFER, monitorData, uniformBuffer );
    }

    @Nullable
    private GlUniform getUniformChecked( String name )
    {
        GlUniform uniform = getUniform( name );
        if( uniform == null )
        {
            LOGGER.warn( "Monitor shader {} should have uniform {}, but it was not present.", getName(), name );
        }

        return uniform;
    }

    public static void setTerminalData( ByteBuffer buffer, Terminal terminal )
    {
        int width = terminal.getWidth(), height = terminal.getHeight();

        int pos = 0;
        for( int y = 0; y < height; y++ )
        {
            TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
            for( int x = 0; x < width; x++ )
            {
                buffer.put( pos, (byte) (text.charAt( x ) & 0xFF) );
                buffer.put( pos + 1, (byte) getColour( textColour.charAt( x ), Colour.WHITE ) );
                buffer.put( pos + 2, (byte) getColour( background.charAt( x ), Colour.BLACK ) );
                pos += 3;
            }
        }

        buffer.limit( pos );
    }

    public static void setUniformData( ByteBuffer buffer, Terminal terminal, boolean greyscale )
    {
        int pos = 0;
        var palette = terminal.getPalette();
        for( int i = 0; i < 16; i++ )
        {
            {
                double[] colour = palette.getColour( i );
                if( greyscale )
                {
                    float f = FixedWidthFontRenderer.toGreyscale( colour );
                    buffer.putFloat( pos, f ).putFloat( pos + 4, f ).putFloat( pos + 8, f );
                }
                else
                {
                    buffer.putFloat( pos, (float) colour[0] ).putFloat( pos + 4, (float) colour[1] ).putFloat( pos + 8, (float) colour[2] );
                }
            }

            pos += 4 * 4; // std140 requires these are 4-wide
        }

        boolean showCursor = FixedWidthFontRenderer.isCursorVisible( terminal );
        buffer
            .putInt( pos, terminal.getWidth() ).putInt( pos + 4, terminal.getHeight() )
            .putInt( pos + 8, showCursor ? terminal.getCursorX() : -2 )
            .putInt( pos + 12, showCursor ? terminal.getCursorY() : -2 )
            .putInt( pos + 16, 15 - terminal.getTextColour() );

        buffer.limit( UNIFORM_SIZE );
    }
}
