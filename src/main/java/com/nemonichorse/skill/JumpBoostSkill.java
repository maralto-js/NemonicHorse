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

public final class JumpBoostSkill implements HorseSkill {

    @Override public String getId()           { return "JUMP"; }
    @Override public String getDisplayName()  { return "Salto Heroico"; }
    @Override public String getDescription()  { return "Salto potencializado por 5 segundos."; }
    @Override public int getRequiredLevel()   { return 6; }
    @Override public long getCooldownMillis() { return 25_000L; }
    @Override public double getManaCost()     { return 15; }
    @Override public HorseRarity getMinimumRarity() { return HorseRarity.UNCOMMON; }
    @Override public String getPermission()   { return "nemonicorp.horse.skill.jump"; }
    @Override public Material getIconMaterial() { return Material.RABBIT_FOOT; }

    @Override
    public void activate(Player rider, AbstractHorse horse, HorseData data) {
        // Jump Boost IV (amplifier 3) for 5 seconds — applied to horse so it actually jumps higher
        horse.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 3, false, true, true));

        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);

        horse.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                horse.getLocation().add(0, 0.5, 0),
                20, 0.4, 0.4, 0.4, 0.0);

        rider.sendActionBar(
                net.kyori.adventure.text.Component.text("✦ Salto Heroico ✦",
                        net.kyori.adventure.text.format.NamedTextColor.GREEN));
    }
}
