package com.nemonichorse.listener;

import com.nemonichorse.manager.NamingSessionManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public final class PlayerListener implements Listener {

    private final NamingSessionManager namingSessionManager;
    private final Plugin plugin;

    public PlayerListener(NamingSessionManager namingSessionManager, Plugin plugin) {
        this.namingSessionManager = namingSessionManager;
        this.plugin = plugin;
    }

    // ── Chat: intercept for active naming sessions ─────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!namingSessionManager.hasPendingSession(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Must run on main thread to update entity state
        plugin.getServer().getScheduler().runTask(plugin, () ->
                namingSessionManager.processInput(event.getPlayer(), text));
    }

    // ── Quit: remove session and auto-name if needed ───────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        namingSessionManager.removeSession(event.getPlayer().getUniqueId());
    }
}
