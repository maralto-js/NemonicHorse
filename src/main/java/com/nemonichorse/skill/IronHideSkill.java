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

public final class IronHideSkill implements HorseSkill {

    @Override public String getId()           { return "IRONHIDE"; }
    @Override public String getDisplayName()  { return "Casco de Ferro"; }
    @Override public String getDescription()  { return "Resistência total por 8 segundos."; }
    @Override public int getRequiredLevel()   { return 10; }
    @Override public long getCooldownMillis() { return 45_000L; }
    @Override public double getManaCost()     { return 30; }
    @Override public HorseRarity getMinimumRarity() { return HorseRarity.RARE; }
    @Override public String getPermission()   { return "nemonicorp.horse.skill.ironhide"; }
    @Override public Material getIconMaterial() { return Material.IRON_CHESTPLATE; }

    @Override
    public void activate(Player rider, AbstractHorse horse, HorseData data) {
        // Resistance II for 8 seconds on both rider and horse
        PotionEffect resistance = new PotionEffect(PotionEffectType.RESISTANCE, 160, 1, false, true, true);
        rider.addPotionEffect(resistance);
        horse.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1, false, false, false));

        horse.getWorld().playSound(horse.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 2f, 0.9f);

        horse.getWorld().spawnParticle(
                Particle.CRIT,
                horse.getLocation().add(0, 1.0, 0),
                30, 0.5, 0.5, 0.5, 0.1);

        rider.sendActionBar(
                net.kyori.adventure.text.Component.text("⚔ Casco de Ferro ⚔",
                        net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }
}
