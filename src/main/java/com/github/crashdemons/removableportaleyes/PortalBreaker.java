/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/ .
 */
package com.github.crashdemons.removableportaleyes;

import com.github.crashdemons.removableportaleyes.util.BlockMapper;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 *
 * @author crashdemons (crashenator at gmail.com)
 */
public class PortalBreaker extends BlockMapper {
    
    private static final BlockFace[] FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final Material[] MATERIALS = new Material[]{Material.END_PORTAL};
    
    public PortalBreaker(){
        super(FACES,MATERIALS);
    }
    
    public void startWith(Block block){
        discoverBlock(block);
    }
    
    public void breakAll(){
        exploreAll();
    }
    
    @Override
    public void onBlockExplored(Block block){//once we have gotten all the neighbors of this block recorded, we can break it.
        block.breakNaturally();
    }
}
