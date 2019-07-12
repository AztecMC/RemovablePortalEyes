/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/ .
 */
package com.github.crashdemons.removableportaleyes.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 *
 * @author crashdemons (crashenator at gmail.com)
 */
public class PlayerTakeEndereyeEvent extends PlayerEvent implements Cancellable{
    private static final HandlerList HANDLERS = new HandlerList();
    private final Block block;
    private boolean cancelled;
    
    
    public PlayerTakeEndereyeEvent(Player player, Block block){
        super(player);
        this.block=block;
        this.cancelled=false;
    }
    
    public Block getClickedBlock(){
        return block;
    }
    
    @Override
    public void setCancelled(boolean state){
        cancelled=state;
    }
    
    public boolean isCancelled(){
        return cancelled;
    }
    
    
    /**
     * Get a list of handlers for the event.
     *
     * @return a list of handlers for the event
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Get a list of handlers for the event.
     *
     * @return a list of handlers for the event
     */
    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    
}
