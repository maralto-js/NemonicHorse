package com.nemonichorse.skill;

import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRarity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DashSkill implements HorseSkill {

    @Override public String getId()           { return "DASH"; }
    @Override public String getDisplayName()  { return "Ímpeto"; }
    @Override public String getDescription()  { return "Velocidade explosiva por 3 segundos."; }
    @Override public int getRequiredLevel()   { return 3; }
    @Override public long getCooldownMillis() { return 30_000L; }
    @Override public double getManaCost()     { return 20; }
    @Override public HorseRarity getMinimumRarity() { return HorseRarity.COMMON; }
    @Override public String getPermission()   { return "nemonicorp.horse.skill.dash"; }
    @Override public Material getIconMaterial() { return Material.FEATHER; }

    @Override
    public void activate(Player rider, AbstractHorse horse, HorseData data) {
        // Speed III (amplifier 2) for 3 seconds — applied to horse so it actually moves faster
        horse.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, false, true, true));

        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_HORSE_GALLOP, 2f, 1.4f);

        // Trail particles behind the horse
        horse.getWorld().spawnParticle(
                Particle.CLOUD,
                horse.getLocation().add(0, 0.3, 0),
                12, 0.3, 0.2, 0.3, 0.05);

        rider.sendActionBar(
                net.kyori.adventure.text.Component.text("⚡ Ímpeto ⚡",
                        net.kyori.adventure.text.format.NamedTextColor.AQUA));
    }
}
