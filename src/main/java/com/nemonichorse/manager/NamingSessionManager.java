package com.nemonichorse.manager;

import com.nemonichorse.config.HorseConfig;
import com.nemonichorse.model.HorseData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Tracks pending naming sessions created when a horse is born via breeding.
 * Sessions persist until the player provides a valid name or logs out.
 */
public final class NamingSessionManager {

    private record PendingNaming(AbstractHorse foal, HorseData data, long createdAt) {}

    private final Map<UUID, PendingNaming> pending = new ConcurrentHashMap<>();
    private final HorseConfig config;
    /** Called when a name is confirmed: (data, name). Provided by HorseManager. */
    private BiConsumer<HorseData, String> onNameConfirmed;

    public NamingSessionManager(HorseConfig config) {
        this.config = config;
    }

    public void setOnNameConfirmed(BiConsumer<HorseData, String> callback) {
        this.onNameConfirmed = callback;
    }

    /** Returns true if a new session was started, false if one already existed. */
    public boolean startSession(Player breeder, AbstractHorse foal, HorseData data) {
        if (pending.containsKey(breeder.getUniqueId())) return false;
        pending.put(breeder.getUniqueId(), new PendingNaming(foal, data, System.currentTimeMillis()));

        breeder.sendMessage(color("&8&m──────────────────────────────────"));
        breeder.sendMessage(color("&6[&e✦ Cavalaria&6] &eSeu cavalo nasceu!"));
        breeder.sendMessage(color("&7Digite o nome no chat. &8(máx. " + config.getNamingMaxLength() + " caracteres)"));
        breeder.sendMessage(color("&7Digite &cCANCELAR &7para gerar um nome automático."));
        breeder.sendMessage(color("&8&m──────────────────────────────────"));
        return true;
    }

    public boolean hasPendingSession(UUID playerUuid) {
        return pending.containsKey(playerUuid);
    }

    /**
     * Processes the player's chat input as a horse name.
     * Returns true if the session was handled (regardless of cancel/success).
     */
    public boolean processInput(Player player, String input) {
        PendingNaming session = pending.remove(player.getUniqueId());
        if (session == null) return false;

        if (input.equalsIgnoreCase("CANCELAR")) {
            String autoName = generateAutoName(session.data());
            finalise(player, session, autoName, true);
            return true;
        }

        // Validate length
        if (input.length() > config.getNamingMaxLength()) {
            player.sendMessage(color("&c[Cavalaria] Nome muito longo! Máximo "
                    + config.getNamingMaxLength() + " caracteres. Tente novamente."));
            // Re-add so the player can try again
            pending.put(player.getUniqueId(), session);
            return true;
        }

        // Check blocked words
        String lower = input.toLowerCase();
        for (String blocked : config.getBlockedWords()) {
            if (lower.contains(blocked.toLowerCase())) {
                player.sendMessage(color("&c[Cavalaria] Nome inválido. Escolha outro nome."));
                pending.put(player.getUniqueId(), session);
                return true;
            }
        }

        finalise(player, session, input, false);
        return true;
    }

    /** Remove session on quit so it can be started fresh on next login. */
    public void removeSession(UUID playerUuid) {
        PendingNaming session = pending.remove(playerUuid);
        if (session != null && config.isAutoNameOnNull()) {
            // Horse unnamed because player quit — assign auto-name silently
            String autoName = generateAutoName(session.data());
            session.data().setName(autoName);
            if (onNameConfirmed != null) {
                onNameConfirmed.accept(session.data(), autoName);
            }
        }
    }

    private void finalise(Player player, PendingNaming session, String name, boolean wasAuto) {
        session.data().setName(name);
        if (onNameConfirmed != null) {
            onNameConfirmed.accept(session.data(), name);
        }
        if (wasAuto) {
            player.sendMessage(color("&7[Cavalaria] Nome automático atribuído: &e" + name));
        } else {
            player.sendMessage(color("&a[Cavalaria] Seu cavalo agora se chama: &e" + name));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    private String generateAutoName(HorseData data) {
        String[] prefixes = {"Nobre", "Veloz", "Bravo", "Selvagem", "Antigo", "Sombrio"};
        String[] suffixes = {"Corcel", "Trovão", "Tempestade", "Vento", "Chama", "Gelo"};
        int h = Math.abs(data.getHorseId().hashCode());
        return prefixes[h % prefixes.length] + " " + suffixes[(h / prefixes.length) % suffixes.length];
    }

    private static String color(String s) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }
}
