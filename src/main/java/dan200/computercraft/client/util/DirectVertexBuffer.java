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
import org.lwjgl.opengl.GL32C;

import java.nio.ByteBuffer;

/**
 * A specialised version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly and drawing from a
 * vertex offset. Note this version only support sequential indices.
 */
public class DirectVertexBuffer
{
    private int vertexArrayObjectId;
    private int vertexBufferId;

    private VertexFormat.Mode mode;
    private VertexFormat format;

    private RenderSystem.AutoStorageIndexBuffer indexBuffer;

    private ShaderInstance currentShader;
    private boolean vaoInitialised;

    public DirectVertexBuffer()
    {
        vertexArrayObjectId = GL32C.glGenVertexArrays();
        vertexBufferId = DirectBuffers.createBuffer();
    }

    public void upload( VertexFormat.Mode mode, VertexFormat format, ByteBuffer buffer )
    {
        DirectBuffers.setBufferData( GL32C.GL_ARRAY_BUFFER, vertexBufferId, buffer, GL32C.GL_STATIC_DRAW );

        this.format = format;
        this.mode = mode;
        indexBuffer = RenderSystem.getSequentialBuffer( mode );

        vaoInitialised = false;
    }

    /**
     * Call this method before calling {@link DirectVertexBuffer#draw(int, int)}. When done drawing out of the vbo, make
     * sure to call {@link DirectVertexBuffer#end()}!
     *
     * @param modelView  The modelView matrix.
     * @param projection The projection matrix.
     * @param shader     The shader to use for this series of draws.
     */
    public void begin( Matrix4f modelView, Matrix4f projection, ShaderInstance shader )
    {
        BufferUploader.reset();
        bindVertexArray();
        if( !vaoInitialised )
        {
            // Bind vertex buffer and setup vertex attribute pointers.
            GL32C.glBindBuffer( GL32C.GL_ARRAY_BUFFER, vertexBufferId );
            format.setupBufferState();
            GL32C.glBindBuffer( GL32C.GL_ARRAY_BUFFER, 0 );

            // Bind index buffer (this binding is kept in the VAO state!).
            GL32C.glBindBuffer( GL32C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.type().asGLType );

            vaoInitialised = true;
        }

        currentShader = shader;
        setupShader( shader, modelView, projection );
        shader.apply();
    }

    public void end()
    {
        currentShader.clear();
        currentShader = null;
        unbindVertexArray();
    }

    public void draw( int vertexCount, int baseVertex )
    {
        if( vertexCount == 0 || currentShader == null ) return;

        GL32C.glDrawElementsBaseVertex( mode.asGLMode, mode.indexCount( vertexCount ), indexBuffer.type().asGLType, 0L, baseVertex );
    }

    private void bindVertexArray()
    {
        GL32C.glBindVertexArray( vertexArrayObjectId );
    }

    private static void unbindVertexArray()
    {
        GL32C.glBindVertexArray( 0 );
    }

    public void close()
    {
        if( vertexBufferId > 0 )
        {
            DirectBuffers.deleteBuffer( GL32C.GL_ARRAY_BUFFER, vertexBufferId );
            vertexBufferId = 0;
        }
        if( vertexArrayObjectId > 0 )
        {
            GL32C.glDeleteVertexArrays( vertexArrayObjectId );
            vertexArrayObjectId = 0;
        }
    }

    private void setupShader( ShaderInstance shader, Matrix4f modelView, Matrix4f projection )
    {
        for( int i = 0; i < 12; ++i )
        {
            int j = RenderSystem.getShaderTexture( i );
            shader.setSampler( "Sampler" + i, j );
        }
        if( shader.MODEL_VIEW_MATRIX != null ) shader.MODEL_VIEW_MATRIX.set( modelView );
        if( shader.PROJECTION_MATRIX != null ) shader.PROJECTION_MATRIX.set( projection );
        if( shader.INVERSE_VIEW_ROTATION_MATRIX != null )
        {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set( RenderSystem.getInverseViewRotationMatrix() );
        }
        if( shader.COLOR_MODULATOR != null ) shader.COLOR_MODULATOR.set( RenderSystem.getShaderColor() );
        if( shader.FOG_START != null ) shader.FOG_START.set( RenderSystem.getShaderFogStart() );
        if( shader.FOG_END != null ) shader.FOG_END.set( RenderSystem.getShaderFogEnd() );
        if( shader.FOG_COLOR != null ) shader.FOG_COLOR.set( RenderSystem.getShaderFogColor() );
        if( shader.FOG_SHAPE != null ) shader.FOG_SHAPE.set( RenderSystem.getShaderFogShape().getIndex() );
        if( shader.TEXTURE_MATRIX != null ) shader.TEXTURE_MATRIX.set( RenderSystem.getTextureMatrix() );
        if( shader.GAME_TIME != null ) shader.GAME_TIME.set( RenderSystem.getShaderGameTime() );
        if( shader.SCREEN_SIZE != null )
        {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set( window.getWidth(), window.getHeight() );
        }
        if( shader.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP) )
        {
            shader.LINE_WIDTH.set( RenderSystem.getShaderLineWidth() );
        }
        RenderSystem.setupShaderLights( shader );
    }
}
