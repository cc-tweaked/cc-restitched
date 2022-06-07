/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.integration.IrisCompat;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
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
        identity.setIdentity();
        IDENTITY = identity;
    }

    public TileEntityMonitorRenderer( BlockEntityRendererProvider.Context context )
    {
    }

    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull PoseStack transform, @Nonnull MultiBufferSource bufferSource, int lightmapCoord, int overlayLight )
    {
        // Render from the origin monitor
        ClientMonitor originTerminal = monitor.getClientMonitor();

        if( originTerminal == null ) return;
        TileMonitor origin = originTerminal.getOrigin();
        BlockPos monitorPos = monitor.getBlockPos();

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

        BlockPos originPos = origin.getBlockPos();

        // Determine orientation
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.toYRot();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.pushPose();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.mulPose( Vector3f.YN.rotationDegrees( yaw ) );
        transform.mulPose( Vector3f.XP.rotationDegrees( pitch ) );
        transform.translate(
            -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
            0.5
        );
        double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Draw the contents
        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null && !IrisCompat.INSTANCE.isRenderingShadowPass() )
        {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            double xScale = xSize / pixelWidth;
            double yScale = ySize / pixelHeight;
            transform.pushPose();
            // Avoid PoseStack#scale to preserve normal matrix.
            transform.last().pose().multiply( Matrix4f.createScaleMatrix( (float) xScale, (float) -yScale, 1.0f ) );

            renderTerminal( bufferSource, transform, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            transform.popPose();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                FixedWidthFontRenderer.toVertexConsumer( transform, bufferSource.getBuffer( RenderTypes.MONITOR ) ),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        transform.popPose();
    }

    private static void renderTerminal( @Nonnull MultiBufferSource bufferSource, PoseStack transform, ClientMonitor monitor, float xMargin, float yMargin )
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

                Matrix4f matrix = transform.last().pose();
                VertexConsumer buffer = bufferSource.getBuffer( RenderTypes.MONITOR_TBO );
                tboVertex( buffer, matrix, -xMargin, -yMargin );
                tboVertex( buffer, matrix, -xMargin, pixelHeight + yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, -yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, pixelHeight + yMargin );

                // Force a flush of the buffer. WorldRenderer.updateCameraAndRender will "finish" all the built-in buffers
                // before calling renderer.finish, which means our TBO quad won't be rendered yet!
                bufferSource.getBuffer( RenderType.solid() );
                break;
            }

            case VBO:
            {
                var vbo = monitor.buffer;
                if( redraw )
                {
                    // Draw the background contents of the terminal into one vbo.
                    var sink = IrisCompat.INSTANCE.getVertexSink(
                        DirectFixedWidthFontRenderer.getVertexCount( terminal ),
                        TileEntityMonitorRenderer::getBuffer
                    );
                    int vertexSize = sink.getFormat().getVertexSize();
                    var buffer = sink.buffer();

                    DirectFixedWidthFontRenderer.drawTerminalBackground( sink, 0, 0, terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin );
                    int backgroundBytes = buffer.position();
                    DirectFixedWidthFontRenderer.drawTerminalForeground( sink, 0, 0, terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin );
                    int termVertices = buffer.position() / vertexSize;
                    monitor.backgroundVertexCount = backgroundBytes / vertexSize;
                    monitor.foregroundVertexCount = termVertices - monitor.backgroundVertexCount;

                    // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                    // render n or n+1 quads and so toggle the cursor on and off.
                    DirectFixedWidthFontRenderer.drawCursor( sink, 0, 0, terminal, !monitor.isColour() );

                    buffer.flip();
                    vbo.upload( buffer.position() / vertexSize, RenderTypes.MONITOR.mode(), sink.getFormat(), buffer );
                }

                // TODO Figure out how to setup the inverse view rotation matrix properly.
                // This weird hack makes the VBO geometry interact with fog at the correct distance, although the fog
                // shape doesn't look quite right.
                Matrix3f popViewRotation = RenderSystem.getInverseViewRotationMatrix();
                RenderSystem.setInverseViewRotationMatrix( IDENTITY );

                var matrix = transform.last().pose();

                // For some reason, if Canvas is present we need to do this matrix multiplication ourselves.
                if( MonitorRenderer.canvasModPresent )
                {
                    var modelViewMatrix = RenderSystem.getModelViewMatrix().copy();
                    modelViewMatrix.multiply( matrix );
                    matrix = modelViewMatrix;
                }

                // Setup state
                RenderTypes.MONITOR.setupRenderState();
                vbo.begin( matrix, RenderSystem.getProjectionMatrix(), RenderTypes.getMonitorShader() );

                // Render background geometry
                vbo.draw( monitor.backgroundVertexCount, 0 );

                // Render foreground geometry with glPolygonOffset enabled.
                GL11.glPolygonOffset( -1.0f, -10.0f );
                GL11.glEnable( GL11.GL_POLYGON_OFFSET_FILL );
                vbo.draw(
                    // As mentioned in an above comment, render the extra cursor quad if it is visible this frame.
                    FixedWidthFontRenderer.isCursorVisible( terminal ) && FrameInfo.getGlobalCursorBlink() ? monitor.foregroundVertexCount + 4 : monitor.foregroundVertexCount,
                    monitor.backgroundVertexCount
                );

                // Clear state
                GL11.glPolygonOffset( 0.0f, -0.0f );
                GL11.glDisable( GL11.GL_POLYGON_OFFSET_FILL );
                vbo.end();
                RenderTypes.MONITOR.clearRenderState();

                RenderSystem.setInverseViewRotationMatrix( popViewRotation );
                break;
            }
        }
    }

    private static void tboVertex( VertexConsumer builder, Matrix4f matrix, float x, float y )
    {
        // We encode position in the UV, as that's not transformed by the matrix.
        builder.vertex( matrix, x, y, 0 ).uv( x, y ).endVertex();
    }

    @Nonnull
    private static ByteBuffer getBuffer( int capacity )
    {

        ByteBuffer buffer = backingBuffer;
        if( buffer == null || buffer.capacity() < capacity )
        {
            buffer = backingBuffer = buffer == null ? MemoryTracker.create( capacity ) : MemoryTracker.resize( buffer, capacity );
        }

        buffer.clear();
        return buffer;
    }

    @Override
    public int getViewDistance()
    {
        return ComputerCraft.monitorDistance;
    }
}
