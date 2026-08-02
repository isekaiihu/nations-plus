package com.isekai.nationsplus.data;

public enum Role {
    KING(7, "king"),
    QUEEN(7, "queen"),
    RIGHT_HAND(6, "right-hand"),
    LEFT_HAND(5, "left-hand"),
    COMMANDER(4, "commander"),
    LIEUTENANT(3, "lieutenant"),
    COURTIER(2, "courtier"),
    CITIZEN(1, "citizen");

    private final int weight;
    private final String configKey;

    Role(int weight, String configKey) {
        this.weight = weight;
        this.configKey = configKey;
    }

    public int getWeight() { return weight; }
    public String getConfigKey() { return configKey; }

    public boolean isLeader() { return this == KING || this == QUEEN; }
    public boolean isHighRank() { return weight >= 5; }
    public boolean canManageMembers() { return weight >= 4; }
    public boolean canManageBank() { return weight >= 5; }
    public boolean canClaim() { return weight >= 3; }

    public boolean outranks(Role other) { return this.weight > other.weight; }

    public static Role fromString(String s) {
        if (s == null) return CITIZEN;
        try {
            return valueOf(s.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return CITIZEN;
        }
    }
}
