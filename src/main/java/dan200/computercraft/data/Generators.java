/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class Generators implements DataGeneratorEntrypoint
{
    @Override
    public void onInitializeDataGenerator( FabricDataGenerator generator )
    {
        generator.addProvider( new RecipeGenerator( generator ) );
        generator.addProvider( new LootTableGenerator( generator ) );
        generator.addProvider( new BlockModelProvider( generator ) );

        BlockTagsGenerator blockTags = new BlockTagsGenerator( generator );
        generator.addProvider( blockTags );
        generator.addProvider( new ItemTagsGenerator( generator, blockTags ) );
    }
}
