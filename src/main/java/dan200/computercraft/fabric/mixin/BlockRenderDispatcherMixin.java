/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.util.WorldUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * Provides custom block breaking progress for modems, so it only applies to the current part.
 *
 * @see BlockRenderManager#renderDamage(BlockState, BlockPos, BlockRenderView, MatrixStack, VertexConsumer)
 */
@Mixin( BlockRenderManager.class )
public class BlockRenderDispatcherMixin
{
    @Shadow
    private final Random random;
    @Shadow
    private final BlockModels blockModelShaper;
    @Shadow
    private final BlockModelRenderer modelRenderer;

    public BlockRenderDispatcherMixin( Random random, BlockModels blockModelShaper, BlockModelRenderer modelRenderer )
    {
        this.random = random;
        this.blockModelShaper = blockModelShaper;
        this.modelRenderer = modelRenderer;
    }

    @Inject(
        method = "renderBreakingTexture",
        at = @At( "HEAD" ),
        cancellable = true,
        require = 0 // This isn't critical functionality, so don't worry if we can't apply it.
    )
    public void renderBlockDamage(
        BlockState state, BlockPos pos, BlockRenderView world, MatrixStack pose, VertexConsumer buffers,
        CallbackInfo info
    )
    {
        // Only apply to cables which have both a cable and modem
        if( state.getBlock() != Registry.ModBlocks.CABLE
            || !state.get( BlockCable.CABLE )
            || state.get( BlockCable.MODEM ) == CableModemVariant.None
        )
        {
            return;
        }

        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if( hit == null || hit.getType() != HitResult.Type.BLOCK ) return;
        BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();

        if( !hitPos.equals( pos ) ) return;

        info.cancel();
        BlockState newState = WorldUtil.isVecInside( CableShapes.getModemShape( state ), hit.getPos().subtract( pos.getX(), pos.getY(), pos.getZ() ) )
            ? state.getBlock().getDefaultState().with( BlockCable.MODEM, state.get( BlockCable.MODEM ) )
            : state.with( BlockCable.MODEM, CableModemVariant.None );

        BakedModel model = blockModelShaper.getModel( newState );
        long seed = newState.getRenderingSeed( pos );
        modelRenderer.render( world, model, newState, pos, pose, buffers, true, random, seed, OverlayTexture.DEFAULT_UV );
    }
}
