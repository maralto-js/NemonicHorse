package com.nemonichorse.gui;

import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.skill.HorseSkill;
import com.nemonichorse.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

/**
 * GUI to view and equip horse skills.
 * Tracks open GUIs in a set to distinguish from other inventory clicks.
 */
public final class SkillSelectGUI implements Listener {

    private final HorseManager horseManager;
    private final Plugin plugin;

    private final String TITLE = "⚡ Habilidades do Cavalo";
    private final Set<UUID> openPlayers = new HashSet<>();

    // Skill slots: 10, 12, 14, 16
    private static final int[] SKILL_SLOTS = {10, 12, 14, 16};

    public SkillSelectGUI(HorseManager horseManager, Plugin plugin) {
        this.horseManager = horseManager;
        this.plugin = plugin;
    }

    public void open(Player player, HorseData data) {
        Inventory inv = Bukkit.createInventory(null, 36,
                TextUtil.colorComponent("&0⚡ &bHabilidades do Cavalo"));

        fillBorder(inv);

        int i = 0;
        for (HorseSkill skill : horseManager.getSkillManager().getAllSkills()) {
            if (i >= SKILL_SLOTS.length) break;
            Long lastUsed = data.getSkillCooldowns().get(skill.getId());
            boolean onCd = lastUsed != null
                    && System.currentTimeMillis() - lastUsed < skill.getCooldownMillis();
            long remaining = onCd ? skill.getCooldownMillis() - (System.currentTimeMillis() - lastUsed) : 0;

            inv.setItem(SKILL_SLOTS[i], skill.createIcon(data, onCd, remaining));
            i++;
        }

        // Unequip slot
        inv.setItem(31, buildUnequipItem(data));

        openPlayers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openPlayers.contains(player.getUniqueId())) return;

        // Check title matches
        String invTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());
        if (!invTitle.contains("Habilidades do Cavalo")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        AbstractHorse horse = horseManager.getRiddenHorse(player);
        if (horse == null) {
            player.closeInventory();
            return;
        }

        HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
        if (data == null) {
            player.closeInventory();
            return;
        }

        // Handle unequip slot
        if (slot == 31) {
            data.setEquippedSkillId(null);
            player.sendMessage(TextUtil.color("&7[Cavalaria] Habilidade removida."));
            player.closeInventory();
            return;
        }

        // Handle skill slots
        List<HorseSkill> skillList = new ArrayList<>(horseManager.getSkillManager().getAllSkills());
        for (int i = 0; i < SKILL_SLOTS.length && i < skillList.size(); i++) {
            if (SKILL_SLOTS[i] != slot) continue;
            HorseSkill skill = skillList.get(i);

            if (!player.hasPermission(skill.getPermission())) {
                player.sendMessage(TextUtil.color("&c[Cavalaria] Você não desbloqueou " + skill.getDisplayName() + "."));
                return;
            }
            if (data.getLevel() < skill.getRequiredLevel()) {
                player.sendMessage(TextUtil.color("&c[Cavalaria] Requer nível " + skill.getRequiredLevel() + "."));
                return;
            }
            if (skill.getMinimumRarity() != null
                    && data.getRarity().ordinal() < skill.getMinimumRarity().ordinal()) {
                player.sendMessage(TextUtil.color("&c[Cavalaria] &f" + skill.getDisplayName()
                        + " &crequere raridade "
                        + skill.getMinimumRarity().getColorCode()
                        + skill.getMinimumRarity().getDisplayName() + "&c ou superior."));
                return;
            }

            data.setEquippedSkillId(skill.getId());
            player.sendMessage(TextUtil.color("&a[Cavalaria] &f" + skill.getDisplayName() + " &aequipada!"));
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openPlayers.remove(player.getUniqueId());
        }
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = buildGlass();
        int[] border = {0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32,33,34,35};
        for (int s : border) inv.setItem(s, glass);
    }

    private ItemStack buildGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildUnequipItem(HorseData data) {
        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ser.deserialize("&cRemover Habilidade"));
            meta.lore(List.of(ser.deserialize(
                    "&7Atual: &f" + (data.getEquippedSkillId() != null
                            ? data.getEquippedSkillId() : "NENHUMA"))));
            item.setItemMeta(meta);
        }
        return item;
    }
}
