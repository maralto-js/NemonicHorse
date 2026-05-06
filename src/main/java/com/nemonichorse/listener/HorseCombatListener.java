package com.nemonichorse.listener;

import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRace;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class HorseCombatListener implements Listener {

    private final HorseManager horseManager;

    public HorseCombatListener(HorseManager horseManager) {
        this.horseManager = horseManager;
    }

    // Evict dead horses from cache so memory doesn't leak and DB record is cleaned
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHorseDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        horseManager.saveAndEvict(horse.getUniqueId());
    }

    // XP when the mounted player kills a mob
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Entity vehicle = killer.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) return;

        HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
        if (data == null) return;
        if (data.getLevel() >= horseManager.getConfig().getMaxLevel()) return;

        horseManager.addXp(horse, data, horseManager.getConfig().getXpPerKill());
        horseManager.addBondXp(data, 2.0);
    }

    // XP when the horse takes damage in combat
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHorseDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;

        HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
        if (data == null) return;
        if (data.getLevel() >= horseManager.getConfig().getMaxLevel()) return;

        boolean hasRider = horse.getPassengers().stream().anyMatch(e -> e instanceof Player);
        if (!hasRider) return;

        horseManager.addXp(horse, data, horseManager.getConfig().getXpPerDamageTaken());
    }

    // Race trait: JUNGLE reduces fall damage 50%, DESERT immune to fire/lava
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHorseEnvironmentalDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return; // handled above
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;

        HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
        if (data == null) return;

        switch (event.getCause()) {
            case FALL -> {
                if (data.getRace() == HorseRace.JUNGLE) {
                    event.setDamage(event.getDamage() * 0.5);
                }
            }
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> {
                if (data.getRace() == HorseRace.DESERT) {
                    event.setCancelled(true);
                    horse.setFireTicks(0);
                }
            }
            default -> { /* no trait for other causes */ }
        }
    }
}
