package com.nemonichorse.listener;

import com.nemonichorse.manager.HorseManager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkListener implements Listener {

    private final HorseManager horseManager;

    public ChunkListener(HorseManager horseManager) {
        this.horseManager = horseManager;
    }

    // Pre-populate cache when a chunk loads so horses are ready before players interact with them.
    // EntitySpawnEvent does NOT fire for entities loading from disk on server startup.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof AbstractHorse horse) {
                horseManager.tryLoad(horse);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof AbstractHorse horse) {
                horseManager.saveAndEvict(horse.getUniqueId());
            }
        }
    }
}
