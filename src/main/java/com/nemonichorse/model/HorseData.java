package com.nemonichorse.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mutable data class representing a registered horse. Thread-safe for the dirty flag only. */
public final class HorseData {

    private final UUID horseId;
    private UUID ownerId;
    private String name;
    private HorseRarity rarity;
    private HorseRace race;
    private int level;
    private double xp;
    private int bondLevel;
    private double bondXp;
    private String equippedSkillId;
    private final Map<String, Long> skillCooldowns = new HashMap<>();
    private volatile boolean dirty;
    private final long createdAt;
    private long updatedAt;

    public HorseData(UUID horseId, UUID ownerId, HorseRarity rarity, HorseRace race) {
        this.horseId = horseId;
        this.ownerId = ownerId;
        this.rarity = rarity;
        this.race = race;
        this.level = 1;
        this.xp = 0;
        this.bondLevel = 0;
        this.bondXp = 0;
        this.equippedSkillId = null;
        this.dirty = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    /** Constructor used when loading from database. */
    public HorseData(UUID horseId, UUID ownerId, String name, HorseRarity rarity, HorseRace race,
                     int level, double xp, int bondLevel, double bondXp,
                     String equippedSkillId, Map<String, Long> skillCooldowns,
                     long createdAt, long updatedAt) {
        this.horseId = horseId;
        this.ownerId = ownerId;
        this.name = name;
        this.rarity = rarity;
        this.race = race;
        this.level = level;
        this.xp = xp;
        this.bondLevel = bondLevel;
        this.bondXp = bondXp;
        this.equippedSkillId = equippedSkillId;
        this.skillCooldowns.putAll(skillCooldowns);
        this.dirty = false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addXp(double amount) {
        this.xp += amount;
        markDirty();
    }

    public void markDirty() {
        this.dirty = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markClean() {
        this.dirty = false;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public UUID getHorseId()               { return horseId; }
    public UUID getOwnerId()               { return ownerId; }
    public String getName()                { return name; }
    public HorseRarity getRarity()         { return rarity; }
    public HorseRace getRace()             { return race; }
    public int getLevel()                  { return level; }
    public double getXp()                  { return xp; }
    public int getBondLevel()              { return bondLevel; }
    public double getBondXp()              { return bondXp; }
    public String getEquippedSkillId()     { return equippedSkillId; }
    public Map<String, Long> getSkillCooldowns() { return skillCooldowns; }
    public boolean isDirty()               { return dirty; }
    public long getCreatedAt()             { return createdAt; }
    public long getUpdatedAt()             { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setOwnerId(UUID ownerId)             { this.ownerId = ownerId; markDirty(); }
    public void setName(String name)                 { this.name = name; markDirty(); }
    public void setRarity(HorseRarity rarity)        { this.rarity = rarity; markDirty(); }
    public void setRace(HorseRace race)              { this.race = race; markDirty(); }
    public void setLevel(int level)                  { this.level = level; markDirty(); }
    public void setXp(double xp)                     { this.xp = xp; markDirty(); }
    public void setBondLevel(int bondLevel)          { this.bondLevel = bondLevel; markDirty(); }
    public void setBondXp(double bondXp)             { this.bondXp = bondXp; markDirty(); }
    public void setEquippedSkillId(String skillId)   { this.equippedSkillId = skillId; markDirty(); }
}
