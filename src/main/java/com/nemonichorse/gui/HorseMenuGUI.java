package com.nemonichorse.gui;

import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.manager.NamingSessionManager;
import com.nemonichorse.manager.SkillManager;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Native GUI — no DeluxeMenus required.
 * Size 36 (4 rows). All state is read from HorseData at open time; never updates live.
 */
public final class HorseMenuGUI implements Listener {

    private static final int SLOT_STATUS   = 13;
    private static final int SLOT_SKILLS   = 11;
    private static final int SLOT_NAMING   = 15;
    private static final int SLOT_BOND     = 22;
    private static final int SLOT_CLOSE    = 31;

    private final HorseManager horseManager;
    private final NamingSessionManager namingSessionManager;
    private final SkillSelectGUI skillSelectGUI;
    private final Set<UUID> openPlayers = new HashSet<>();

    public HorseMenuGUI(HorseManager horseManager,
                        NamingSessionManager namingSessionManager,
                        SkillSelectGUI skillSelectGUI) {
        this.horseManager = horseManager;
        this.namingSessionManager = namingSessionManager;
        this.skillSelectGUI = skillSelectGUI;
    }

    /** Opens the horse menu for the given player. Must be called from the main thread. */
    public void open(Player player, HorseData data) {
        Inventory inv = Bukkit.createInventory(null, 36,
                TextUtil.colorComponent("&0⚔ &6&lCavalaria &0⚔"));

        fillBorder(inv);

        inv.setItem(SLOT_STATUS,  buildStatusItem(data));
        inv.setItem(SLOT_SKILLS,  buildSkillsItem(data));
        inv.setItem(SLOT_NAMING,  buildNamingItem());
        inv.setItem(SLOT_BOND,    buildBondItem(data));
        inv.setItem(SLOT_CLOSE,   buildCloseItem());

        openPlayers.add(player.getUniqueId());
        player.openInventory(inv);
    }

    // ── Event handlers ────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openPlayers.contains(player.getUniqueId())) return;

        String invTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());
        if (!invTitle.contains("Cavalaria")) return;

        // Cancel ALL clicks — including shift-clicks into player inventory (rawSlot >= topSize)
        event.setCancelled(true);

        // Ignore clicks that land on the player's own inventory rows
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_SKILLS -> {
                AbstractHorse horse = horseManager.getRiddenHorse(player);
                if (horse == null) { player.closeInventory(); return; }
                HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
                if (data == null) { player.closeInventory(); return; }
                // closeInventory fires InventoryCloseEvent → removes from openPlayers before skill GUI adds
                player.closeInventory();
                skillSelectGUI.open(player, data);
            }
            case SLOT_NAMING -> {
                AbstractHorse horse = horseManager.getRiddenHorse(player);
                if (horse == null) { player.closeInventory(); return; }
                HorseData data = horseManager.getIfLoaded(horse.getUniqueId());
                if (data == null) { player.closeInventory(); return; }
                player.closeInventory();
                if (namingSessionManager.hasPendingSession(player.getUniqueId())) {
                    player.sendMessage(TextUtil.color("&c[Cavalaria] Você já tem uma sessão de nomeação ativa."));
                    return;
                }
                namingSessionManager.startSession(player, horse, data);
            }
            case SLOT_CLOSE -> player.closeInventory();
            // SLOT_STATUS and SLOT_BOND are info-only — event already cancelled above
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openPlayers.remove(player.getUniqueId());
        }
    }

    // ── Item builders ────────────────────────────────────────────────

    private ItemStack buildStatusItem(HorseData data) {
        String name   = data.getName() != null ? data.getName() : "Sem Nome";
        int level     = data.getLevel();
        double xp     = data.getXp();
        double needed = horseManager.getConfig().xpForLevel(level);
        int pct       = level >= horseManager.getConfig().getMaxLevel() ? 100
                : (int) (xp / needed * 100);

        List<String> lore = new ArrayList<>();
        lore.add(color("&8─────────────────────"));
        lore.add(color("&7Nome:      " + data.getRarity().getColorCode() + name));
        lore.add(color("&7Raridade:  " + data.getRarity().getColorCode() + data.getRarity().getDisplayName()));
        lore.add(color("&7Raça:      &f" + data.getRace().getDisplayName()));
        lore.add(color("&7Nível:     &e" + level + " &8/ &e" + horseManager.getConfig().getMaxLevel()));
        lore.add(color("&7XP:        &f" + (int) xp + " &8/ &f" + (int) needed + " &8(" + pct + "%)"));
        lore.add(color("&7Vínculo:   &d" + data.getBondLevel() + " &8/ &d10"));
        lore.add(color("&8─────────────────────"));
        lore.add(color("&7Traço: &f" + data.getRace().getTraitDescription()));

        return buildItem(Material.SADDLE, "&e✦ Status do Cavalo", lore);
    }

    private ItemStack buildSkillsItem(HorseData data) {
        SkillManager sm = horseManager.getSkillManager();
        List<String> lore = new ArrayList<>();
        lore.add(color("&8─────────────────────"));
        lore.add(color("&7Equipada: &b" + (data.getEquippedSkillId() != null
                ? data.getEquippedSkillId() : "NENHUMA")));
        lore.add("");

        for (HorseSkill skill : sm.getAllSkills()) {
            boolean locked = data.getLevel() < skill.getRequiredLevel();
            String status = locked
                    ? "&c✘ Lv " + skill.getRequiredLevel() + " necessário"
                    : "&a✔ Disponível";
            lore.add(color("&f" + skill.getDisplayName() + " &8— " + status));
        }

        lore.add("");
        lore.add(color("&eClique para gerenciar habilidades"));

        return buildItem(Material.BLAZE_ROD, "&b⚡ Habilidades", lore);
    }

    private ItemStack buildNamingItem() {
        return buildItem(Material.NAME_TAG, "&6✎ Nomear Cavalo",
                List.of(color("&8─────────────────────"),
                        color("&7Dê um nome único ao"),
                        color("&7seu cavalo via chat."),
                        "",
                        color("&eClique para abrir a nomeação")));
    }

    private ItemStack buildBondItem(HorseData data) {
        int bond = data.getBondLevel();
        double bondXp  = data.getBondXp();
        double needed  = (bond + 1) * 200.0;
        int pct = bond >= 10 ? 100 : (int) (bondXp / needed * 100);

        return buildItem(Material.HEART_OF_THE_SEA, "&d❤ Vínculo",
                List.of(color("&8─────────────────────"),
                        color("&7Nível de vínculo: &d" + bond + " &8/ &d10"),
                        color("&7Progresso: &f" + (int) bondXp + " &8/ &f" + (int) needed + " &8(" + pct + "%)"),
                        "",
                        color("&7Vincule-se andando e"),
                        color("&7combatendo com seu cavalo.")));
    }

    private ItemStack buildCloseItem() {
        return buildItem(Material.BARRIER, "&cFechar", List.of());
    }

    // ── GUI utility ───────────────────────────────────────────────────

    private void fillBorder(Inventory inv) {
        ItemStack glass = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,17,18,19,20,21,23,24,25,26,27,28,29,30,32,33,34,35};
        for (int slot : borderSlots) {
            inv.setItem(slot, glass);
        }
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        LegacyComponentSerializer sec = LegacyComponentSerializer.legacySection();
        meta.displayName(sec.deserialize(color(name)));
        meta.lore(lore.stream()
                .map(s -> s.isEmpty() ? Component.empty() : sec.deserialize(s))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String s) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }
}
