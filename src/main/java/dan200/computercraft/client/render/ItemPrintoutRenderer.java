/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.shared.media.items.ItemPrintout;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import static dan200.computercraft.client.render.PrintoutRenderer.*;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINES_PER_PAGE;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINE_MAX_LENGTH;

/**
 * Emulates map and item-frame rendering for printouts.
 */
public final class ItemPrintoutRenderer extends ItemMapLikeRenderer
{
    public static final ItemPrintoutRenderer INSTANCE = new ItemPrintoutRenderer();

    private ItemPrintoutRenderer()
    {
    }

    public boolean renderInFrame( MatrixStack transform, VertexConsumerProvider renderer, ItemStack stack, int light )
    {
        if( !(stack.getItem() instanceof ItemPrintout) ) return false;

        // Move a little bit forward to ensure we're not clipping with the frame
        transform.translate( 0.0f, 0.0f, -0.001f );
        transform.multiply( Vec3f.POSITIVE_Z.getDegreesQuaternion( 180f ) );
        // Avoid PoseStack#scale to preserve normal matrix, and fix the normals ourselves.
        transform.peek().getPositionMatrix().multiply( Matrix4f.scale( 0.95f, 0.95f, -0.95f ) );
        transform.peek().getNormalMatrix().multiply( -1.0f );

        //transform.last().normal().mul( -1.0f );
        transform.translate( -0.5f, -0.5f, 0.0f );

        drawPrintout( transform, renderer, stack, light );

        return true;
    }

    @Override
    protected void renderItem( MatrixStack transform, VertexConsumerProvider renderer, ItemStack stack, int light )
    {
        transform.multiply( Vec3f.POSITIVE_X.getDegreesQuaternion( 180f ) );
        // Avoid PoseStack#scale to preserve normal matrix, and fix the normals ourselves.
        transform.peek().getPositionMatrix().multiply( Matrix4f.scale( 0.42f, 0.42f, -0.42f ) );
        transform.peek().getNormalMatrix().multiply( -1.0f );
        transform.translate( -0.5f, -0.48f, 0.0f );

        drawPrintout( transform, renderer, stack, light );
    }

    private static void drawPrintout( MatrixStack transform, VertexConsumerProvider renderer, ItemStack stack, int light )
    {
        int pages = ItemPrintout.getPageCount( stack );
        boolean book = ((ItemPrintout) stack.getItem()).getType() == ItemPrintout.Type.BOOK;

        double width = LINE_MAX_LENGTH * FONT_WIDTH + X_TEXT_MARGIN * 2;
        double height = LINES_PER_PAGE * FONT_HEIGHT + Y_TEXT_MARGIN * 2;

        // Non-books will be left aligned
        if( !book ) width += offsetAt( pages - 1 );

        double visualWidth = width, visualHeight = height;

        // Meanwhile books will be centred
        if( book )
        {
            visualWidth += 2 * COVER_SIZE + 2 * offsetAt( pages );
            visualHeight += 2 * COVER_SIZE;
        }

        double max = Math.max( visualHeight, visualWidth );

        // Scale the printout to fit correctly.
        float scale = (float) (1.0 / max);
        transform.scale( scale, scale, scale );
        transform.translate( (max - width) / 2.0, (max - height) / 2.0, 0.0 );

        drawBorder(
            transform, renderer.getBuffer( RenderTypes.ITEM_PRINTOUT_BACKGROUND ),
            0, 0, -0.01f, 0, pages, book, light
        );
        drawText(
            transform, renderer.getBuffer( RenderTypes.ITEM_PRINTOUT_TEXT ),
            X_TEXT_MARGIN, Y_TEXT_MARGIN, 0, light, ItemPrintout.getText( stack ), ItemPrintout.getColours( stack )
        );
    }
}
