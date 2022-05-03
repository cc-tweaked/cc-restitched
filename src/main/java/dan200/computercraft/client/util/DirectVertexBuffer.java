/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

/**
 * A version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly.
 *
 * This should probably be its own class (rather than subclassing), but I need access to {@link VertexBuffer#drawWithShader}.
 */
public class DirectVertexBuffer extends VertexBuffer
{
    private int actualIndexCount;

    public DirectVertexBuffer()
    {
        if( DirectBuffers.HAS_DSA )
        {
            RenderSystem.glDeleteBuffers( vertextBufferId );
            if( DirectBuffers.ON_LINUX ) BufferUploader.reset(); // See comment on DirectBuffers.deleteBuffer.
            vertextBufferId = GL45C.glCreateBuffers();
        }
    }

    public void upload( int vertexCount, VertexFormat.Mode mode, VertexFormat format, ByteBuffer buffer )
    {
        RenderSystem.assertOnRenderThread();

        DirectBuffers.setBufferData( GL15.GL_ARRAY_BUFFER, vertextBufferId, buffer, GL15.GL_STATIC_DRAW );

        this.format = format;
        this.mode = mode;
        actualIndexCount = indexCount = mode.indexCount( vertexCount );
        indexType = VertexFormat.IndexType.SHORT;
        sequentialIndices = true;
    }

    public void drawWithShader( Matrix4f modelView, Matrix4f projection, ShaderInstance shader, int vertexCount, int baseVertex )
    {
        indexCount = mode.indexCount( vertexCount );
        drawWithShaderBaseVertex( modelView, projection, shader, baseVertex );
        indexCount = actualIndexCount;
    }

    private void drawWithShaderBaseVertex( Matrix4f matrix4f, Matrix4f matrix4f2, ShaderInstance shaderInstance, int baseVertex )
    {
        if ( indexCount == 0 )
        {
            return;
        }
        RenderSystem.assertOnRenderThread();
        BufferUploader.reset();
        for ( int i = 0; i < 12; ++i )
        {
            int j = RenderSystem.getShaderTexture( i );
            shaderInstance.setSampler( "Sampler" + i, j );
        }
        if ( shaderInstance.MODEL_VIEW_MATRIX != null )
        {
            shaderInstance.MODEL_VIEW_MATRIX.set( matrix4f );
        }
        if ( shaderInstance.PROJECTION_MATRIX != null )
        {
            shaderInstance.PROJECTION_MATRIX.set( matrix4f2 );
        }
        if ( shaderInstance.INVERSE_VIEW_ROTATION_MATRIX != null )
        {
            shaderInstance.INVERSE_VIEW_ROTATION_MATRIX.set( RenderSystem.getInverseViewRotationMatrix() );
        }
        if ( shaderInstance.COLOR_MODULATOR != null )
        {
            shaderInstance.COLOR_MODULATOR.set( RenderSystem.getShaderColor() );
        }
        if ( shaderInstance.FOG_START != null )
        {
            shaderInstance.FOG_START.set( RenderSystem.getShaderFogStart() );
        }
        if ( shaderInstance.FOG_END != null )
        {
            shaderInstance.FOG_END.set( RenderSystem.getShaderFogEnd() );
        }
        if ( shaderInstance.FOG_COLOR != null )
        {
            shaderInstance.FOG_COLOR.set( RenderSystem.getShaderFogColor() );
        }
        if ( shaderInstance.FOG_SHAPE != null )
        {
            shaderInstance.FOG_SHAPE.set( RenderSystem.getShaderFogShape().getIndex() );
        }
        if ( shaderInstance.TEXTURE_MATRIX != null )
        {
            shaderInstance.TEXTURE_MATRIX.set( RenderSystem.getTextureMatrix() );
        }
        if ( shaderInstance.GAME_TIME != null )
        {
            shaderInstance.GAME_TIME.set( RenderSystem.getShaderGameTime() );
        }
        if ( shaderInstance.SCREEN_SIZE != null )
        {
            Window window = Minecraft.getInstance().getWindow();
            shaderInstance.SCREEN_SIZE.set( (float)window.getWidth(), (float)window.getHeight() );
        }
        if ( shaderInstance.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP) )
        {
            shaderInstance.LINE_WIDTH.set( RenderSystem.getShaderLineWidth() );
        }
        RenderSystem.setupShaderLights( shaderInstance );
        bindVertexArray();
        bind();
        getFormat().setupBufferState();
        shaderInstance.apply();
        GL32.glDrawElementsBaseVertex( mode.asGLMode, indexCount, indexType.asGLType, 0L, baseVertex );
        shaderInstance.clear();
        getFormat().clearBufferState();
        VertexBuffer.unbind();
        VertexBuffer.unbindVertexArray();
    }

    public int getIndexCount()
    {
        return actualIndexCount;
    }

    @Override
    public void close()
    {
        super.close();
        if( DirectBuffers.ON_LINUX ) BufferUploader.reset(); // See comment on DirectBuffers.deleteBuffer.
    }
}
