package com.xenchinryu7.mcagenticlabs.structure;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Loads Minecraft structure templates from NBT files for sequential block-by-block placement
 */
public class StructureTemplateLoader {
    
    public static class TemplateBlock {
        public final BlockPos relativePos;
        public final BlockState blockState;
        
        public TemplateBlock(BlockPos relativePos, BlockState blockState) {
            this.relativePos = relativePos;
            this.blockState = blockState;
        }
    }
    
    public static class LoadedTemplate {
        public final String name;
        public final List<TemplateBlock> blocks;
        public final int width;
        public final int height;
        public final int depth;
        
        public LoadedTemplate(String name, List<TemplateBlock> blocks, int width, int height, int depth) {
            this.name = name;
            this.blocks = blocks;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }
        
    /**
     * Load a structure from an NBT file (either custom or Minecraft's native format)
     */
    public static LoadedTemplate loadFromNBT(ServerLevel level, String structureName) {        File structuresDir = new File(System.getProperty("user.dir"), "structures");
        AgentsMod.LOGGER.info("Structures directory: {}", structuresDir.getAbsolutePath());
        
        File exactMatch = new File(structuresDir, structureName + ".nbt");
        if (exactMatch.exists()) {
            AgentsMod.LOGGER.info("Found structure (exact match): {}", exactMatch.getName());
            return loadFromFile(exactMatch, structureName);
        }
        
        String withSpaces = structureName.replaceAll("(\\w)(\\p{Upper})", "$1 $2").toLowerCase();
        File spacedMatch = new File(structuresDir, withSpaces + ".nbt");
        if (spacedMatch.exists()) {
            AgentsMod.LOGGER.info("Found structure (spaced match): {}", spacedMatch.getName());
            return loadFromFile(spacedMatch, structureName);
        }
        
        if (structuresDir.exists() && structuresDir.isDirectory()) {
            File[] files = structuresDir.listFiles((dir, name) -> {
                if (!name.endsWith(".nbt")) return false;
                
                String nameWithoutExt = name.substring(0, name.length() - 4);
                
                // Normalize both strings: lowercase, remove spaces and underscores
                String normalizedFile = nameWithoutExt.toLowerCase().replace(" ", "").replace("_", "");
                String normalizedSearch = structureName.toLowerCase().replace(" ", "").replace("_", "");
                
                return normalizedFile.equals(normalizedSearch);
            });
            
            if (files != null && files.length > 0) {
                AgentsMod.LOGGER.info("Found structure (fuzzy match): {}", files[0].getName());
                return loadFromFile(files[0], structureName);
            }
        }
        
        try {
            ResourceLocation identifier = ResourceLocation.parse(structureName);
            var templateManager = level.getStructureManager();
            var template = templateManager.get(identifier);
            
            if (template.isPresent()) {                return loadFromMinecraftTemplate(template.get(), structureName);
            }
        } catch (Exception e) {        }
        
        AgentsMod.LOGGER.warn("Structure '{}' not found. Available structures: {}", 
            structureName, getAvailableStructures());
        return null;
    }
    
    /**
     * Load from a custom NBT file
     */
    private static LoadedTemplate loadFromFile(File file, String name) {
        try (InputStream inputStream = new FileInputStream(file)) {
            CompoundTag nbt = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            return parseNBTStructure(nbt, name);
        } catch (IOException e) {
            AgentsMod.LOGGER.error("Failed to load structure from file: {}", file, e);
            return null;
        }
    }
    
    /**
     * Load from Minecraft's native StructureTemplate
     * Note: This is a simplified version that works with NBT directly
     */
    private static LoadedTemplate loadFromMinecraftTemplate(StructureTemplate template, String name) {
        List<TemplateBlock> blocks = new ArrayList<>();
        
        var size = template.getSize();
        int width = size.getX();
        int height = size.getY();
        int depth = size.getZ();
        
        // This method is here for future compatibility with Minecraft's template system
        
        AgentsMod.LOGGER.warn("Direct template loading not fully implemented, please use NBT files directly");
        return null;
    }
    
