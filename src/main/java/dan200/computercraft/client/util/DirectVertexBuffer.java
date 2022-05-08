/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Matrix4f;

/**
 * A version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly.
 *
 * This should probably be its own class (rather than subclassing), but I need access to {@link VertexBuffer#setShader}.
 */
public class DirectVertexBuffer extends VertexBuffer
{
    private int actualIndexCount;

    public DirectVertexBuffer()
    {
        if( DirectBuffers.HAS_DSA )
        {
            RenderSystem.glDeleteBuffers( vertexBufferId );
            if( DirectBuffers.ON_LINUX ) BufferRenderer.unbindAll(); // See comment on DirectBuffers.deleteBuffer.
            vertexBufferId = GL45C.glCreateBuffers();
        }
    }

    public void upload( int vertexCount, VertexFormat.DrawMode mode, VertexFormat format, ByteBuffer buffer )
    {
        RenderSystem.assertOnRenderThread();

        DirectBuffers.setBufferData( GL15.GL_ARRAY_BUFFER, vertexBufferId, buffer, GL15.GL_STATIC_DRAW );

        this.vertexFormat = format;
        this.drawMode = mode;
        actualIndexCount = vertexCount = mode.getSize( vertexCount );
        elementFormat = VertexFormat.IntType.SHORT;
        usesTexture = true;
    }

    public void drawWithShader( Matrix4f modelView, Matrix4f projection, Shader shader, int indexCount )
    {
        this.vertexCount = indexCount;
        setShader( modelView, projection, shader );
        this.vertexCount = actualIndexCount;
    }

    public int getIndexCount()
    {
        return actualIndexCount;
    }

    @Override
    public void close()
    {
        super.close();
        if( DirectBuffers.ON_LINUX ) BufferRenderer.unbindAll(); // See comment on DirectBuffers.deleteBuffer.
    }
}
