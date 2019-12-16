/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/ .
 */
package com.github.crashdemons.removableportaleyes.util;

import org.bukkit.block.Block;

/**
 *
 * @author crashdemons (crashenator at gmail.com)
 */
public class BlockMappingState {
    public final Block block;
    public boolean explored;
    
    public BlockMappingState(Block block){
        this.block=block;
        this.explored=false;
    }
    
    public BlockMappingState(Block block, boolean explorationState){
        this.block=block;
        this.explored=explorationState;
    }
}
