package com.nemonichorse.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Soft wrapper for MMOCore integration.
 * Mana check via PlaceholderAPI (same approach as the old Skript).
 * Mana removal and XP grant via console commands — stable and version-agnostic.
 */
public final class MMOCoreHook {

    private final Logger logger;
    private final boolean papiAvailable;

    public MMOCoreHook(Logger logger) {
        this.logger = logger;
        boolean papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.papiAvailable = papi;
        if (papi) {
            logger.info("[NemonicHorse] PlaceholderAPI found — mana check enabled.");
        } else {
            logger.warning("[NemonicHorse] PlaceholderAPI not found — mana cost will be skipped.");
        }
    }

    public boolean isMMOCoreAvailable() {
        return Bukkit.getPluginManager().getPlugin("MMOCore") != null;
    }

    /** Returns current mana or Double.MAX_VALUE if PAPI/MMOCore is absent. */
    public double getMana(Player player) {
        if (!papiAvailable || !isMMOCoreAvailable()) return Double.MAX_VALUE;
        try {
            String raw = PlaceholderAPI.setPlaceholders(player, "%mmocore_mana%");
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Returns true and consumes mana if sufficient, false if insufficient.
     * If MMOCore/PAPI is absent always returns true (no cost).
     */
    public boolean consumeMana(Player player, double cost) {
        if (cost <= 0) return true;
        if (!isMMOCoreAvailable()) return true;

        double current = getMana(player);
        if (current < cost) return false;

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mmocore admin mana remove " + player.getName() + " " + (int) cost);
        return true;
    }

    /** Grants main-class XP to the player via console command. */
    public void giveMainXp(Player player, double amount) {
        if (!isMMOCoreAvailable()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "mmocore admin xp give " + player.getName() + " main " + (int) amount);
    }
}
