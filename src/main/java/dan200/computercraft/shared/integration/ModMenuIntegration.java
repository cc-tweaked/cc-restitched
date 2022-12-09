/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.util.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

// A poor mod menu integration just for testing the monitor rendering changes we've been making :)

@Environment( EnvType.CLIENT )
public class ModMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return parent -> {

            var monitorRendererOption = new OptionInstance<>(
                "Monitor Renderer",
                OptionInstance.cachedConstantTooltip( Component.nullToEmpty( rewrapComment( Config.clientConfig.getComment( "monitor_renderer" ) ) ) ),
                ( c, value ) -> Component.literal( value.name() ),
                new OptionInstance.Enum<>( Arrays.asList( MonitorRenderer.values() ), Codec.unit( MonitorRenderer.BEST ) ),
                ComputerCraft.monitorRenderer,
                renderer -> Config.clientConfig.set( "monitor_renderer", renderer )
            );

            return new OptionsSubScreen( parent, Minecraft.getInstance().options, Component.literal( "Computer Craft" ) )
            {
                private OptionsList list;

                @Override
                protected void init()
                {
                    this.list = new OptionsList( this.minecraft, this.width, this.height, 32, this.height - 32, 25 );
                    this.list.addBig( monitorRendererOption );

                    this.addWidget( this.list );

                    this.addRenderableWidget( Button.builder( CommonComponents.GUI_DONE, button -> {
                        this.minecraft.setScreen( this.lastScreen );
                    } ).width( this.width / 2 - 100 ).pos( 200, 20 ).build() );
                }

                @Override
                public void removed()
                {
                    Config.clientSpec.correct( Config.clientConfig );
                    Config.sync();
                    Config.save();
                }

                @Override
                public void render( PoseStack poseStack, int i, int j, float f )
                {
                    this.renderBackground( poseStack );
                    this.list.render( poseStack, i, j, f );
                    drawCenteredString( poseStack, this.font, this.title, this.width / 2, 20, 16777215 );
                    super.render( poseStack, i, j, f );
                    //List<FormattedCharSequence> list = tooltipAt( this.list, i, j );
                    //this.renderTooltip( poseStack, list, i, j );
                }
            };
        };
    }

    private static String rewrapComment( String comment )
    {
        String[] words = comment.strip().replaceAll( "[\r\n]", "" ).split( " " );

        StringBuilder builder = new StringBuilder();
        int lineLength = 0;
        for( String word : words )
        {
            int wordLength = word.length();

            if( lineLength + wordLength + 1 > 50 )
            {
                builder.append( "\n" );
                lineLength = 0;
                builder.append( word );
                lineLength += wordLength;
            }
            else
            {
                if( builder.length() == 0 )
                {
                    builder.append( word );
                    lineLength += wordLength;
                }
                else
                {
                    builder.append( " " );
                    builder.append( word );
                    lineLength += wordLength + 1;
                }
            }
        }

        return new String( builder );
    }
}
