package com.nemonichorse.listener;

import com.nemonichorse.item.HorseItemRegistry;
import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.manager.NamingSessionManager;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.util.TextUtil;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class HorseInteractListener implements Listener {

    private final HorseManager horseManager;
    private final HorseItemRegistry itemRegistry;
    private final NamingSessionManager namingSessionManager;

    public HorseInteractListener(HorseManager horseManager,
                                 HorseItemRegistry itemRegistry,
                                 NamingSessionManager namingSessionManager) {
        this.horseManager = horseManager;
        this.itemRegistry = itemRegistry;
        this.namingSessionManager = namingSessionManager;
    }

    // ── Skill activation: player rides + Shift+RightClick with spur ──

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only main hand, right-click actions
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!itemRegistry.isSpur(held)) return;

        event.setCancelled(true);

        HorseData data = horseManager.tryLoadSync(horse);
        if (data == null) {
            player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                    + "&cEste cavalo não está registrado no sistema."));
            return;
        }

        if (!data.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                    + "&cVocê não é o dono deste cavalo."));
            return;
        }

        horseManager.activateSkill(player, horse, data);
    }

    // ── Shift+RightClick ON horse: name tag or elixir ─────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof AbstractHorse horse)) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        String itemType = itemRegistry.getItemType(held);
        if (itemType == null) return;

        event.setCancelled(true);

        HorseData data = horseManager.tryLoadSync(horse);
        if (data == null) {
            player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                    + "&cEste cavalo não está registrado."));
            return;
        }

        if (!data.getOwnerId().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                    + "&cVocê não é o dono deste cavalo."));
            return;
        }

        switch (itemType) {
            case HorseItemRegistry.TYPE_NAME_TAG -> startNaming(player, horse, data);
            case HorseItemRegistry.TYPE_ELIXIR   -> useElixir(player, horse, data, held);
        }
    }

    private void startNaming(Player player, AbstractHorse horse, HorseData data) {
        if (namingSessionManager.hasPendingSession(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c[Cavalaria] Você já tem uma sessão de nomeação ativa."));
            return;
        }
        namingSessionManager.startSession(player, horse, data);
    }

    private void useElixir(Player player, AbstractHorse horse, HorseData data, ItemStack item) {
        if (data.getLevel() >= horseManager.getConfig().getMaxLevel()) {
            player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                    + "&e" + (data.getName() != null ? data.getName() : "Seu cavalo")
                    + " &ejá está no nível máximo!"));
            return;
        }

        // Consume one elixir
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        horseManager.addXp(horse, data, horseManager.getConfig().getXpPerElixir());
        player.sendMessage(TextUtil.color(horseManager.getConfig().getPrefix()
                + "&f+100 XP &apara &e"
                + (data.getName() != null ? data.getName() : "seu cavalo") + "&a!"));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
