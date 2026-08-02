package com.isekai.nationsplus.data;

import org.bukkit.configuration.ConfigurationSection;

public class WarData {

    private final String attackerId;
    private final String defenderId;
    private final long declaredAt;
    private final long graceEndsAt;
    private final long expiresAt;
    private boolean graceOver;
    private boolean active;

    public WarData(String attackerId, String defenderId, long gracePeriodMs, long maxDurationMs) {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.declaredAt = System.currentTimeMillis();
        this.graceEndsAt = declaredAt + gracePeriodMs;
        this.expiresAt = declaredAt + maxDurationMs;
        this.graceOver = false;
        this.active = true;
    }

    private WarData(String attackerId, String defenderId, long declaredAt, long graceEndsAt, long expiresAt, boolean graceOver, boolean active) {
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.declaredAt = declaredAt;
        this.graceEndsAt = graceEndsAt;
        this.expiresAt = expiresAt;
        this.graceOver = graceOver;
        this.active = active;
    }

    public void save(ConfigurationSection section) {
        section.set("attacker", attackerId);
        section.set("defender", defenderId);
        section.set("declared-at", declaredAt);
        section.set("grace-ends-at", graceEndsAt);
        section.set("expires-at", expiresAt);
        section.set("grace-over", graceOver);
        section.set("active", active);
    }

    public static WarData load(ConfigurationSection section) {
        return new WarData(
            section.getString("attacker"),
            section.getString("defender"),
            section.getLong("declared-at"),
            section.getLong("grace-ends-at"),
            section.getLong("expires-at"),
            section.getBoolean("grace-over", false),
            section.getBoolean("active", true)
        );
    }

    public String getWarKey() {
        return attackerId.compareTo(defenderId) < 0 ?
            attackerId + ":" + defenderId : defenderId + ":" + attackerId;
    }

    public boolean isInGracePeriod() { return !graceOver && System.currentTimeMillis() < graceEndsAt; }
    public boolean isExpired() { return System.currentTimeMillis() >= expiresAt; }
    public boolean involves(String nationId) { return attackerId.equals(nationId) || defenderId.equals(nationId); }
    public String getOpponent(String nationId) {
        return attackerId.equals(nationId) ? defenderId : attackerId;
    }

    public long getRemainingGraceMs() { return Math.max(0, graceEndsAt - System.currentTimeMillis()); }
    public long getRemainingMs() { return Math.max(0, expiresAt - System.currentTimeMillis()); }

    // Getters/Setters
    public String getAttackerId() { return attackerId; }
    public String getDefenderId() { return defenderId; }
    public long getDeclaredAt() { return declaredAt; }
    public long getGraceEndsAt() { return graceEndsAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isGraceOver() { return graceOver; }
    public void setGraceOver(boolean graceOver) { this.graceOver = graceOver; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
