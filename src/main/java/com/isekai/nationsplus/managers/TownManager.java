package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TownManager {

    private final NationsPlus plugin;
    private final Map<String, Town> towns = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTownMap = new ConcurrentHashMap<>(); // player -> townId
    private final Map<ClaimChunk, String> claimMap = new ConcurrentHashMap<>(); // chunk -> townId
    private File dataFile;

    public TownManager(NationsPlus plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "towns.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        towns.clear();
        playerTownMap.clear();
        claimMap.clear();

        ConfigurationSection townsSection = config.getConfigurationSection("towns");
        if (townsSection != null) {
            for (String id : townsSection.getKeys(false)) {
                Town town = Town.load(id, townsSection.getConfigurationSection(id));
                towns.put(id, town);

                for (UUID member : town.getMembers().keySet()) {
                    playerTownMap.put(member, id);
                }
                for (ClaimChunk chunk : town.getClaims()) {
                    claimMap.put(chunk, id);
                }
            }
        }

        plugin.getLogger().info("Loaded " + towns.size() + " towns.");
    }

    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection townsSection = config.createSection("towns");
        for (Map.Entry<String, Town> entry : towns.entrySet()) {
            entry.getValue().save(townsSection.createSection(entry.getKey()));
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save towns data: " + e.getMessage());
        }
    }

    public Town createTown(String name, UUID leader) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Town town = new Town(id, name, leader);
        towns.put(id, town);
        playerTownMap.put(leader, id);
        saveData(); // Synchronous save to ensure persistence
        return town;
    }

    public void deleteTown(String townId) {
        Town town = towns.remove(townId);
        if (town == null) return;

        for (UUID member : town.getMembers().keySet()) {
            playerTownMap.remove(member);
        }
        for (ClaimChunk chunk : town.getClaims()) {
            claimMap.remove(chunk);
        }

        // Remove from nation
        if (town.getNationId() != null) {
            Nation nation = plugin.getNationManager().getNation(town.getNationId());
            if (nation != null) nation.removeTown(townId);
        }

        // Save synchronously to ensure deletion persists across restarts
        saveData();
        plugin.getDynmapManager().removeTownMarker(townId);
    }

    public void addMember(String townId, UUID player, Role role) {
        Town town = towns.get(townId);
        if (town == null) return;
        town.addMember(player, role);
        playerTownMap.put(player, townId);
        saveData();
    }

    public void removeMember(String townId, UUID player) {
        Town town = towns.get(townId);
        if (town == null) return;
        town.removeMember(player);
        playerTownMap.remove(player);
        saveData();
    }

    public void addClaim(String townId, ClaimChunk chunk) {
        Town town = towns.get(townId);
        if (town == null) return;
        town.addClaim(chunk);
        claimMap.put(chunk, townId);
        saveData();
        plugin.getDynmapManager().updateTownClaims(town);
    }

    public void removeClaim(String townId, ClaimChunk chunk) {
        Town town = towns.get(townId);
        if (town == null) return;
        town.removeClaim(chunk);
        claimMap.remove(chunk);
        saveData();
        plugin.getDynmapManager().updateTownClaims(town);
    }

    // ── Lookups ──
    public Town getTown(String id) { return towns.get(id); }
    public Town getTownByName(String name) {
        String lower = name.toLowerCase();
        return towns.values().stream()
            .filter(t -> t.getSearchKey().equalsIgnoreCase(lower) || t.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }
    public String getPlayerTownId(UUID player) { return playerTownMap.get(player); }
    public Town getPlayerTown(UUID player) {
        String id = playerTownMap.get(player);
        return id == null ? null : towns.get(id);
    }
    public String getClaimOwner(ClaimChunk chunk) { return claimMap.get(chunk); }
    public boolean isClaimed(ClaimChunk chunk) { return claimMap.containsKey(chunk); }
    public Collection<Town> getAllTowns() { return towns.values(); }
    public List<String> getAllTownNames() {
        return towns.values().stream().map(Town::getName).collect(Collectors.toList());
    }
    public int getMaxClaims(Town town) {
        return plugin.getConfig().getInt("town.max-claims", 500);
    }
    public int getMaxMembers() { return plugin.getConfig().getInt("town.max-members", 50); }

    public List<Town> searchTowns(String query) {
        String lower = query.toLowerCase();
        return towns.values().stream()
            .filter(t -> t.getSearchKey().contains(lower) || t.getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public void reload() {
        loadData();
    }
}