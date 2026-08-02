package com.isekai.nationsplus.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Nation {

    private final String id;
    private String name;
    private String searchKey;
    private UUID ruler;
    private final Set<String> townIds;
    private final Map<String, Relation> relations;
    private Location spawn;
    private double bank;
    private boolean neutral;
    private long createdAt;

    public Nation(String id, String name, UUID ruler) {
        this.id = id;
        this.name = name;
        this.searchKey = stripColors(name).toLowerCase();
        this.ruler = ruler;
        this.townIds = new HashSet<>();
        this.relations = new HashMap<>();
        this.bank = 0;
        this.neutral = false;
        this.createdAt = System.currentTimeMillis();
    }

    private Nation(String id) {
        this.id = id;
        this.name = "";
        this.searchKey = "";
        this.ruler = null;
        this.townIds = new HashSet<>();
        this.relations = new HashMap<>();
        this.bank = 0;
        this.neutral = false;
        this.createdAt = System.currentTimeMillis();
    }

    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("search-key", searchKey);
        section.set("ruler", ruler.toString());
        section.set("bank", bank);
        section.set("neutral", neutral);
        section.set("created-at", createdAt);
        section.set("towns", new ArrayList<>(townIds));

        if (spawn != null) {
            section.set("spawn.world", spawn.getWorld().getName());
            section.set("spawn.x", spawn.getX());
            section.set("spawn.y", spawn.getY());
            section.set("spawn.z", spawn.getZ());
            section.set("spawn.yaw", spawn.getYaw());
            section.set("spawn.pitch", spawn.getPitch());
        }

        ConfigurationSection relSection = section.createSection("relations");
        for (Map.Entry<String, Relation> entry : relations.entrySet()) {
            relSection.set(entry.getKey(), entry.getValue().name());
        }
    }

    public static Nation load(String id, ConfigurationSection section) {
        Nation nation = new Nation(id);
        nation.name = section.getString("name", "Unknown");
        nation.searchKey = section.getString("search-key", nation.name.toLowerCase());
        nation.ruler = UUID.fromString(section.getString("ruler", UUID.randomUUID().toString()));
        nation.bank = section.getDouble("bank", 0);
        nation.neutral = section.getBoolean("neutral", false);
        nation.createdAt = section.getLong("created-at", System.currentTimeMillis());
        nation.townIds.addAll(section.getStringList("towns"));

        if (section.contains("spawn")) {
            try {
                nation.spawn = new Location(
                    Bukkit.getWorld(section.getString("spawn.world", "world")),
                    section.getDouble("spawn.x"),
                    section.getDouble("spawn.y"),
                    section.getDouble("spawn.z"),
                    (float) section.getDouble("spawn.yaw"),
                    (float) section.getDouble("spawn.pitch")
                );
            } catch (Exception ignored) {}
        }

        ConfigurationSection relSection = section.getConfigurationSection("relations");
        if (relSection != null) {
            for (String key : relSection.getKeys(false)) {
                nation.relations.put(key, Relation.fromString(relSection.getString(key)));
            }
        }
        return nation;
    }

    private static String stripColors(String s) {
        return s.replaceAll("(?i)(&[0-9a-fk-or]|&#[a-f0-9]{6}|§[0-9a-fk-or])", "");
    }

    // Getters/Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.searchKey = stripColors(name).toLowerCase(); }
    public String getSearchKey() { return searchKey; }
    public UUID getRuler() { return ruler; }
    public void setRuler(UUID ruler) { this.ruler = ruler; }
    public Set<String> getTownIds() { return townIds; }
    public Map<String, Relation> getRelations() { return relations; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public double getBank() { return bank; }
    public void setBank(double bank) { this.bank = bank; }
    public boolean isNeutral() { return neutral; }
    public void setNeutral(boolean neutral) { this.neutral = neutral; }
    public long getCreatedAt() { return createdAt; }

    public void addTown(String townId) { townIds.add(townId); }
    public void removeTown(String townId) { townIds.remove(townId); }
    public boolean hasTown(String townId) { return townIds.contains(townId); }
    public int getTownCount() { return townIds.size(); }

    public Relation getRelation(String nationId) { return relations.getOrDefault(nationId, Relation.NEUTRAL); }
    public void setRelation(String nationId, Relation rel) {
        if (rel == Relation.NEUTRAL) relations.remove(nationId);
        else relations.put(nationId, rel);
    }
    public boolean isAtWarWith(String nationId) { return getRelation(nationId) == Relation.WAR; }
    public boolean isAllyWith(String nationId) { return getRelation(nationId) == Relation.ALLY; }

    public void deposit(double amount) { bank += amount; }
    public boolean withdraw(double amount) {
        if (bank < amount) return false;
        bank -= amount;
        return true;
    }
}
