package com.nemonichorse.listener;

import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.manager.NamingSessionManager;
import com.nemonichorse.model.HorseRace;
import com.nemonichorse.model.HorseRarity;
import com.nemonichorse.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.UUID;

public final class HorseLifecycleListener implements Listener {

    private final HorseManager horseManager;
    private final NamingSessionManager namingSessionManager;

    public HorseLifecycleListener(HorseManager horseManager,
                                  NamingSessionManager namingSessionManager) {
        this.horseManager = horseManager;
        this.namingSessionManager = namingSessionManager;
    }

    // ── Taming: register horse on domestication ───────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        if (!(event.getOwner() instanceof Player tamer)) return;

        String biomeName = horse.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();
        HorseRace race = HorseRace.fromBiomeName(biomeName);

        horseManager.loadOrCreate(horse, tamer.getUniqueId(), HorseRarity.COMMON, race);

        tamer.sendMessage(TextUtil.color(
                "&6[&e✦ Cavalaria&6] &eVocê domou um cavalo! Use &f/horse &epara gerenciá-lo."));
        tamer.playSound(tamer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ── Breeding: foal inherits rarity, starts tamed ─────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse foal)) return;

        Entity breederEntity = event.getBreeder();

        HorseRarity p1Rarity = horseManager.get(event.getMother().getUniqueId())
                .map(d -> d.getRarity()).orElse(HorseRarity.COMMON);
        HorseRarity p2Rarity = horseManager.get(event.getFather().getUniqueId())
                .map(d -> d.getRarity()).orElse(HorseRarity.COMMON);

        // New table-based breeding — no longer uses config upgrade/downgrade chances
        HorseRarity foalRarity = HorseRarity.fromBreeding(p1Rarity, p2Rarity);

        HorseRace foalRace = horseManager.get(event.getMother().getUniqueId())
                .map(d -> d.getRace()).orElse(HorseRace.PLAINS);

        Player breeder = breederEntity instanceof Player p ? p : null;
        UUID ownerId = breeder != null ? breeder.getUniqueId()
                : horseManager.get(event.getMother().getUniqueId())
                        .map(d -> d.getOwnerId()).orElse(null);

        if (ownerId == null) return;

        // Auto-tame: foal born from two registered horses is already tamed by owner.
        // Must happen BEFORE loadOrCreate so stampPdc sees the correct taming state.
        foal.setTamed(true);
        foal.setOwner(Bukkit.getOfflinePlayer(ownerId));

        var foalData = horseManager.loadOrCreate(foal, ownerId, foalRarity, foalRace);

        if (breeder != null) {
            breeder.sendMessage(TextUtil.color(
                    "&6[&e✦ Cavalaria&6] &eSeu potro nasceu como &r"
                    + foalRarity.getColorCode() + foalRarity.getDisplayName()
                    + "&e! Dê um nome a ele."));
            namingSessionManager.startSession(breeder, foal, foalData);
        }
    }

    // ── Spawn: restore state when horse entity loads ──────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse)) return;
        horseManager.tryLoad(horse);
    }

    // ── Mount: retroactive registration + reapply stats ──────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!(event.getVehicle() instanceof AbstractHorse horse)) return;

        // Retroactive registration: horse tamed before plugin install, or edge-case
        // where EntityTameEvent wasn't captured (e.g. temper raised via feeding then plugin reloaded)
        if (horse.isTamed()
                && horse.getOwner() != null
                && horse.getOwner().getUniqueId().equals(player.getUniqueId())
                && !horseManager.isRegistered(horse)) {
            String biomeName = horse.getLocation().getBlock().getBiome().getKey().getKey().toUpperCase();
            HorseRace race = HorseRace.fromBiomeName(biomeName);
            horseManager.loadOrCreate(horse, player.getUniqueId(), HorseRarity.COMMON, race);
            player.sendMessage(TextUtil.color(
                    "&6[&e✦ Cavalaria&6] &eCavalo registrado! Use &f/horse &epara gerenciá-lo."));
        }

        // Sync-load covers the case where the horse is registered but not yet in cache
        // (e.g. server just restarted and ChunkLoadEvent's async tryLoad hasn't resolved yet)
        horseManager.tryLoadSync(horse);
    }

    // ── Dismount: small bond XP reward ───────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) return;
        if (!(event.getVehicle() instanceof AbstractHorse horse)) return;

        horseManager.get(horse.getUniqueId()).ifPresent(data ->
                horseManager.addBondXp(data, 5.0));
    }
}
