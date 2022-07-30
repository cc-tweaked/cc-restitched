/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import com.google.common.primitives.Ints;
import dan200.computercraft.shared.command.text.ChatHelpers;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.command.text.TableFormatter;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

public class ClientTableFormatter implements TableFormatter
{
    private static final byte[] COMPUTER_CRAFT_BYTES = "ComputerCraft".getBytes( StandardCharsets.UTF_8 );

    public static final ClientTableFormatter INSTANCE = new ClientTableFormatter();

    private static final Int2IntOpenHashMap lastHeights = new Int2IntOpenHashMap();

    private static Font renderer()
    {
        return Minecraft.getInstance().font;
    }

    @Override
    @Nullable
    public Component getPadding( Component component, int width )
    {
        int extraWidth = width - getWidth( component );
        if( extraWidth <= 0 ) return null;

        Font renderer = renderer();

        float spaceWidth = renderer.width( " " );
        int spaces = Mth.floor( extraWidth / spaceWidth );
        int extra = extraWidth - (int) (spaces * spaceWidth);

        return ChatHelpers.coloured( StringUtils.repeat( ' ', spaces ) + StringUtils.repeat( (char) 712, extra ), ChatFormatting.GRAY );
    }

    @Override
    public int getColumnPadding()
    {
        return 3;
    }

    @Override
    public int getWidth( Component component )
    {
        return renderer().width( component );
    }

    @Override
    public void writeLine( int id, Component component )
    {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.getChat();

        // TODO: Trim the text if it goes over the allowed length
        // int maxWidth = MathHelper.floor( chat.getChatWidth() / chat.getScale() );
        // List<ITextProperties> list = RenderComponentsUtil.wrapComponents( component, maxWidth, mc.fontRenderer );
        // if( !list.isEmpty() ) chat.printChatMessageWithOptionalDeletion( list.get( 0 ), id );

        chat.addMessage( component, createMessageSignature( id ), GuiMessageTag.system() );
    }

    @Override
    public int display( TableBuilder table )
    {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();

        int lastHeight = lastHeights.get( table.getId() );

        int height = TableFormatter.super.display( table );
        lastHeights.put( table.getId(), height );

        for( int i = height; i < lastHeight; i++ ) chat.deleteMessage( createMessageSignature( i + table.getId() ) );
        return height;
    }

    public MessageSignature createMessageSignature( int id )
    {
        ByteList list = new ByteArrayList();

        list.addElements( 0, Ints.toByteArray( id ) );
        list.addElements( list.size(), COMPUTER_CRAFT_BYTES );

        return new MessageSignature( list.toByteArray() );
    }
}
