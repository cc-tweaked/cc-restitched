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
        FabricDataGenerator.Pack pack = generator.createPack();

        pack.addProvider( RecipeGenerator::new );
        pack.addProvider( LootTableGenerator::new );
        pack.addProvider( BlockModelProvider::new );

        pack.addProvider( BlockTagsGenerator::new );
        pack.addProvider( ItemTagsGenerator::new );
    }
}
