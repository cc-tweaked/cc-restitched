/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.client;

import com.mojang.math.Transformation;
import dan200.computercraft.impl.client.ClientPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * A model to render, combined with a transformation matrix to apply.
 */
public final class TransformedModel {
    private final BakedModel model;
    private final Transformation matrix;

    public TransformedModel(BakedModel model, Transformation matrix) {
        this.model = Objects.requireNonNull(model);
        this.matrix = Objects.requireNonNull(matrix);
    }

    public TransformedModel(BakedModel model) {
        this.model = Objects.requireNonNull(model);
        matrix = Transformation.identity();
    }

    public static TransformedModel of(ModelResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(modelManager.getModel(location));
    }

    public static TransformedModel of(ResourceLocation location) {
        var modelManager = Minecraft.getInstance().getModelManager();
        return new TransformedModel(ClientPlatformHelper.get().getModel(modelManager, location));
    }

    public static TransformedModel of(ItemStack item, Transformation transform) {
        var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        return new TransformedModel(model, transform);
    }

    public BakedModel getModel() {
        return model;
    }

    public Transformation getMatrix() {
        return matrix;
    }
}
