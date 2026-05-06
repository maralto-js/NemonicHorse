package com.nemonichorse.skill;

import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRarity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Contract for every horse skill.
 * Each skill is stateless — all cooldown/state data lives in HorseData.
 */
public interface HorseSkill {

    /** Unique uppercase identifier, e.g. "DASH". */
    String getId();

    /** Display name shown in GUI and messages, e.g. "Ímpeto". */
    String getDisplayName();

    /** Short description shown in GUI lore. */
    String getDescription();

    /** Minimum horse level to equip or use this skill. */
    int getRequiredLevel();

    /** Cooldown in milliseconds. Evaluated against System.currentTimeMillis(). */
    long getCooldownMillis();

    /** Mana cost deducted via MMOCoreHook. 0 = free. */
    double getManaCost();

    /** Minimum rarity required. */
    HorseRarity getMinimumRarity();

    /** LuckPerms/Bukkit permission node. */
    String getPermission();

    /**
     * Additional pre-activation checks beyond level, cooldown and mana.
     * Default implementation always returns true.
     */
    default boolean canUse(Player rider, AbstractHorse horse, HorseData data) {
        return true;
    }

    /**
     * Execute the skill effect. Called on the main server thread.
     * Cooldown and mana have already been consumed before this call.
     */
    void activate(Player rider, AbstractHorse horse, HorseData data);

    /** Build the GUI icon for this skill. */
    default ItemStack createIcon(HorseData data, boolean onCooldown, long remainingMs) {
        ItemStack icon = new ItemStack(getIconMaterial());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        String cooldownSuffix = onCooldown
                ? " &8(&c" + (remainingMs / 1000) + "s&8)"
                : " &a[Pronto]";

        meta.displayName(parse("&b" + getDisplayName() + cooldownSuffix));

        boolean equipped = getId().equals(data.getEquippedSkillId());
        String rarityLine = getMinimumRarity() != null && getMinimumRarity() != HorseRarity.COMMON
                ? "&7Raridade mínima: " + getMinimumRarity().getColorCode() + getMinimumRarity().getDisplayName()
                : "&7Raridade mínima: &fTodas";

        meta.lore(List.of(
                parse("&7" + getDescription()),
                Component.empty(),
                parse("&7Nível mínimo: &e" + getRequiredLevel()),
                parse(rarityLine),
                parse("&7Custo: &b" + (int) getManaCost() + " mana"),
                parse("&7Recarga: &f" + (getCooldownMillis() / 1000) + "s"),
                Component.empty(),
                parse(equipped ? "&a✔ Equipada" : "&eClique para equipar")
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    private static Component parse(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    /** Icon material shown in the GUI. Override for custom materials. */
    default Material getIconMaterial() {
        return Material.BLAZE_ROD;
    }
}
