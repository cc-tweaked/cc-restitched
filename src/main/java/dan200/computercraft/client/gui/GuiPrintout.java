/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.media.items.ItemPrintout;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

public class GuiPrintout extends HandledScreen<ContainerHeldItem>
{
    private final boolean book;
    private final int pages;
    private final TextBuffer[] text;
    private final TextBuffer[] colours;
    private int page;

    public GuiPrintout( ContainerHeldItem container, PlayerInventory player, Text title )
    {
        super( container, player, title );

        backgroundHeight = Y_SIZE;

        String[] text = ItemPrintout.getText( container.getStack() );
        this.text = new TextBuffer[text.length];
        for( int i = 0; i < this.text.length; i++ ) this.text[i] = new TextBuffer( text[i] );

        String[] colours = ItemPrintout.getColours( container.getStack() );
        this.colours = new TextBuffer[colours.length];
        for( int i = 0; i < this.colours.length; i++ ) this.colours[i] = new TextBuffer( colours[i] );

        page = 0;
        pages = Math.max( this.text.length / ItemPrintout.LINES_PER_PAGE, 1 );
        book = ((ItemPrintout) container.getStack().getItem()).getType() == ItemPrintout.Type.BOOK;
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Check for key presses.
        if( key == GLFW.GLFW_KEY_RIGHT )
        {
            if( page < pages - 1 ) page++;
            return true;
        }

        if( key == GLFW.GLFW_KEY_LEFT )
        {
            if( page > 0 ) page--;
            return true;
        }

        // Otherwise, default to parent function
        if( super.keyPressed( key, scancode, modifiers ) ) return true;

        return false;
    }

    @Override
    public boolean mouseScrolled( double x, double y, double delta )
    {
        // Check for key presses.
        if( delta < 0 )
        {
            // Scroll up goes to the next page
            if( page < pages - 1 ) page++;
            return true;
        }

        if( delta > 0 )
        {
            // Scroll down goes to the previous page
            if( page > 0 ) page--;
            return true;
        }

        // Otherwise, default to parent function
        if( super.mouseScrolled( x, y, delta ) ) return true;

        return false;
    }

    @Override
    protected void drawBackground( @Nonnull MatrixStack transform, float partialTicks, int mouseX, int mouseY )
    {
        // Draw the printout
        VertexConsumerProvider.Immediate renderer = VertexConsumerProvider.immediate( Tessellator.getInstance().getBuffer() );

        VertexConsumer borderBuffer = renderer.getBuffer( RenderTypes.GUI_PRINTOUT_BACKGROUND );
        drawBorder( transform, borderBuffer, x, y, getZOffset(), page, pages, book, FULL_BRIGHT_LIGHTMAP );

        VertexConsumer textBuffer = renderer.getBuffer( RenderTypes.GUI_PRINTOUT_TEXT );
        drawText( transform, textBuffer, x + X_TEXT_MARGIN, y + Y_TEXT_MARGIN, ItemPrintout.LINES_PER_PAGE * page, FULL_BRIGHT_LIGHTMAP, text, colours );

        renderer.draw();
    }

    @Override
    public void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        // We must take the background further back in order to not overlap with our printed pages.
        setZOffset( getZOffset() - 1 );
        renderBackground( stack );
        setZOffset( getZOffset() + 1 );

        super.render( stack, mouseX, mouseY, partialTicks );
    }

    @Override
    protected void drawForeground( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }
}
