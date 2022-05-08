/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.client.util.DirectVertexBuffer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.DirectionUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

public class TileEntityMonitorRenderer implements BlockEntityRenderer<TileMonitor>
{
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);
    private static ByteBuffer backingBuffer;

    private static final Matrix3f IDENTITY;

    static
    {
        Matrix3f identity = new Matrix3f();
        identity.loadIdentity();
        IDENTITY = identity;
    }

    public TileEntityMonitorRenderer( BlockEntityRendererFactory.Context context )
    {
    }

    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull MatrixStack transform, @Nonnull VertexConsumerProvider bufferSource, int lightmapCoord, int overlayLight )
    {
        // Render from the origin monitor
        ClientMonitor originTerminal = monitor.getClientMonitor();

        if( originTerminal == null ) return;
        TileMonitor origin = originTerminal.getOrigin();
        BlockPos monitorPos = monitor.getPos();

        // Ensure each monitor terminal is rendered only once. We allow rendering a specific tile
        // multiple times in a single frame to ensure compatibility with shaders which may run a
        // pass multiple times.
        long renderFrame = FrameInfo.getRenderFrame();
        if( originTerminal.lastRenderFrame == renderFrame && !monitorPos.equals( originTerminal.lastRenderPos ) )
        {
            return;
        }

        originTerminal.lastRenderFrame = renderFrame;
        originTerminal.lastRenderPos = monitorPos;

        BlockPos originPos = origin.getPos();

        // Determine orientation
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.asRotation();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.push();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.multiply( Vec3f.NEGATIVE_Y.getDegreesQuaternion( yaw ) );
        transform.multiply( Vec3f.POSITIVE_X.getDegreesQuaternion( pitch ) );
        transform.translate(
            -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
            0.5
        );
        double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Draw the contents
        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null )
        {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            double xScale = xSize / pixelWidth;
            double yScale = ySize / pixelHeight;
            transform.push();
            // Avoid PoseStack#scale to preserve normal matrix.
            transform.peek().getPositionMatrix().multiply( Matrix4f.scale( (float) xScale, (float) -yScale, 1.0f ) );

            renderTerminal( bufferSource, transform, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            transform.pop();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                FixedWidthFontRenderer.toVertexConsumer( transform, bufferSource.getBuffer( RenderTypes.MONITOR ) ),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2), FULL_BRIGHT_LIGHTMAP
            );
        }

        transform.pop();
    }

    private static void renderTerminal( @Nonnull VertexConsumerProvider bufferSource, MatrixStack transform, ClientMonitor monitor, float xMargin, float yMargin )
    {
        Terminal terminal = monitor.getTerminal();
        int width = terminal.getWidth(), height = terminal.getHeight();
        int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

        MonitorRenderer renderType = MonitorRenderer.current();
        boolean redraw = monitor.pollTerminalChanged();
        if( monitor.createBuffer( renderType ) ) redraw = true;

        switch( renderType )
        {
            case TBO:
            {
                if( redraw )
                {
                    var terminalBuffer = getBuffer( width * height * 3 );
                    MonitorTextureBufferShader.setTerminalData( terminalBuffer, terminal );
                    DirectBuffers.setBufferData( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer, terminalBuffer, GL20.GL_STATIC_DRAW );

                    var uniformBuffer = getBuffer( MonitorTextureBufferShader.UNIFORM_SIZE );
                    MonitorTextureBufferShader.setUniformData( uniformBuffer, terminal, !monitor.isColour() );
                    DirectBuffers.setBufferData( GL31.GL_UNIFORM_BUFFER, monitor.tboUniform, uniformBuffer, GL20.GL_STATIC_DRAW );
                }

                // Nobody knows what they're doing!
                int active = GlStateManager._getActiveTexture();
                RenderSystem.activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                RenderSystem.activeTexture( active );

                MonitorTextureBufferShader shader = RenderTypes.getMonitorTextureBufferShader();
                shader.setupUniform( monitor.tboUniform );

                Matrix4f matrix = transform.peek().getPositionMatrix();
                VertexConsumer buffer = bufferSource.getBuffer( RenderTypes.MONITOR_TBO );
                tboVertex( buffer, matrix, -xMargin, -yMargin );
                tboVertex( buffer, matrix, -xMargin, pixelHeight + yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, -yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, pixelHeight + yMargin );

                break;
            }

            case VBO:
            {
                var vbo = monitor.buffer;
                if( redraw )
                {
                    int vertexSize = RenderTypes.MONITOR.getVertexFormat().getVertexSize();
                    ByteBuffer buffer = getBuffer( DirectFixedWidthFontRenderer.getVertexCount( terminal ) * vertexSize );

                    // Draw the main terminal and store how many vertices it has.
                    DirectFixedWidthFontRenderer.drawTerminalWithoutCursor(
                        buffer, 0, 0, terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin
                    );
                    int termIndexes = buffer.position() / vertexSize;

                    // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                    // render n or n+1 quads and so toggle the cursor on and off.
                    DirectFixedWidthFontRenderer.drawCursor( buffer, 0, 0, terminal, !monitor.isColour() );

                    buffer.flip();

                    vbo.upload( termIndexes, RenderTypes.MONITOR.getDrawMode(), RenderTypes.MONITOR.getVertexFormat(), buffer );
                }

                Matrix3f popViewRotation = RenderSystem.getInverseViewRotationMatrix();
                RenderSystem.setInverseViewRotationMatrix( IDENTITY );

                bufferSource.getBuffer( RenderTypes.MONITOR );
                RenderTypes.MONITOR.startDrawing();

                var matrix = transform.peek().getPositionMatrix();
                if( MonitorRenderer.canvasModPresent )
                {
                    var modelViewMatrix = RenderSystem.getModelViewMatrix().copy();
                    modelViewMatrix.multiply( matrix );
                    matrix = modelViewMatrix;
                }

                vbo.drawWithShader(
                    matrix, RenderSystem.getProjectionMatrix(), RenderTypes.getMonitorShader(),
                    // As mentioned in the above comment, render the extra cursor quad if it is visible this frame. Each
                    // quad has an index count of 6.
                    FixedWidthFontRenderer.isCursorVisible( terminal ) && FrameInfo.getGlobalCursorBlink() ? vbo.getIndexCount() + 6 : vbo.getIndexCount()
                );

                RenderSystem.setInverseViewRotationMatrix( popViewRotation );
                break;
            }
        }

        // Force a flush of the buffer. WorldRenderer.updateCameraAndRender will "finish" all the built-in buffers
        // before calling renderer.finish, which means our TBO quad or depth blocker won't be rendered yet!
        bufferSource.getBuffer( RenderType.solid() );
    }

    private static void tboVertex( VertexConsumer builder, Matrix4f matrix, float x, float y )
    {
        // We encode position in the UV, as that's not transformed by the matrix.
        builder.vertex( matrix, x, y, 0 ).texture( x, y ).next();
    }

    @Nonnull
    private static ByteBuffer getBuffer( int capacity )
    {

        ByteBuffer buffer = backingBuffer;
        if( buffer == null || buffer.capacity() < capacity )
        {
            buffer = backingBuffer = buffer == null ? GlAllocationUtils.allocateByteBuffer( capacity ) : GlAllocationUtils.resizeByteBuffer( buffer, capacity );
        }

        buffer.clear();
        return buffer;
    }

    @Override
    public int getRenderDistance()
    {
        return ComputerCraft.monitorDistance;
    }
}
