/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.removableportaleyes;

import com.github.crashdemons.removableportaleyes.antispam.EyeSpamPreventer;
import com.github.crashdemons.removableportaleyes.antispam.PortalBreakSpamPreventer;
import com.github.crashdemons.removableportaleyes.events.PlayerTakeEndereyeEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.playerheads.events.LivingEntityDropHeadEvent;

/**
 *
 * @author crash
 */
public class RPEPlugin extends JavaPlugin implements Listener {
    public final RPEPlugin instance;
    
    WorldGuardPlugin wgp = null;
    WorldGuard wg = null;
    LivingEntityDropHeadEvent event;
    
    EyeSpamPreventer eyeDelayer = new EyeSpamPreventer(20,500);
    PortalBreakSpamPreventer portalAntispam = new PortalBreakSpamPreventer(5,5*60*1000);
    
    
    private static final BlockFace[] FACES = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public static final StateFlag FLAG_ENDEREYE_REMOVE = new StateFlag("endereye-remove", true);
    
    public RPEPlugin(){
        super();
        instance=this;
    }
    
    
    public WorldGuard getWorldGuard(){ return wg; }
    public WorldGuardPlugin getWorldGuardPlugin(){ return wgp; }
    
    private WorldGuardPlugin findWorldGuardPlugin() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }
    
    private boolean wgInit(){
        wgp = findWorldGuardPlugin();
        wg = WorldGuard.getInstance();
        if(wgp==null || wg==null){
            return false; 
        }
        
        FlagRegistry registry = wg.getFlagRegistry();
        try {
            // register our flag with the registry
            registry.register(FLAG_ENDEREYE_REMOVE);
            return true;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you may want to re-register with a different name, but this
            // could cause issues with saved flags in region files. it's better
            // to print a message to let the server admin know of the conflict
            getLogger().severe("Could not register WG flags due to a conflict with another plugin");
            return false;
        }
    }
    
    
    public void debug(String str){
        //getLogger().info(str);
    }
    /*
    private BlockFace getCcwFace(BlockFace facing){
        int i=Arrays.asList(FACES).indexOf(facing);
        i=(i+3)%4;//rotate ccw;
        return FACES[i];
    }
    
    private BlockFace getCwFace(BlockFace facing){
        int i=Arrays.asList(FACES).indexOf(facing);
        i=(i+1)%4;//rotate ccw;
        return FACES[i];
    }*/
    private void breakPortalFromPortal(Block block){//bad recursive method but the max depth for a normal end portal is low.
        block.breakNaturally();
        for(BlockFace face : FACES){
            Block adjacentBlock = block.getRelative(face);
            if(adjacentBlock.getType()==Material.END_PORTAL) breakPortalFromPortal(adjacentBlock);
        }
    }
    
    private Block getFirstPortal(Block block, EndPortalFrame frame){
        return block.getRelative(frame.getFacing());
    }
    
    private void breakPortalFromFrame( Location loc, Block block, EndPortalFrame frame){
        Block firstPortal = getFirstPortal(block,frame);
        if(firstPortal.getType()!=Material.END_PORTAL){
            debug("End portal block not found! "+firstPortal.getLocation().toVector());
            return;
        }
        breakPortalFromPortal(firstPortal);
        
    }
    
    
    private synchronized void takeEyeFrom(Player player,  Location loc, Block block, EndPortalFrame frame){
        if(!frame.hasEye()) return;
        breakPortalFromFrame(loc,block,frame);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 3.0f, 0.2f);
        frame.setEye(false);
        
        Location newloc = loc.clone();
        //newloc.setY(loc.getY()+1.5);
        block.getWorld().dropItemNaturally(loc, new ItemStack(Material.ENDER_EYE,1));
        
        block.setBlockData(frame);
        block.getState().update();
    }
    
    
    
    @EventHandler(ignoreCancelled=true,priority=EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        if(block==null) return;
        debug("Interact: "+block.getType());
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK) return;//not right-clicking the block.
        if(block.getType()!=Material.END_PORTAL_FRAME) return;//not clicking a portal
        Player player = event.getPlayer();
        Location loc = block.getLocation();
        BlockData blockData = block.getBlockData();
        debug(" bd:"+blockData.getClass().getName());
        if(!(blockData instanceof EndPortalFrame)) return;
        EndPortalFrame frame = (EndPortalFrame) blockData;
        debug(" eye:"+frame.hasEye());
        

        
        synchronized(instance){
            if(eyeDelayer.recordEvent(event).isSpam()){
                debug(" cancelling spam interact");
                event.setCancelled(true);
                return;
            }
            
            Material mat = player.getInventory().getItemInMainHand().getType();
            debug(" holding main:"+mat.name());
            if(mat==Material.ENDER_EYE) return;//must click with open hand
            mat = player.getInventory().getItemInOffHand().getType();
            debug(" holding off:"+mat.name());
            if(mat==Material.ENDER_EYE) return;//must click with open hand
             
            if(!frame.hasEye()) return;//can't remove eye from empty frame block
            
            if(getFirstPortal(block,frame).getType()==Material.END_PORTAL){//deactivating a portal
                debug(" deactivating portal");
                if(portalAntispam.checkEvent(event).isSpam()){
                    debug("   cancelling spam interact");
                    player.sendMessage(ChatColor.RED+"You must wait 5 minutes before deactivating another End Portal. "+ChatColor.GRAY+ChatColor.ITALIC+"("+"Debes esperar 5 minutos antes de desactivar otro Portal del End"+")");
                    event.setCancelled(true);
                    return;
                }else portalAntispam.addEvent(event);//reset spam timer only on real portal destructions
            }
        }
        
        
        
        LocalPlayer wgPlayer = getWorldGuardPlugin().wrapPlayer(player);
        
        com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);
        RegionQuery query = getWorldGuard().getPlatform().getRegionContainer().createQuery();
        StateFlag.State state = query.queryState(wgLoc, wgPlayer, FLAG_ENDEREYE_REMOVE);
        StateFlag.State warningstate = StateFlag.State.DENY;//query.queryState(wgLoc, null, FLAG_LECTERN_TAKE_WARNING);//Not implemented yet
        debug(" wg:"+state.name());
        if(state==StateFlag.State.DENY){
           if(warningstate!=StateFlag.State.DENY) player.sendMessage(ChatColor.RED+"Hey! "+ChatColor.GRAY+"Sorry, but you can't take Endereyes from End Portals here.");
           //event.setCancelled(true);// we're cancelling an action we implement ourselves.
           return;
        }

        PlayerTakeEndereyeEvent takeEyeEvent = new  PlayerTakeEndereyeEvent(player,block);
        getServer().getPluginManager().callEvent(takeEyeEvent);
        debug(" cancel:"+takeEyeEvent.isCancelled());
        if(takeEyeEvent.isCancelled()) return;
        
        debug(" set eye false");
        takeEyeFrom(player, loc, block, frame);
        
        
    }
    

    private boolean pluginInit(){
        return true;
    }
    
    @Override
    public void onLoad(){
        if(!wgInit()) return;
    }
    
    @Override
    public void onEnable(){
        getLogger().info("Enabling...");
        if(!pluginInit()) return;
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Enabled.");
    }
    
    @Override
    public void onDisable(){
        getLogger().info("Disabling...");
        getLogger().info("Disabled.");
    }

}
