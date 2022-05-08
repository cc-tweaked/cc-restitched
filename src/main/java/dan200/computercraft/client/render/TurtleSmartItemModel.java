/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.render.TurtleSmartItemModel.TurtleModelCombination;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Environment( EnvType.CLIENT )
public class TurtleSmartItemModel implements BakedModel
{
    private static final AffineTransformation identity, flip;

    static
    {
        MatrixStack stack = new MatrixStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = AffineTransformation.identity();
        flip = new AffineTransformation( stack.peek().getPositionMatrix() );
    }

    private static record TurtleModelCombination(
        boolean colour,
        ITurtleUpgrade leftUpgrade,
        ITurtleUpgrade rightUpgrade,
        ResourceLocation overlay,
        boolean christmas,
        boolean flip
    )
    {
    }

    private final BakedModel familyModel;
    private final BakedModel colourModel;

    private final HashMap<TurtleModelCombination, BakedModel> cachedModels = new HashMap<>();
    private final ModelOverrideList overrides;

    public TurtleSmartItemModel( BakedModel familyModel, BakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        // this actually works I think, trust me
        overrides = new ModelOverrideList( null, null, null, Collections.emptyList() )
        {
            @Nonnull
            @Override
            public BakedModel apply( @Nonnull BakedModel originalModel, @Nonnull ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int random )
            {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.LEFT );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.RIGHT );
                Identifier overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.CHRISTMAS;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                BakedModel model = cachedModels.get( combo );
                if( model == null ) cachedModels.put( combo, model = buildModel( combo ) );
                return model;
            }
        };
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides()
    {
        return overrides;
    }

    private BakedModel buildModel( TurtleModelCombination combo )
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        BakedModelManager modelManager = mc.getItemRenderer().getModels().getModelManager();
        ModelIdentifier overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.overlay, combo.christmas );

        BakedModel baseModel = combo.colour ? colourModel : familyModel;
        BakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        AffineTransformation transform = combo.flip ? flip : identity;
        TransformedModel leftModel = combo.leftUpgrade != null ? combo.leftUpgrade.getModel( null, TurtleSide.LEFT ) : null;
        TransformedModel rightModel = combo.rightUpgrade != null ? combo.rightUpgrade.getModel( null, TurtleSide.RIGHT ) : null;
        return new TurtleMultiModel( baseModel, overlayModel, transform, leftModel, rightModel );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand )
    {
        return familyModel.getQuads( state, facing, rand );
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return familyModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth()
    {
        return familyModel.hasDepth();
    }

    @Override
    public boolean isBuiltin()
    {
        return familyModel.isBuiltin();
    }

    @Override
    public boolean isSideLit()
    {
        return familyModel.isSideLit();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getParticleSprite()
    {
        return familyModel.getParticleSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public ModelTransformation getTransformation()
    {
        return familyModel.getTransformation();
    }

}
