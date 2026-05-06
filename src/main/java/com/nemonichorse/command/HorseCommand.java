package com.nemonichorse.command;

import com.nemonichorse.gui.HorseMenuGUI;
import com.nemonichorse.gui.SkillSelectGUI;
import com.nemonichorse.item.HorseItemRegistry;
import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRarity;
import com.nemonichorse.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class HorseCommand implements CommandExecutor, TabCompleter {

    private final HorseManager horseManager;
    private final HorseMenuGUI menuGUI;
    private final SkillSelectGUI skillGUI;
    private final HorseItemRegistry itemRegistry;

    public HorseCommand(HorseManager horseManager, HorseMenuGUI menuGUI,
                        SkillSelectGUI skillGUI, HorseItemRegistry itemRegistry) {
        this.horseManager = horseManager;
        this.menuGUI = menuGUI;
        this.skillGUI = skillGUI;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            return handleInfo(player);
        }

        if (args[0].equalsIgnoreCase("skill")) {
            return handleSkill(player);
        }

        if (args[0].equalsIgnoreCase("admin") && player.hasPermission("nemonicorp.horse.admin")) {
            return handleAdmin(player, args);
        }

        sendUsage(player);
        return true;
    }

    private boolean handleInfo(Player player) {
        AbstractHorse horse = horseManager.getRiddenHorse(player);
        if (horse == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Monte em seu cavalo primeiro."));
            return true;
        }
        HorseData data = horseManager.tryLoadSync(horse);
        if (data == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Este cavalo não está registrado."));
            return true;
        }
        menuGUI.open(player, data);
        return true;
    }

    private boolean handleSkill(Player player) {
        AbstractHorse horse = horseManager.getRiddenHorse(player);
        if (horse == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Monte em seu cavalo primeiro."));
            return true;
        }
        HorseData data = horseManager.tryLoadSync(horse);
        if (data == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Este cavalo não está registrado."));
            return true;
        }
        if (!data.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Este não é seu cavalo."));
            return true;
        }
        skillGUI.open(player, data);
        return true;
    }

    private boolean handleAdmin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&7[Cavalaria Admin] Subcomandos: setlevel, addxp, setrarity, give, debug"));
            return true;
        }

        // ── /horse admin give <spur|nametag|elixir> ────────────────────
        if (args[1].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                player.sendMessage(TextUtil.color("&cUso: /horse admin give <spur|nametag|elixir>"));
                return true;
            }
            ItemStack item = switch (args[2].toLowerCase()) {
                case "spur"    -> itemRegistry.buildSpurItem();
                case "nametag" -> itemRegistry.buildNameTagItem();
                case "elixir"  -> itemRegistry.buildElixirItem();
                default -> null;
            };
            if (item == null) {
                player.sendMessage(TextUtil.color("&cItem inválido. Use: spur, nametag, elixir"));
                return true;
            }
            player.getInventory().addItem(item);
            player.sendMessage(TextUtil.color("&a[Admin] Item &f" + args[2].toLowerCase()
                    + " &adado. &8(fallback vanilla — funciona sem ItemsAdder)"));
            return true;
        }

        // ── Comandos que exigem cavalo montado ──────────────────────────
        AbstractHorse horse = horseManager.getRiddenHorse(player);
        if (horse == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Monte em um cavalo primeiro."));
            return true;
        }
        HorseData data = horseManager.tryLoadSync(horse);
        if (data == null) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Cavalo não registrado."));
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "setlevel" -> {
                if (args.length < 3) { player.sendMessage(TextUtil.color("&cUse: /horse admin setlevel <1-20>")); return true; }
                try {
                    int lvl = Integer.parseInt(args[2]);
                    if (lvl < 1 || lvl > horseManager.getConfig().getMaxLevel()) {
                        player.sendMessage(TextUtil.color("&cNível inválido."));
                        return true;
                    }
                    data.setLevel(lvl);
                    data.setXp(0);
                    horseManager.applyStats(horse, data);
                    horseManager.updateDisplay(horse, data);
                    player.sendMessage(TextUtil.color("&a[Admin] Nível definido para &f" + lvl));
                } catch (NumberFormatException e) {
                    player.sendMessage(TextUtil.color("&cNúmero inválido."));
                }
            }
            case "addxp" -> {
                if (args.length < 3) { player.sendMessage(TextUtil.color("&cUse: /horse admin addxp <quantidade>")); return true; }
                try {
                    double amount = Double.parseDouble(args[2]);
                    horseManager.addXp(horse, data, amount);
                    player.sendMessage(TextUtil.color("&a[Admin] +" + amount + " XP adicionado."));
                } catch (NumberFormatException e) {
                    player.sendMessage(TextUtil.color("&cNúmero inválido."));
                }
            }
            case "setrarity" -> {
                if (args.length < 3) { player.sendMessage(TextUtil.color("&cUse: /horse admin setrarity <raridade>")); return true; }
                HorseRarity rarity = HorseRarity.fromName(args[2]);
                data.setRarity(rarity);
                horseManager.applyStats(horse, data);
                horseManager.updateDisplay(horse, data);
                player.sendMessage(TextUtil.color("&a[Admin] Raridade definida para " + rarity.getColorCode() + rarity.getDisplayName()));
            }
            case "debug" -> {
                player.sendMessage(TextUtil.color("&8&m─────────────────────"));
                player.sendMessage(TextUtil.color("&6Horse Debug: &e" + data.getHorseId()));
                player.sendMessage(TextUtil.color("&7Dono:     &f" + data.getOwnerId()));
                player.sendMessage(TextUtil.color("&7Nível:    &e" + data.getLevel()));
                player.sendMessage(TextUtil.color("&7XP:       &f" + data.getXp()));
                player.sendMessage(TextUtil.color("&7Nome:     &f" + data.getName()));
                player.sendMessage(TextUtil.color("&7Raridade: " + data.getRarity().getColorCode() + data.getRarity().getDisplayName()));
                player.sendMessage(TextUtil.color("&7Raça:     &f" + data.getRace().getDisplayName()));
                player.sendMessage(TextUtil.color("&7Skill:    &b" + data.getEquippedSkillId()));
                player.sendMessage(TextUtil.color("&7Vínculo:  &d" + data.getBondLevel()));
                player.sendMessage(TextUtil.color("&7Dirty:    &e" + data.isDirty()));
                player.sendMessage(TextUtil.color("&7ItemsAdder: &f"
                        + (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")
                        ? "&ainstalado" : "&causeante — usando fallback vanilla")));
                player.sendMessage(TextUtil.color("&8&m─────────────────────"));
            }
            default -> player.sendMessage(TextUtil.color("&c[Admin] Subcomando desconhecido."));
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(TextUtil.color("&c[Cavalaria] Uso: /horse | /horse skill | /horse admin"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("info", "skill", "admin");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("setlevel", "addxp", "setrarity", "give", "debug");
        }
        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("setrarity")) {
                return List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
            }
            if (args[1].equalsIgnoreCase("give")) {
                return List.of("spur", "nametag", "elixir");
            }
        }
        return List.of();
    }
}
