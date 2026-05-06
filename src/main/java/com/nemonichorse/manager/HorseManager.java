package com.nemonichorse.manager;

import com.nemonichorse.NemonicHorse;
import com.nemonichorse.config.HorseConfig;
import com.nemonichorse.hook.MMOCoreHook;
import com.nemonichorse.model.HorseData;
import com.nemonichorse.model.HorseRace;
import com.nemonichorse.model.HorseRarity;
import com.nemonichorse.repository.HorseRepository;
import com.nemonichorse.skill.HorseSkill;
import com.nemonichorse.util.AttributeUtil;
import com.nemonichorse.util.ParticleUtil;
import com.nemonichorse.util.TextUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central manager: owns the in-memory cache, drives XP/leveling,
 * applies stats, and dispatches skill activation.
 */
public final class HorseManager {

    // PDC keys stamped on the horse entity for fast identification
    private final NamespacedKey keyOwner;
    private final NamespacedKey keyRarity;
    private final NamespacedKey keyRegistered;

    private final ConcurrentHashMap<UUID, HorseData> cache = new ConcurrentHashMap<>();

    private final NemonicHorse plugin;
    private final HorseConfig config;
    private final HorseRepository repository;
    private final SkillManager skillManager;
    private final MMOCoreHook mmoCoreHook;

    private BukkitTask saveTask;

    public HorseManager(NemonicHorse plugin, HorseConfig config, HorseRepository repository,
                        SkillManager skillManager, MMOCoreHook mmoCoreHook) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.skillManager = skillManager;
        this.mmoCoreHook = mmoCoreHook;

