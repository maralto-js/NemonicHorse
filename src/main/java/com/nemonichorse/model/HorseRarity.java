package com.nemonichorse.model;

import org.bukkit.Color;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

public enum HorseRarity {

    COMMON   ("Comum",    "&f",    1.00, Particle.CLOUD,         Color.WHITE),
    UNCOMMON ("Incomum",  "&a",    1.15, Particle.HAPPY_VILLAGER, Color.GREEN),
    RARE     ("Raro",     "&9",    1.35, Particle.ENCHANT,        Color.BLUE),
    EPIC     ("Épico",    "&5",    1.60, Particle.DRAGON_BREATH,  Color.PURPLE),
    LEGENDARY("Lendário", "&6&l",  2.00, Particle.END_ROD,        Color.ORANGE);

    private final String displayName;
    private final String colorCode;
    private final double attributeMultiplier;
    private final Particle levelParticle;
    private final Color particleColor;

    HorseRarity(String displayName, String colorCode, double attributeMultiplier,
                Particle levelParticle, Color particleColor) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.attributeMultiplier = attributeMultiplier;
        this.levelParticle = levelParticle;
        this.particleColor = particleColor;
    }

    public String getDisplayName()        { return displayName; }
    public String getColorCode()          { return colorCode; }
    public double getAttributeMultiplier(){ return attributeMultiplier; }
    public Particle getLevelParticle()    { return levelParticle; }
    public Color getParticleColor()       { return particleColor; }

    // ── Breeding probability table ────────────────────────────────────
    //
    // Each entry: {minRarityOrdinal, cumulativeThresholds[]}
    // Index i in thresholds → values()[minOrdinal + i]
    //
    // Combinations (lo=lower ordinal, hi=higher ordinal):
    //   C+C  → C:70%  U:25%  R:05%
    //   C+U  → C:50%  U:40%  R:10%
    //   C+R  → C:25%  U:45%  R:25%  E:05%
    //   C+E  → U:35%  R:40%  E:20%  L:05%
    //   C+L  → U:25%  R:45%  E:25%  L:05%
    //   U+U  → U:60%  R:30%  E:10%
    //   U+R  → U:25%  R:50%  E:20%  L:05%
    //   U+E  → R:35%  E:45%  L:20%
    //   U+L  → R:25%  E:45%  L:30%
    //   R+R  → R:50%  E:35%  L:15%
    //   R+E  → R:20%  E:50%  L:30%
    //   R+L  → R:10%  E:45%  L:45%
    //   E+E  → E:60%  L:40%
    //   E+L  → E:25%  L:75%
    //   L+L  → E:15%  L:85%

    private record BreedOutcome(int minOrd, double[] cum) {}

    private static final BreedOutcome[][] BREED_TABLE = new BreedOutcome[5][5];

    static {
        BREED_TABLE[0][0] = new BreedOutcome(0, new double[]{0.70, 0.95, 1.00});
        BREED_TABLE[0][1] = new BreedOutcome(0, new double[]{0.50, 0.90, 1.00});
        BREED_TABLE[0][2] = new BreedOutcome(0, new double[]{0.25, 0.70, 0.95, 1.00});
        BREED_TABLE[0][3] = new BreedOutcome(1, new double[]{0.35, 0.75, 0.95, 1.00});
        BREED_TABLE[0][4] = new BreedOutcome(1, new double[]{0.25, 0.70, 0.95, 1.00});
        BREED_TABLE[1][1] = new BreedOutcome(1, new double[]{0.60, 0.90, 1.00});
        BREED_TABLE[1][2] = new BreedOutcome(1, new double[]{0.25, 0.75, 0.95, 1.00});
        BREED_TABLE[1][3] = new BreedOutcome(2, new double[]{0.35, 0.80, 1.00});
        BREED_TABLE[1][4] = new BreedOutcome(2, new double[]{0.25, 0.70, 1.00});
        BREED_TABLE[2][2] = new BreedOutcome(2, new double[]{0.50, 0.85, 1.00});
        BREED_TABLE[2][3] = new BreedOutcome(2, new double[]{0.20, 0.70, 1.00});
        BREED_TABLE[2][4] = new BreedOutcome(2, new double[]{0.10, 0.55, 1.00});
        BREED_TABLE[3][3] = new BreedOutcome(3, new double[]{0.60, 1.00});
        BREED_TABLE[3][4] = new BreedOutcome(3, new double[]{0.25, 1.00});
        BREED_TABLE[4][4] = new BreedOutcome(3, new double[]{0.15, 1.00});
        // Mirror lower triangle (table is symmetric)
        for (int lo = 0; lo < 5; lo++) {
            for (int hi = lo + 1; hi < 5; hi++) {
                BREED_TABLE[hi][lo] = BREED_TABLE[lo][hi];
            }
        }
    }

    /**
     * Determines foal rarity from two parent rarities using a weighted probability table.
     * Replaces the old broken avgOrdinal approach.
     */
    public static HorseRarity fromBreeding(HorseRarity p1, HorseRarity p2) {
        int lo = Math.min(p1.ordinal(), p2.ordinal());
        int hi = Math.max(p1.ordinal(), p2.ordinal());
        BreedOutcome outcome = BREED_TABLE[lo][hi];
        double roll = ThreadLocalRandom.current().nextDouble();
        for (int i = 0; i < outcome.cum().length; i++) {
            if (roll < outcome.cum()[i]) {
                return values()[outcome.minOrd() + i];
            }
        }
        return values()[outcome.minOrd() + outcome.cum().length - 1];
    }

    public static HorseRarity fromName(String name) {
        if (name == null) return COMMON;
        for (HorseRarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return COMMON;
    }
}
