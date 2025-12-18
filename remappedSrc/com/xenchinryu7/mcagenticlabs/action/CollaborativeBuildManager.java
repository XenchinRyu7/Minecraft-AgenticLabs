package com.xenchinryu7.mcagenticlabs.action;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages collaborative building where multiple agents work on DIFFERENT SECTIONS of the same structure
 */
public class CollaborativeBuildManager {
    
    public static class CollaborativeBuild {
        public final String structureId;
        public final List<BlockPlacement> buildPlan;
        private final List<BuildSection> sections;
        private final Map<String, Integer> agentToSectionMap;
        private final AtomicInteger nextSectionIndex;
        public final Set<String> participatingagents;
        public final BlockPos startPos;
        
        public CollaborativeBuild(String structureId, List<BlockPlacement> buildPlan, BlockPos startPos) {
            this.structureId = structureId;
            this.buildPlan = buildPlan;
            this.participatingagents = ConcurrentHashMap.newKeySet();
            this.startPos = startPos;
            this.agentToSectionMap = new ConcurrentHashMap<>();
            this.nextSectionIndex = new AtomicInteger(0);
            this.sections = divideBuildIntoSections(buildPlan);
            
            AgentsMod.LOGGER.info("Divided '{}' into {} sections for collaborative building", 
                structureId, sections.size());
        }
        
        /**
         * Divide the build into 4 QUADRANTS (NW, NE, SW, SE)
         * Each quadrant is sorted BOTTOM-TO-TOP so each Agent builds their quadrant from the ground up
         */
        private List<BuildSection> divideBuildIntoSections(List<BlockPlacement> plan) {
            if (plan.isEmpty()) {
                return new ArrayList<>();
            }
            
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            
            for (BlockPlacement placement : plan) {
                minX = Math.min(minX, placement.pos.getX());
                maxX = Math.max(maxX, placement.pos.getX());
                minZ = Math.min(minZ, placement.pos.getZ());
                maxZ = Math.max(maxZ, placement.pos.getZ());
            }
            
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            
            List<BlockPlacement> northWest = new ArrayList<>();
            List<BlockPlacement> northEast = new ArrayList<>();
            List<BlockPlacement> southWest = new ArrayList<>();
            List<BlockPlacement> southEast = new ArrayList<>();
            
            for (BlockPlacement placement : plan) {
                int x = placement.pos.getX();
                int z = placement.pos.getZ();
                
                if (x <= centerX && z <= centerZ) {
                    northWest.add(placement);
                } else if (x > centerX && z <= centerZ) {
                    northEast.add(placement);
                } else if (x <= centerX && z > centerZ) {
                    southWest.add(placement);
                } else {
                    southEast.add(placement);
                }
            }
            Comparator<BlockPlacement> bottomToTop = Comparator.comparingInt(p -> p.pos.getY());
            northWest.sort(bottomToTop);
            northEast.sort(bottomToTop);
            southWest.sort(bottomToTop);
            southEast.sort(bottomToTop);
            
            List<BuildSection> sectionList = new ArrayList<>();
            if (!northWest.isEmpty()) sectionList.add(new BuildSection(0, northWest, "NORTH-WEST"));
            if (!northEast.isEmpty()) sectionList.add(new BuildSection(1, northEast, "NORTH-EAST"));
            if (!southWest.isEmpty()) sectionList.add(new BuildSection(2, southWest, "SOUTH-WEST"));
            if (!southEast.isEmpty()) sectionList.add(new BuildSection(3, southEast, "SOUTH-EAST"));
            
            AgentsMod.LOGGER.info("Divided structure into {} quadrants (BOTTOM-TO-TOP): NW={}, NE={}, SW={}, SE={} blocks", 
                sectionList.size(), northWest.size(), northEast.size(), southWest.size(), southEast.size());
            
            return sectionList;
        }
        
        public int getTotalBlocks() {
            return buildPlan.size();
        }
        
        public int getBlocksPlaced() {
            int total = 0;
            for (BuildSection section : sections) {
                total += section.getBlocksPlaced();
            }
            return total;
        }
        
        public boolean isComplete() {
            for (BuildSection section : sections) {
                if (!section.isComplete()) {
                    return false;
                }
            }
            return true;
        }
        
        public int getProgressPercentage() {
            return (getBlocksPlaced() * 100) / buildPlan.size();
        }
    }
    
    /**
     * A section of the build that one Agent works on (represents a spatial quadrant)
     */
    public static class BuildSection {
        public final int yLevel; // Used as section ID
        public final String sectionName;
        private final List<BlockPlacement> blocks;
        private final AtomicInteger nextBlockIndex;
        
        public BuildSection(int sectionId, List<BlockPlacement> blocks, String sectionName) {
            this.yLevel = sectionId;
            this.sectionName = sectionName;
            this.blocks = blocks;
            this.nextBlockIndex = new AtomicInteger(0);
        }
        