    /**
     * Parse a structure from raw NBT data
     */
    private static LoadedTemplate parseNBTStructure(CompoundTag nbt, String name) {
        List<TemplateBlock> blocks = new ArrayList<>();
        
        Optional<ListTag> sizeOpt = nbt.getList("size");
        if (!sizeOpt.isPresent()) return null;
        ListTag sizeList = sizeOpt.get();
        Optional<Integer> widthOpt = sizeList.getInt(0);
        Optional<Integer> heightOpt = sizeList.getInt(1);
        Optional<Integer> depthOpt = sizeList.getInt(2);
        if (!widthOpt.isPresent() || !heightOpt.isPresent() || !depthOpt.isPresent()) return null;
        int width = widthOpt.get();
        int height = heightOpt.get();
        int depth = depthOpt.get();
        
        Optional<ListTag> paletteOpt = nbt.getList("palette");
        if (!paletteOpt.isPresent()) return null;
        ListTag paletteList = paletteOpt.get();
        List<BlockState> palette = new ArrayList<>();
        
        for (int i = 0; i < paletteList.size(); i++) {
            Optional<CompoundTag> blockTagOpt = paletteList.getCompound(i);
            if (!blockTagOpt.isPresent()) continue;
            CompoundTag blockTag = blockTagOpt.get();
            Optional<String> blockNameOpt = blockTag.getString("Name");
            if (!blockNameOpt.isPresent()) continue;
            String blockName = blockNameOpt.get();
            
            try {
                ResourceLocation blockLocation = ResourceLocation.parse(blockName);
                Optional<Block> blockOpt = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(blockLocation);
                Block block = blockOpt.orElse(Blocks.AIR);
                palette.add(block.defaultBlockState());
            } catch (Exception e) {
                AgentsMod.LOGGER.warn("Unknown block in structure: {}", blockName);
                palette.add(Blocks.AIR.defaultBlockState());
            }
        }
        
        Optional<ListTag> blocksOpt = nbt.getList("blocks");
        if (!blocksOpt.isPresent()) return null;
        ListTag blocksList = blocksOpt.get();
        for (int i = 0; i < blocksList.size(); i++) {
            Optional<CompoundTag> blockTagOpt = blocksList.getCompound(i);
            if (!blockTagOpt.isPresent()) continue;
            CompoundTag blockTag = blockTagOpt.get();
            
            Optional<Integer> paletteIndexOpt = blockTag.getInt("state");
            if (!paletteIndexOpt.isPresent()) continue;
            int paletteIndex = paletteIndexOpt.get();
            Optional<ListTag> posOpt = blockTag.getList("pos");
            if (!posOpt.isPresent()) continue;
            ListTag posList = posOpt.get();
            
            Optional<Integer> xOpt = posList.getInt(0);
            Optional<Integer> yOpt = posList.getInt(1);
            Optional<Integer> zOpt = posList.getInt(2);
            if (!xOpt.isPresent() || !yOpt.isPresent() || !zOpt.isPresent()) continue;
            
            BlockPos pos = new BlockPos(
                xOpt.get(),
                yOpt.get(),
                zOpt.get()
            );
            
            BlockState state = palette.get(paletteIndex);
            if (!state.isAir()) {
                blocks.add(new TemplateBlock(pos, state));
            }
        }
        
        AgentsMod.LOGGER.info("Loaded {} blocks from NBT '{}' ({}x{}x{})", blocks.size(), name, width, height, depth);
        return new LoadedTemplate(name, blocks, width, height, depth);
    }
    
    /**
     * Get list of available structure templates
     */
    public static List<String> getAvailableStructures() {
        List<String> structures = new ArrayList<>();
        
        File structuresDir = new File(System.getProperty("user.dir"), "structures");
        if (structuresDir.exists() && structuresDir.isDirectory()) {
            File[] files = structuresDir.listFiles((dir, name) -> name.endsWith(".nbt"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".nbt", "");
                    structures.add(name);
                }
            }
        }
        
        return structures;
    }
}

