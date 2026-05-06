package com.nemonichorse.skill;

import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRarity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class TrampleSkill implements HorseSkill {

    private static final double RADIUS = 4.0;
    private static final double DAMAGE = 8.0;

    @Override public String getId()           { return "TRAMPLE"; }
    @Override public String getDisplayName()  { return "Esmagar"; }
    @Override public String getDescription()  { return "Dano em área de " + (int) RADIUS + " blocos."; }
    @Override public int getRequiredLevel()   { return 15; }
    @Override public long getCooldownMillis() { return 20_000L; }
    @Override public double getManaCost()     { return 25; }
    @Override public HorseRarity getMinimumRarity() { return HorseRarity.EPIC; }
    @Override public String getPermission()   { return "nemonicorp.horse.skill.trample"; }
    @Override public Material getIconMaterial() { return Material.GOLDEN_BOOTS; }

    @Override
    public void activate(Player rider, AbstractHorse horse, HorseData data) {
        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_LAND, 2f, 0.6f);

        horse.getWorld().spawnParticle(
                Particle.EXPLOSION,
                horse.getLocation().add(0, 0.5, 0),
                3, 1.0, 0.3, 1.0, 0.0);

        horse.getWorld().spawnParticle(
                Particle.BLOCK,
                horse.getLocation(),
                40, 1.5, 0.1, 1.5, 0.0,
                horse.getWorld().getBlockAt(horse.getLocation()).getBlockData());

        Collection<LivingEntity> nearby = horse.getWorld().getNearbyLivingEntities(
                horse.getLocation(), RADIUS, RADIUS, RADIUS);

        int hit = 0;
        for (LivingEntity entity : nearby) {
            if (entity.equals(rider) || entity.equals(horse)) continue;
            if (entity instanceof Player) continue; // no PvP — configurable in future
            entity.damage(DAMAGE, rider);
            hit++;
        }

        rider.sendActionBar(
                net.kyori.adventure.text.Component.text("💥 Esmagar (" + hit + " alvos) 💥",
                        net.kyori.adventure.text.format.NamedTextColor.RED));
    }
}
