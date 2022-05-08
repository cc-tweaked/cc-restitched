/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin( HeldItemRenderer.class )
public interface ItemInHandRendererAccess
{
    @Invoker
    float callCalculateMapTilt( float tickDelta );

    @Invoker
    void callRenderMapHand( MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm );

    @Invoker
    void callRenderPlayerArm( MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm );
}
