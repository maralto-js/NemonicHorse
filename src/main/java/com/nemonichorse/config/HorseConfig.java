package com.nemonichorse.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class HorseConfig {

    private final int maxLevel;
    private final double xpPerBlock;
    private final double xpPerKill;
    private final double xpPerDamageTaken;
    private final double xpPerElixir;
    private final double mmoCoreXpOnLevelUp;

    private final double baseHealth;
    private final double healthPerLevel;
    private final double baseSpeed;
    private final double speedPerLevel;
    private final double maxSpeed;
    private final double baseJump;
    private final double jumpPerLevel;
    private final double maxJump;

    private final int namingMaxLength;
    private final boolean autoNameOnNull;
    private final List<String> blockedWords;

    private final long saveIntervalTicks;
    private final String prefix;

    public HorseConfig(FileConfiguration cfg) {
        maxLevel           = cfg.getInt("horse.max-level", 20);
        xpPerBlock         = cfg.getDouble("horse.xp-per-block", 0.5);
        xpPerKill          = cfg.getDouble("horse.xp-per-kill", 10.0);
        xpPerDamageTaken   = cfg.getDouble("horse.xp-per-damage-taken", 2.0);
        xpPerElixir        = cfg.getDouble("horse.xp-per-elixir", 100.0);
        mmoCoreXpOnLevelUp = cfg.getDouble("horse.mmocore-xp-on-levelup", 50.0);

        baseHealth    = cfg.getDouble("horse.stats.base-health", 15.0);
        healthPerLevel= cfg.getDouble("horse.stats.health-per-level", 1.25);
        baseSpeed     = cfg.getDouble("horse.stats.base-speed", 0.225);
        speedPerLevel = cfg.getDouble("horse.stats.speed-per-level", 0.004);
        maxSpeed      = cfg.getDouble("horse.stats.max-speed", 0.45);
        baseJump      = cfg.getDouble("horse.stats.base-jump", 0.70);
        jumpPerLevel  = cfg.getDouble("horse.stats.jump-per-level", 0.02);
        maxJump       = cfg.getDouble("horse.stats.max-jump", 2.0);

        namingMaxLength    = cfg.getInt("horse.naming.max-length", 24);
        autoNameOnNull     = cfg.getBoolean("horse.naming.auto-name-on-null", true);
        blockedWords       = cfg.getStringList("horse.naming.blocked-words");

        int saveMin = cfg.getInt("database.save-interval-minutes", 5);
        saveIntervalTicks = 20L * 60 * saveMin;

        prefix = cfg.getString("messages.prefix", "&6[&e✦ Cavalaria&6] &r");
    }

    public int getMaxLevel()              { return maxLevel; }
    public double getXpPerBlock()         { return xpPerBlock; }
    public double getXpPerKill()          { return xpPerKill; }
    public double getXpPerDamageTaken()   { return xpPerDamageTaken; }
    public double getXpPerElixir()        { return xpPerElixir; }
    public double getMmoCoreXpOnLevelUp() { return mmoCoreXpOnLevelUp; }

    public double getBaseHealth()         { return baseHealth; }
    public double getHealthPerLevel()     { return healthPerLevel; }
    public double getBaseSpeed()          { return baseSpeed; }
    public double getSpeedPerLevel()      { return speedPerLevel; }
    public double getMaxSpeed()           { return maxSpeed; }
    public double getBaseJump()           { return baseJump; }
    public double getJumpPerLevel()       { return jumpPerLevel; }
    public double getMaxJump()            { return maxJump; }

    public int getNamingMaxLength()       { return namingMaxLength; }
    public boolean isAutoNameOnNull()     { return autoNameOnNull; }
    public List<String> getBlockedWords() { return blockedWords; }

    public long getSaveIntervalTicks()    { return saveIntervalTicks; }
    public String getPrefix()            { return prefix; }

    public double xpForLevel(int level) {
        return level * 100.0;
    }

    public double computeHealth(int level, double rarityMult, double raceHealthMult) {
        return (baseHealth + level * healthPerLevel) * rarityMult * raceHealthMult;
    }

    public double computeSpeed(int level, double rarityMult, double raceSpeedMult) {
        return Math.min((baseSpeed + level * speedPerLevel) * rarityMult * raceSpeedMult, maxSpeed);
    }

    /** rarityMult was previously ignored — now applied so LEGENDARY jumps higher than COMMON. */
    public double computeJump(int level, double rarityMult, double raceJumpMult) {
        return Math.min((baseJump + level * jumpPerLevel) * rarityMult * raceJumpMult, maxJump);
    }
}
