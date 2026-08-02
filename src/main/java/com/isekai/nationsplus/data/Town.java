package com.isekai.nationsplus.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Town {

    private final String id;
    private String name;
    private String searchKey; // lowercase, color-stripped
    private UUID leader;
    private final Map<UUID, Role> members;
    private final Set<ClaimChunk> claims;
    private TownFlags flags;
    private Location spawn;
    private double bank;
    private int bonusClaimBlocks;
    private String nationId;
    private boolean neutral;
    private long createdAt;
    private String color;     // Color name (e.g. "red", "blue")
    private String colorHex;  // Hex value (e.g. "#FF4444")

    public Town(String id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.searchKey = stripColors(name).toLowerCase();
        this.leader = leader;
        this.members = new HashMap<>();
        this.claims = new HashSet<>();
        this.flags = new TownFlags();
        this.bank = 0;
        this.bonusClaimBlocks = 0;
        this.neutral = false;
        this.createdAt = System.currentTimeMillis();
        this.color = null;
        this.colorHex = null;
        members.put(leader, Role.KING);
    }

    private Town(String id) {
        this.id = id;
        this.name = "";
        this.searchKey = "";
        this.leader = null;
        this.members = new HashMap<>();
        this.claims = new HashSet<>();
        this.flags = new TownFlags();
        this.bank = 0;
        this.bonusClaimBlocks = 0;
        this.neutral = false;
        this.createdAt = System.currentTimeMillis();
        this.color = null;
        this.colorHex = null;
    }

    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("search-key", searchKey);
        section.set("leader", leader.toString());
        section.set("bank", bank);
        section.set("bonus-claim-blocks", bonusClaimBlocks);
        section.set("neutral", neutral);
        section.set("created-at", createdAt);
        if (nationId != null) section.set("nation", nationId);
        if (color != null) section.set("color", color);
        if (colorHex != null) section.set("color-hex", colorHex);

        if (spawn != null) {
            section.set("spawn.world", spawn.getWorld().getName());
            section.set("spawn.x", spawn.getX());
            section.set("spawn.y", spawn.getY());
            section.set("spawn.z", spawn.getZ());
            section.set("spawn.yaw", spawn.getYaw());
            section.set("spawn.pitch", spawn.getPitch());
        }

        ConfigurationSection membersSection = section.createSection("members");
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            membersSection.set(entry.getKey().toString(), entry.getValue().name());
        }

        List<String> claimStrings = new ArrayList<>();
        for (ClaimChunk claim : claims) {
            claimStrings.add(claim.toString());
        }
        section.set("claims", claimStrings);

        flags.save(section.createSection("flags"));
    }

    public static Town load(String id, ConfigurationSection section) {
        Town town = new Town(id);
        town.name = section.getString("name", "Unknown");
        town.searchKey = section.getString("search-key", town.name.toLowerCase());
        town.leader = UUID.fromString(section.getString("leader", UUID.randomUUID().toString()));
        town.bank = section.getDouble("bank", 0);
        town.bonusClaimBlocks = section.getInt("bonus-claim-blocks", 0);
        town.neutral = section.getBoolean("neutral", false);
        town.createdAt = section.getLong("created-at", System.currentTimeMillis());
        town.nationId = section.getString("nation", null);
        town.color = section.getString("color", null);
        town.colorHex = section.getString("color-hex", null);

        if (section.contains("spawn")) {
            try {
                town.spawn = new Location(
                    Bukkit.getWorld(section.getString("spawn.world", "world")),
                    section.getDouble("spawn.x"),
                    section.getDouble("spawn.y"),
                    section.getDouble("spawn.z"),
                    (float) section.getDouble("spawn.yaw"),
                    (float) section.getDouble("spawn.pitch")
                );
            } catch (Exception ignored) {}
        }

        ConfigurationSection membersSection = section.getConfigurationSection("members");
        if (membersSection != null) {
            for (String key : membersSection.getKeys(false)) {
                town.members.put(UUID.fromString(key), Role.fromString(membersSection.getString(key)));
            }
        }

        for (String claimStr : section.getStringList("claims")) {
            ClaimChunk chunk = ClaimChunk.fromString(claimStr);
            if (chunk != null) town.claims.add(chunk);
        }

        town.flags = TownFlags.load(section.getConfigurationSection("flags"));
        return town;
    }

    private static String stripColors(String s) {
        return s.replaceAll("(?i)(&[0-9a-fk-or]|&#[a-f0-9]{6}|§[0-9a-fk-or])", "");
    }

    // ── Getters/Setters ──
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.searchKey = stripColors(name).toLowerCase(); }
    public String getSearchKey() { return searchKey; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Map<UUID, Role> getMembers() { return members; }
    public Set<ClaimChunk> getClaims() { return claims; }
    public TownFlags getFlags() { return flags; }
    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public double getBank() { return bank; }
    public void setBank(double bank) { this.bank = bank; }
    public int getBonusClaimBlocks() { return bonusClaimBlocks; }
    public void setBonusClaimBlocks(int blocks) { this.bonusClaimBlocks = blocks; }
    public String getNationId() { return nationId; }
    public void setNationId(String nationId) { this.nationId = nationId; }
    public boolean isNeutral() { return neutral; }
    public void setNeutral(boolean neutral) { this.neutral = neutral; }
    public long getCreatedAt() { return createdAt; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public void addMember(UUID uuid, Role role) { members.put(uuid, role); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean hasMember(UUID uuid) { return members.containsKey(uuid); }
    public Role getRole(UUID uuid) { return members.getOrDefault(uuid, null); }
    public int getMemberCount() { return members.size(); }
    public int getClaimCount() { return claims.size(); }

    public void deposit(double amount) { bank += amount; }
    public boolean withdraw(double amount) {
        if (bank < amount) return false;
        bank -= amount;
        return true;
    }

    public void addClaim(ClaimChunk chunk) { claims.add(chunk); }
    public void removeClaim(ClaimChunk chunk) { claims.remove(chunk); }
    public boolean hasClaim(ClaimChunk chunk) { return claims.contains(chunk); }

    public void addBonusClaims(int amount) { bonusClaimBlocks += amount; }
}