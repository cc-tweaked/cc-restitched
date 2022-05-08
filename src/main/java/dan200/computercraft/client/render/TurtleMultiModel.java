/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.api.client.TransformedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import javax.annotation.Nonnull;
import java.util.*;

@Environment( EnvType.CLIENT )
public class TurtleMultiModel implements BakedModel
{
    private final BakedModel baseModel;
    private final BakedModel overlayModel;
    private final AffineTransformation generalTransform;
    private final TransformedModel leftUpgradeModel;
    private final TransformedModel rightUpgradeModel;
    private List<BakedQuad> generalQuads = null;
    private final Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>( Direction.class );

    public TurtleMultiModel( BakedModel baseModel, BakedModel overlayModel, AffineTransformation generalTransform, TransformedModel leftUpgradeModel, TransformedModel rightUpgradeModel )
    {
        // Get the models
        this.baseModel = baseModel;
        this.overlayModel = overlayModel;
        this.leftUpgradeModel = leftUpgradeModel;
        this.rightUpgradeModel = rightUpgradeModel;
        this.generalTransform = generalTransform;
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction side, @Nonnull Random rand )
    {
        if( side != null )
        {
            if( !faceQuads.containsKey( side ) ) faceQuads.put( side, buildQuads( state, side, rand ) );
            return faceQuads.get( side );
        }
        else
        {
            if( generalQuads == null ) generalQuads = buildQuads( state, side, rand );
            return generalQuads;
        }
    }

    private List<BakedQuad> buildQuads( BlockState state, Direction side, Random rand )
    {
        ArrayList<BakedQuad> quads = new ArrayList<>();


        transformQuadsTo( quads, baseModel.getQuads( state, side, rand ), generalTransform );
        if( overlayModel != null )
        {
            transformQuadsTo( quads, overlayModel.getQuads( state, side, rand ), generalTransform );
        }
        if( leftUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = generalTransform.multiply( leftUpgradeModel.getMatrix() );
            transformQuadsTo( quads, leftUpgradeModel.getModel().getQuads( state, side, rand ), upgradeTransform );
        }
        if( rightUpgradeModel != null )
        {
            AffineTransformation upgradeTransform = generalTransform.multiply( rightUpgradeModel.getMatrix() );
            transformQuadsTo( quads, rightUpgradeModel.getModel().getQuads( state, side, rand ), upgradeTransform );
        }
        quads.trimToSize();
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth()
    {
        return baseModel.hasDepth();
    }

    @Override
    public boolean isBuiltin()
    {
        return baseModel.isBuiltin();
    }

    @Override
    public boolean isSideLit()
    {
        return baseModel.isSideLit();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getParticleSprite()
    {
        return baseModel.getParticleSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public ModelTransformation getTransformation()
    {
        return baseModel.getTransformation();
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides()
    {
        return ModelOverrideList.EMPTY;
    }

    private void transformQuadsTo( List<BakedQuad> output, List<BakedQuad> quads, AffineTransformation transform )
    {
        ModelTransformer.transformQuadsTo( output, quads, transform.getMatrix() );
    }
}
