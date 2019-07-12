package com.github.crashdemons.removableportaleyes.antispam;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Helper class that records and detects PlayerInteractEvent right-click spam
 * for playerheads.
 *
 * @author crash
 */
public class EyeSpamPreventer extends EventSpamPreventer {

    private final long interactThresholdMs;

    public EyeSpamPreventer(int numRecords, long timeMS) {
        super(numRecords);
        interactThresholdMs = timeMS;
    }

    private final class InteractRecord extends EventSpamRecord {

        final Location location;
        final UUID playerId;

        public InteractRecord(PlayerInteractEvent event) {
            super(event);
            playerId = event.getPlayer().getUniqueId();
            Block block = event.getClickedBlock();
            if (block != null) {
                location = block.getLocation();
            } else {
                location = null;
            }
        }

        boolean closeTo(InteractRecord record) {
            if (record == null) {
                return false;
            }
            if (record.playerId.equals(playerId)) {
                if (record.location == null || location == null) {
                    return false;
                }
                if (record.location.equals(location)) {
                    if (super.closeTo(record, interactThresholdMs)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public SpamResult recordEvent(org.bukkit.event.Event event) {
        if (event instanceof PlayerInteractEvent) {
            return recordEvent((PlayerInteractEvent) event);
        }
        return new SpamResult(false);
    }
    @Override
    public SpamResult checkEvent(org.bukkit.event.Event event) {
        if (event instanceof PlayerInteractEvent) {
            return checkEvent((PlayerInteractEvent) event);
        }
        return new SpamResult(false);
    }
    @Override
    public void addEvent(org.bukkit.event.Event event) {
        if (event instanceof PlayerInteractEvent) {
            addEvent((PlayerInteractEvent) event);
        }
    }
    
    public void addEvent(PlayerInteractEvent event){
        addRecord(new InteractRecord(event));
    }
    

    /**
     * Records an interaction event internally and prepares a result after
     * analyzing the event.
     * <p>
     * For the current implementation, a click to the same block location by the
     * same user within 1 second is considered spam (within 5 click records).
     *
     * @param event The PlayerInteractEvent to send to the spam-preventer.
     * @return The Spam-detection Result object
     * @see EventSpamPreventer#recordEvent(org.bukkit.event.Event)
     */
    public synchronized SpamResult recordEvent(PlayerInteractEvent event) {
        InteractRecord record = new InteractRecord(event);
        SpamResult result = checkRecord(record);
        addRecord(record);
        return result;
    }
    
    private synchronized SpamResult checkRecord(InteractRecord record){
        SpamResult result = new SpamResult(false);
        for (EventSpamRecord otherRecordObj : records) {
            InteractRecord otherRecord = (InteractRecord) otherRecordObj;
            if (record.closeTo(otherRecord)) {
                result.toggle();
                break;
            }
        }
        return result;
    }
    
    public synchronized SpamResult checkEvent(PlayerInteractEvent event){
        InteractRecord record = new InteractRecord(event);
        return checkRecord(record);
    }
}
