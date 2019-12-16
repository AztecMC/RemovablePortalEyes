/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/ .
 */
package com.github.crashdemons.removableportaleyes.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Wrapper for hashmap used for mapping which blocks we've visited when destroying portals.
 * Mappings should only be conducted in a single world, where the 3D vector defines the location of a single block.
 * This mapping maps blocks as "discovered" (recorded, needs exploration) or "explored" (all neighbors are discovered).
 * 
 * Each instance should also be only accessed by a single thread. This is not because of hashmap access, but because all mappings and searches would need to be made synchronized if it weren't.
 * @author crashdemons (crashenator at gmail.com)
 */
public class BlockMapper {
    private final HashMap<Vector,BlockMappingState> mapping;
    private final BlockFace[] explorationDirections;
    private final List<Material> typesToMap;
    
    public BlockMapper(BlockFace[] explorationDirections, Material[] typesToMap){
        this.mapping = new HashMap<>();
        this.explorationDirections = explorationDirections;
        this.typesToMap = Arrays.asList(typesToMap);
    }
    
    
    //------------------------------------------------------------
    private static Vector blockToVector(Block block){
        return block.getLocation().toVector().toBlockVector();
    }
    protected void mapBlock(Block block, Boolean state){
        mapping.put(blockToVector(block), new BlockMappingState(block,state));
    }
    protected BlockMappingState getMapping(Block block){
        return mapping.get(blockToVector(block));
    }
    protected boolean containsBlock(Block block){
        return mapping.containsKey(blockToVector(block));
    }
    protected boolean isDiscovered(Block block){
        BlockMappingState explored = getMapping(block); //false indicated discovered but not explored, true is explored AND discovered.
        return (explored!=null);
    }
    protected boolean isExplored(Block block){
        BlockMappingState state = getMapping(block);//false indicated discovered but not explored.
        if(state==null) return false;
        return state.explored;
    }
    //------------------------------------------------------------
    
    public void discoverBlock(Block block){
        if(!typesToMap.contains(block.getType())) return;//we only allow blocks set by the filter
        if(containsBlock(block)) return;//do not discover already discovered blocks.
        mapBlock(block,false);//mark the block as discovered (not explored) in the map.
        onBlockDiscovered(block);//perform the application-defined operation.
    }
    
    public void exploreBlock(Block block){
        if(!typesToMap.contains(block.getType())) return;//we only allow blocks set by the filter
        if(isExplored(block)) return;//don't explore already-explored blocks (but allow Discovered or Unmapped blocks)
        mapBlock(block,true);//mark the block as explored before we even start, not that order matters since you Should be using a single thread...
        for(BlockFace face : explorationDirections){//discover (record) blocks in all the defined directions from this block for future exploration.
            Block adjacentBlock = block.getRelative(face);
            discoverBlock(adjacentBlock);
        }
        onBlockExplored(block);//perform the application-defined operation.
    }
    
    public int exploreAllDiscovered(){
        final ArrayList<Block> unexplored = new ArrayList<>();
        for(HashMap.Entry<Vector,BlockMappingState> entry : mapping.entrySet()){//queue up explorations OUTSIDE of iteration loop - don't modify what we're currently iterating!
            if(!entry.getValue().explored)
                unexplored.add(entry.getValue().block);
        }
        for(Block block : unexplored){
            exploreBlock(block);
        }
        return unexplored.size();//return the number of blocks explored.
    }
    public void exploreAll(){
        boolean shouldExplore = true;
        do{
            shouldExplore = exploreAllDiscovered() > 0;//we should keep exploring while new blocks were still explored last time.
        }while(shouldExplore);
    }
    
    /**
     * Action to perform when a block is explored (all neighbors recorded for future exploration).
     * This operation can contain world-removal actions since the block will not be revisited.
     * @param block the subject block
     */
    public void onBlockExplored(Block block){
        
    }
    
    /**
     * Action to perform when a block is discovered (all neighbors recorded for future exploration).
     * This operation MUST NOT contain world-removal actions since the block will be revisited for exploration!
     * @param block the subject block
     */
    public void onBlockDiscovered(Block block){
        
    }
}
