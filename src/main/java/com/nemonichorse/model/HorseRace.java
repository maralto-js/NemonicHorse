package com.nemonichorse.model;

public enum HorseRace {

    PLAINS   ("Planícies",  "Velocidade aumentada",      1.10, 1.00, 1.00),
    HIGHLAND ("Montanhas",  "Pulo aumentado",             1.00, 1.00, 1.15),
    JUNGLE   ("Selva",      "Reduz dano de queda",        1.00, 1.05, 1.00),
    DESERT   ("Deserto",    "Resistência a fogo",         1.00, 1.00, 1.05);

    private final String displayName;
    private final String traitDescription;
    private final double speedMultiplier;
    private final double healthMultiplier;
    private final double jumpMultiplier;

    HorseRace(String displayName, String traitDescription,
              double speedMultiplier, double healthMultiplier, double jumpMultiplier) {
        this.displayName = displayName;
        this.traitDescription = traitDescription;
        this.speedMultiplier = speedMultiplier;
        this.healthMultiplier = healthMultiplier;
        this.jumpMultiplier = jumpMultiplier;
    }

    public String getDisplayName()     { return displayName; }
    public String getTraitDescription(){ return traitDescription; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getHealthMultiplier(){ return healthMultiplier; }
    public double getJumpMultiplier()  { return jumpMultiplier; }

    public static HorseRace fromName(String name) {
        if (name == null) return PLAINS;
        for (HorseRace r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return PLAINS;
    }

    /** Assigns a race based on the biome the horse was tamed or spawned in. */
    public static HorseRace fromBiomeName(String biomeName) {
        if (biomeName == null) return PLAINS;
        String b = biomeName.toLowerCase();
        if (b.contains("mountain") || b.contains("peaks") || b.contains("hill")) return HIGHLAND;
        if (b.contains("jungle") || b.contains("forest"))                         return JUNGLE;
        if (b.contains("desert") || b.contains("badlands") || b.contains("savanna")) return DESERT;
        return PLAINS;
    }
}