        public BlockPlacement getNextBlock() {
            int index = nextBlockIndex.getAndIncrement();
            if (index < blocks.size()) {
                return blocks.get(index);
            }
            return null;
        }
        
        public int getBlocksPlaced() {
            return Math.min(nextBlockIndex.get(), blocks.size());
        }
        
        public boolean isComplete() {
            return nextBlockIndex.get() >= blocks.size();
        }
        
        public int getTotalBlocks() {
            return blocks.size();
        }
    }
    
    public static class BlockPlacement {
        public final BlockPos pos;
        public final Block block;
        
        public BlockPlacement(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block;
        }
    }
    
    private static final Map<String, CollaborativeBuild> activeBuilds = new ConcurrentHashMap<>();
    
    /**
     * Register a new collaborative build project
     */
    public static CollaborativeBuild registerBuild(String structureType, List<BlockPlacement> buildPlan, BlockPos startPos) {
        String structureId = structureType + "_" + System.currentTimeMillis();
        CollaborativeBuild build = new CollaborativeBuild(structureId, buildPlan, startPos);
        activeBuilds.put(structureId, build);
        
        AgentsMod.LOGGER.info("Registered collaborative build '{}' at {} with {} blocks", 
            structureType, startPos, buildPlan.size());
        
        return build;
    }
    
    /**
     * Get the next block for a Agent to place (each Agent works on their own section)
     * Returns null if agent's section is complete
     */
    public static BlockPlacement getNextBlock(CollaborativeBuild build, String agentName) {
        if (build.isComplete()) {
            return null;
        }
        
        build.participatingagents.add(agentName);
        
        // Assign Agent to a section if not already assigned
        Integer sectionIndex = build.agentToSectionMap.get(agentName);
        if (sectionIndex == null) {
            sectionIndex = assignagentToSection(build, agentName);
            if (sectionIndex == null) {
                // No sections available
                return null;
            }
        }
        
        BuildSection section = build.sections.get(sectionIndex);
        BlockPlacement block = section.getNextBlock();
        
        if (block == null) {
            if (sectionIndex != null) {
                section = build.sections.get(sectionIndex);
                block = section.getNextBlock();
                if (block != null) {                }
            }
        }
        
        return block;
    }
    
    /**
     * Assign a Agent to a section (quadrant) that needs work
     * Prioritizes unassigned sections, but allows helping on large sections
     * Returns the section index, or null if all sections are complete
     */
    private static Integer assignagentToSection(CollaborativeBuild build, String agentName) {
        // First pass: Find a section that isn't complete and isn't already assigned
        for (int i = 0; i < build.sections.size(); i++) {
            BuildSection section = build.sections.get(i);
            if (!section.isComplete()) {
                boolean alreadyAssigned = build.agentToSectionMap.containsValue(i);
                
                if (!alreadyAssigned) {
                    build.agentToSectionMap.put(agentName, i);
                    AgentsMod.LOGGER.info("Assigned Agent '{}' to {} quadrant - will build {} blocks BOTTOM-TO-TOP", 
                        agentName, section.sectionName, section.getTotalBlocks());
                    return i;
                }
            }
        }
        
        // Second pass: Help with any incomplete section (even if assigned to someone else)
        for (int i = 0; i < build.sections.size(); i++) {
            BuildSection section = build.sections.get(i);
            if (!section.isComplete()) {
                build.agentToSectionMap.put(agentName, i);
                AgentsMod.LOGGER.info("Agent '{}' helping with {} quadrant ({} blocks remaining)", 
                    agentName, section.sectionName, section.getTotalBlocks() - section.getBlocksPlaced());
                return i;
            }
        }
        
        // All sections complete
        return null;
    }
    
    /**
     * Get an active build by ID
     */
    public static CollaborativeBuild getBuild(String structureId) {
        return activeBuilds.get(structureId);
    }
    
    /**
     * Complete and remove a build
     */
    public static void completeBuild(String structureId) {
        CollaborativeBuild build = activeBuilds.remove(structureId);
        if (build != null) {
            AgentsMod.LOGGER.info("Collaborative build '{}' completed by {} agents", 
                structureId, build.participatingagents.size());
        }
    }
    
    /**
     * Check if there's an active build of a structure type
     */
    public static CollaborativeBuild findActiveBuild(String structureType) {
        for (CollaborativeBuild build : activeBuilds.values()) {
            if (build.structureId.startsWith(structureType) && !build.isComplete()) {
                return build;
            }
        }
        return null;
    }
    
    /**
     * Clean up completed builds
     */
    public static void cleanupCompletedBuilds() {
        activeBuilds.entrySet().removeIf(entry -> entry.getValue().isComplete());
    }
}

