package com.isekai.nationsplus.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String gender;
    private UUID spouse;
    private long lastReproduceTime;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.gender = null;
        this.spouse = null;
        this.lastReproduceTime = 0;
    }

    public void save(ConfigurationSection section) {
        if (gender != null) section.set("gender", gender);
        if (spouse != null) section.set("spouse", spouse.toString());
        section.set("last-reproduce", lastReproduceTime);
    }

    public static PlayerProfile load(UUID uuid, ConfigurationSection section) {
        PlayerProfile profile = new PlayerProfile(uuid);
        if (section == null) return profile;
        profile.gender = section.getString("gender", null);
        String spouseStr = section.getString("spouse", null);
        if (spouseStr != null && !spouseStr.isEmpty()) {
            try { profile.spouse = UUID.fromString(spouseStr); } catch (Exception ignored) {}
        }
        profile.lastReproduceTime = section.getLong("last-reproduce", 0);
        return profile;
    }

    public UUID getUuid() { return uuid; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public boolean hasGender() { return gender != null; }
    public UUID getSpouse() { return spouse; }
    public void setSpouse(UUID spouse) { this.spouse = spouse; }
    public boolean isMarried() { return spouse != null; }
    public long getLastReproduceTime() { return lastReproduceTime; }
    public void setLastReproduceTime(long time) { this.lastReproduceTime = time; }
}
