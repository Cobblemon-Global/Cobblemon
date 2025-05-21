package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.world.CobblemonStructureIDs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.Set;
import java.util.HashSet;

@Mixin(Structure.class)
public abstract class StructureMixin {

    @Shadow
    public abstract Optional<Structure.GenerationStub> findValidGenerationPoint(Structure.GenerationContext context);

    // Define a set of structures that should not spawn below Y=65
    private static final Set<ResourceLocation> RESTRICTED_STRUCTURES = new HashSet<>();

    static {
        RESTRICTED_STRUCTURES.add(CobblemonStructureIDs.STONJOURNER_HENGE);
        RESTRICTED_STRUCTURES.add(CobblemonStructureIDs.LUNA_HENGE);
        RESTRICTED_STRUCTURES.add(CobblemonStructureIDs.SOL_HENGE);
    }

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    public void generate(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkPos chunkPos, int references, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        Structure.GenerationContext generationContext = new Structure.GenerationContext(registryAccess, chunkGenerator, biomeSource, randomState, structureTemplateManager, seed, chunkPos, heightAccessor, validBiome);
        Optional<Structure.GenerationStub> optional = this.findValidGenerationPoint(generationContext);
        if (optional.isPresent()) {
            BlockPos position = optional.get().position();

            ResourceLocation structureKey = registryAccess.registryOrThrow(Registries.STRUCTURE).getKey((Structure) (Object) this);

            // Modify the 65 if you want to change the Y restriction
            if (RESTRICTED_STRUCTURES.contains(structureKey) && position.getY() < 65) {
                cir.setReturnValue(StructureStart.INVALID_START);
                return;
            }

            StructurePiecesBuilder structurePiecesBuilder = optional.get().getPiecesBuilder();
            StructureStart structureStart = new StructureStart((Structure) (Object) this, chunkPos, references, structurePiecesBuilder.build());
            if (structureStart.isValid()) {
                cir.setReturnValue(structureStart);
                return;
            }
        }

        cir.setReturnValue(StructureStart.INVALID_START);
    }
}