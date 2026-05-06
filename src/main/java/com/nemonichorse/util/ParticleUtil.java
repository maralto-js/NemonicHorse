package com.nemonichorse.util;

import com.nemonichorse.model.HorseRarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public final class ParticleUtil {

    private ParticleUtil() {}

    public static void levelUpEffect(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 60, 0.5, 0.5, 0.5, 0.15);
        w.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 2, 0), 20, 0.6, 0.3, 0.6, 0.0);
    }

    public static void rarityAmbient(Location loc, HorseRarity rarity) {
        World w = loc.getWorld();
        if (w == null) return;
        // Only spawn ambient particles for RARE and above to avoid performance cost
        if (rarity.ordinal() < HorseRarity.RARE.ordinal()) return;

        w.spawnParticle(rarity.getLevelParticle(), loc.clone().add(0, 1.8, 0),
                rarity.ordinal() * 3 + 2, 0.4, 0.2, 0.4, 0.02);
    }

    public static void dashEffect(Location loc) {
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.CLOUD, loc, 12, 0.3, 0.2, 0.3, 0.05);
    }
}