        this.keyOwner      = new NamespacedKey(plugin, "owner");
        this.keyRarity     = new NamespacedKey(plugin, "rarity");
        this.keyRegistered = new NamespacedKey(plugin, "registered");
    }

    public void startSaveTask() {
        saveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::saveAllDirty,
                config.getSaveIntervalTicks(),
                config.getSaveIntervalTicks());
    }

    public void shutdown() {
        if (saveTask != null) saveTask.cancel();
        // Synchronous final save on shutdown
        List<HorseData> dirty = cache.values().stream()
                .filter(HorseData::isDirty).collect(Collectors.toList());
        if (!dirty.isEmpty()) {
            repository.saveBatch(dirty);
        }
        cache.clear();
    }

    // ── Cache access ──────────────────────────────────────────────────

    @Nullable
    public HorseData getIfLoaded(UUID horseEntityUuid) {
        return cache.get(horseEntityUuid);
    }

    /**
     * Returns cached data or loads synchronously from DB if the horse has a registered PDC stamp.
     * Use this on interactive paths (commands, item-use events) where a brief DB call is acceptable.
     * High-frequency paths (move, combat) should keep using getIfLoaded().
     */
    @Nullable
    public HorseData tryLoadSync(AbstractHorse entity) {
        UUID id = entity.getUniqueId();
        HorseData cached = cache.get(id);
        if (cached != null) return cached;
        if (!isRegistered(entity)) return null;

        Optional<HorseData> fromDb = repository.findById(id);
        if (fromDb.isEmpty()) return null;

        HorseData data = fromDb.get();
        cache.put(id, data);
        applyStats(entity, data);
        updateDisplay(entity, data);
        return data;
    }

    public Optional<HorseData> get(UUID horseEntityUuid) {
        return Optional.ofNullable(cache.get(horseEntityUuid));
    }

    /**
     * Returns existing cached/DB data or creates a new record.
     * Must be called from the main thread (PDC access).
     */
    public HorseData loadOrCreate(AbstractHorse entity, UUID ownerId,
                                  HorseRarity rarity, HorseRace race) {
        UUID id = entity.getUniqueId();
        HorseData cached = cache.get(id);
        if (cached != null) return cached;

        // Try loading from DB synchronously (called from lifecycle events)
        Optional<HorseData> fromDb = repository.findById(id);
        if (fromDb.isPresent()) {
            cache.put(id, fromDb.get());
            applyStats(entity, fromDb.get());
            updateDisplay(entity, fromDb.get());
            return fromDb.get();
        }

        // Create new record
        HorseData newData = new HorseData(id, ownerId, rarity, race);
        stampPdc(entity, ownerId, rarity);
        cache.put(id, newData);
        applyStats(entity, newData);
        updateDisplay(entity, newData);

        // Async write — no blocking the main thread for new horse creation
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.save(newData);
        });
        return newData;
    }

    /**
     * Attempts to load a horse from the DB into cache when its chunk loads.
     * Must be called from the main thread.
     */
    public void tryLoad(AbstractHorse entity) {
        UUID id = entity.getUniqueId();
        if (cache.containsKey(id)) {
            // Already cached — just reapply visual state
            applyStats(entity, cache.get(id));
            updateDisplay(entity, cache.get(id));
            return;
        }
        // Quick check via PDC — only hit DB for registered horses
        if (!isRegistered(entity)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<HorseData> fromDb = repository.findById(id);
            fromDb.ifPresent(data -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                cache.put(id, data);
                if (entity.isValid()) {
                    applyStats(entity, data);
                    updateDisplay(entity, data);
                }
            }));
        });
    }

    public void saveAndEvict(UUID horseEntityUuid) {
        HorseData data = cache.remove(horseEntityUuid);
        if (data != null && data.isDirty()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    repository.save(data));
        }
    }

    public boolean isRegistered(AbstractHorse entity) {
        return entity.getPersistentDataContainer()
                .has(keyRegistered, PersistentDataType.BYTE);
    }

    // ── Progression ───────────────────────────────────────────────────

    public void addXp(AbstractHorse horse, HorseData data, double amount) {
        if (data.getLevel() >= config.getMaxLevel()) return;
        data.addXp(amount);

        double needed = config.xpForLevel(data.getLevel());
        if (data.getXp() >= needed) {
            levelUp(horse, data);
        }
    }

    private void levelUp(AbstractHorse horse, HorseData data) {
        double needed = config.xpForLevel(data.getLevel());
        data.setXp(data.getXp() - needed);
        data.setLevel(data.getLevel() + 1);

        applyStats(horse, data);
        updateDisplay(horse, data);
        ParticleUtil.levelUpEffect(horse.getLocation());
        horse.getWorld().playSound(horse.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Notify rider
        horse.getPassengers().stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .findFirst()
                .ifPresent(rider -> {
                    rider.sendMessage(TextUtil.format(
                            "&6&l✦ LEVEL UP! &e{name} &6chegou ao nível &e{level}&6!",
                            "name", data.getName() != null ? data.getName() : "Seu cavalo",
                            "level", data.getLevel()));
                    rider.sendMessage(TextUtil.color("&7  Vida, velocidade e pulo aumentados."));
                    mmoCoreHook.giveMainXp(rider, config.getMmoCoreXpOnLevelUp());
                });

        data.markDirty();
    }

    // ── Stats application ─────────────────────────────────────────────

    public void applyStats(AbstractHorse horse, HorseData data) {
        double rarityMult = data.getRarity().getAttributeMultiplier();
        double raceHealth = data.getRace().getHealthMultiplier();
        double raceSpeed  = data.getRace().getSpeedMultiplier();
        double raceJump   = data.getRace().getJumpMultiplier();
        int level = data.getLevel();

        double maxHp = config.computeHealth(level, rarityMult, raceHealth);
        double speed = config.computeSpeed(level, rarityMult, raceSpeed);
        double jump  = config.computeJump(level, rarityMult, raceJump);

        AttributeUtil.setBase(horse, Attribute.MAX_HEALTH, maxHp);
        AttributeUtil.setBase(horse, Attribute.MOVEMENT_SPEED, speed);
        AttributeUtil.setBase(horse, Attribute.JUMP_STRENGTH, jump);

        // Ensure current HP doesn't exceed new max
        if (horse.getHealth() > maxHp) {
            horse.setHealth(maxHp);
        }
    }

    public void updateDisplay(AbstractHorse horse, HorseData data) {
        horse.customName(TextUtil.buildHorseDisplayName(data));
        horse.setCustomNameVisible(true);
    }

    // ── Skill activation ──────────────────────────────────────────────

    public boolean activateSkill(Player rider, AbstractHorse horse, HorseData data) {
        String skillId = data.getEquippedSkillId();
        if (skillId == null) {
            rider.sendMessage(TextUtil.color(config.getPrefix() + "&cNenhuma habilidade equipada. Use /horse skill."));
            return false;
        }

        HorseSkill skill = skillManager.getSkill(skillId);
        if (skill == null) return false;

        if (data.getLevel() < skill.getRequiredLevel()) {
            rider.sendMessage(TextUtil.color(config.getPrefix() + "&f" + skill.getDisplayName()
                    + " &crequere o cavalo no &fNível " + skill.getRequiredLevel() + "&c."));
            return false;
        }

        if (!rider.hasPermission(skill.getPermission())) {
            rider.sendMessage(TextUtil.color(config.getPrefix() + "&cVocê não desbloqueou essa habilidade."));
            return false;
        }

        if (isOnCooldown(data, skillId)) {
            long remaining = getRemainingCooldownSeconds(data, skillId);
            rider.sendMessage(TextUtil.format(config.getPrefix() + "&c{skill} em recarga! &8({remaining}s)",
                    "skill", skill.getDisplayName(), "remaining", remaining));
            return false;
        }

        if (!mmoCoreHook.consumeMana(rider, skill.getManaCost())) {
            rider.sendMessage(TextUtil.format(config.getPrefix() + "&cMana insuficiente! &8({cost} necessário)",
                    "cost", (int) skill.getManaCost()));
            return false;
        }

        data.getSkillCooldowns().put(skillId, System.currentTimeMillis());
        data.markDirty();
        skill.activate(rider, horse, data);
        return true;
    }

    private boolean isOnCooldown(HorseData data, String skillId) {
        Long lastUsed = data.getSkillCooldowns().get(skillId);
        if (lastUsed == null) return false;
        HorseSkill skill = skillManager.getSkill(skillId);
        return skill != null && System.currentTimeMillis() - lastUsed < skill.getCooldownMillis();
    }

    private long getRemainingCooldownSeconds(HorseData data, String skillId) {
        Long lastUsed = data.getSkillCooldowns().get(skillId);
        if (lastUsed == null) return 0;
        HorseSkill skill = skillManager.getSkill(skillId);
        if (skill == null) return 0;
        long remainingMs = skill.getCooldownMillis() - (System.currentTimeMillis() - lastUsed);
        return Math.max(0, remainingMs / 1000);
    }

    // ── Bond system ───────────────────────────────────────────────────

    public void addBondXp(HorseData data, double amount) {
        data.setBondXp(data.getBondXp() + amount);
        double needed = (data.getBondLevel() + 1) * 200.0;
        if (data.getBondXp() >= needed && data.getBondLevel() < 10) {
            data.setBondXp(data.getBondXp() - needed);
            data.setBondLevel(data.getBondLevel() + 1);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void stampPdc(AbstractHorse entity, UUID ownerId, HorseRarity rarity) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(keyRegistered, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyOwner,      PersistentDataType.STRING, ownerId.toString());
        pdc.set(keyRarity,     PersistentDataType.STRING, rarity.name());
    }

    private void saveAllDirty() {
        List<HorseData> dirty = cache.values().stream()
                .filter(HorseData::isDirty)
                .collect(Collectors.toList());
        if (!dirty.isEmpty()) {
            repository.saveBatch(dirty);
        }
    }

    // ── Expose for listeners / GUI ────────────────────────────────────

    public HorseConfig getConfig()         { return config; }
    public SkillManager getSkillManager()  { return skillManager; }
    public MMOCoreHook getMmoCoreHook()    { return mmoCoreHook; }

    /** Find horse entity UUID from a player who is currently riding one. */
    @Nullable
    public AbstractHorse getRiddenHorse(Player player) {
        Entity vehicle = player.getVehicle();
        return vehicle instanceof AbstractHorse h ? h : null;
    }
}
