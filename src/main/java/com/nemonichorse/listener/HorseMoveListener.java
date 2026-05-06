package com.nemonichorse.listener;

import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.util.ParticleUtil;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class HorseMoveListener implements Listener {

    private final HorseManager horseManager;

    public HorseMoveListener(HorseManager horseManager) {
        this.horseManager = horseManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // CRITICAL optimisation: only fire when the player crosses a block boundary.
        // Without this, the event fires ~20 times per second per player — pure CPU waste.
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) return;

        HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
        if (data == null) return;
        if (data.getLevel() >= horseManager.getConfig().getMaxLevel()) return;

        horseManager.addXp(horse, data, horseManager.getConfig().getXpPerBlock());
        horseManager.addBondXp(data, 0.1);

        // Ambient rarity particles — 1-in-10 block chance to avoid spam
        if ((System.currentTimeMillis() / 1000) % 10 == 0) {
            ParticleUtil.rarityAmbient(horse.getLocation(), data.getRarity());
        }
    }
}
