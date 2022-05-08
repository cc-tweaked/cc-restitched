/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.fabric.events.CustomClientEvents;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.Config;
import dan200.computercraft.shared.util.PauseAwareTimer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Environment( EnvType.CLIENT )
public final class ComputerCraftProxyClient implements ClientModInitializer
{

    private static void initEvents()
    {
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register( ( blockEntity, world ) -> {
            if( blockEntity instanceof TileGeneric )
            {
                ((TileGeneric) blockEntity).onChunkUnloaded();
            }
        } );

        ServerTickEvents.START_SERVER_TICK.register( server -> PauseAwareTimer.tick() );

        CustomClientEvents.CLIENT_UNLOAD_WORLD_EVENT.register( () -> {
            SpeakerManager.reset();
            ClientMonitor.destroyAll();
        } );

        CustomClientEvents.PLAY_STREAMING_AUDIO_EVENT.register( SpeakerManager::playStreaming );

        // Config
        ClientLifecycleEvents.CLIENT_STARTED.register( Config::clientStarted );
    }

    @Override
    public void onInitializeClient()
    {
        FrameInfo.init();
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.TURTLE_NORMAL, RenderLayer.getTranslucent() );
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.TURTLE_ADVANCED, RenderLayer.getTranslucent() );
        // Monitors' textures have transparent fronts and so count as cutouts.
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.MONITOR_NORMAL, RenderLayer.getCutout() );
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.MONITOR_ADVANCED, RenderLayer.getCutout() );

        // Setup TESRs
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.MONITOR_NORMAL, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.MONITOR_ADVANCED, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.TURTLE_NORMAL, TileEntityTurtleRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.TURTLE_ADVANCED, TileEntityTurtleRenderer::new );

        ClientSpriteRegistryCallback.event( PlayerScreenHandler.BLOCK_ATLAS_TEXTURE )
            .register( ClientRegistry::onTextureStitchEvent );
        ModelLoadingRegistry.INSTANCE.registerModelProvider( ClientRegistry::onModelBakeEvent );
        ModelLoadingRegistry.INSTANCE.registerResourceProvider( loader -> ( name, context ) -> TurtleModelLoader.INSTANCE.accepts( name ) ?
            TurtleModelLoader.INSTANCE.loadModel(
                name ) : null );

        registerItemProperty( "state",
            ( stack, world, player, integer ) -> ItemPocketComputer.getState( stack )
                .ordinal(),
            () -> Registry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> Registry.ModItems.POCKET_COMPUTER_ADVANCED );
        registerItemProperty( "coloured",
            ( stack, world, player, integer ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
            () -> Registry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> Registry.ModItems.POCKET_COMPUTER_ADVANCED );
        ClientRegistry.onItemColours();

        initEvents();
    }

    // My IDE doesn't think so, but we do actually need these generics.
    private static void registerContainers()
    {
        HandledScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.COMPUTER, GuiComputer::create );
        HandledScreens.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER, GuiComputer::createPocket );
        HandledScreens.<ContainerComputerBase, NoTermComputerScreen<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER_NO_TERM, NoTermComputerScreen::new );
        HandledScreens.<ContainerTurtle, GuiTurtle>register( Registry.ModContainers.TURTLE, GuiTurtle::new );

        HandledScreens.<ContainerPrinter, GuiPrinter>register( Registry.ModContainers.PRINTER, GuiPrinter::new );
        HandledScreens.<ContainerDiskDrive, GuiDiskDrive>register( Registry.ModContainers.DISK_DRIVE, GuiDiskDrive::new );
        HandledScreens.<ContainerHeldItem, GuiPrintout>register( Registry.ModContainers.PRINTOUT, GuiPrintout::new );

        HandledScreens.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( Registry.ModContainers.VIEW_COMPUTER,
            GuiComputer::createView );
    }

    @SafeVarargs
    private static void registerItemProperty( String name, UnclampedModelPredicateProvider getter, Supplier<? extends Item>... items )
    {
        Identifier id = new Identifier( ComputerCraft.MOD_ID, name );
        // Terrible hack, but some of our properties return values greater than 1, so we don't want to clamp.
        var unclampedGetter = new UnclampedModelPredicateProvider()
        {
            @Override
            @Deprecated
            public float call( @NotNull ItemStack itemStack, @Nullable ClientWorld clientLevel, @Nullable LivingEntity livingEntity, int i )
            {
                return getter.unclampedCall( itemStack, clientLevel, livingEntity, i );
            }

            @Override
            public float unclampedCall( ItemStack itemStack, @Nullable ClientWorld clientLevel, @Nullable LivingEntity livingEntity, int i )
            {
                return getter.unclampedCall( itemStack, clientLevel, livingEntity, i );
            }
        };
        for( Supplier<? extends Item> item : items )
        {
            ModelPredicateProviderRegistry.register( item.get(), id, unclampedGetter );
        }
    }
}
