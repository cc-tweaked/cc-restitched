/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import dan200.computercraft.shared.Registry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( ServerPlayerInteractionManager.class )
public class MixinServerPlayerGameMode
{
    @Inject(
        at = @At( value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;", ordinal = 0 ),
        method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        cancellable = true
    )
    private void useItemOn( ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir )
    {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState( pos );
        if( player.getMainHandStack().getItem() == Registry.ModItems.DISK && state.getBlock() == Registry.ModBlocks.DISK_DRIVE )
        {
            ActionResult actionResult = state.onUse( world, player, hand, hitResult );
            if( actionResult.isAccepted() )
            {
                Criteria.ITEM_USED_ON_BLOCK.trigger( player, pos, stack );
                cir.setReturnValue( actionResult );
            }
        }
    }
}
