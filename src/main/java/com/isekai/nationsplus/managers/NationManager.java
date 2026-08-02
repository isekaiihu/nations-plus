package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.Nation;
import com.isekai.nationsplus.data.Relation;
import com.isekai.nationsplus.data.Town;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NationManager {

    private final NationsPlus plugin;
    private final Map<String, Nation> nations = new ConcurrentHashMap<>();
    private File dataFile;

    public NationManager(NationsPlus plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "nations.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        nations.clear();

        ConfigurationSection sec = config.getConfigurationSection("nations");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                nations.put(id, Nation.load(id, sec.getConfigurationSection(id)));
            }
        }
        plugin.getLogger().info("Loaded " + nations.size() + " nations.");
    }

    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("nations");
        for (Map.Entry<String, Nation> entry : nations.entrySet()) {
            entry.getValue().save(sec.createSection(entry.getKey()));
        }
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Failed to save nations: " + e.getMessage());
        }
    }

    public Nation createNation(String name, UUID ruler, String capitalTownId) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Nation nation = new Nation(id, name, ruler);
        nation.addTown(capitalTownId);
        nations.put(id, nation);

        Town town = plugin.getTownManager().getTown(capitalTownId);
        if (town != null) town.setNationId(id);

        // Save synchronously
        saveData();
        return nation;
    }

    public void deleteNation(String nationId) {
        Nation nation = nations.remove(nationId);
        if (nation == null) return;

        for (String townId : nation.getTownIds()) {
            Town town = plugin.getTownManager().getTown(townId);
            if (town != null) town.setNationId(null);
        }

        // Remove wars involving this nation
        plugin.getWarManager().removeWarsForNation(nationId);

        // Save synchronously to ensure deletion persists
        saveData();
        plugin.getDynmapManager().removeNationMarker(nationId);
    }

    public void addTownToNation(String nationId, String townId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        nation.addTown(townId);
        Town town = plugin.getTownManager().getTown(townId);
        if (town != null) town.setNationId(nationId);
        saveData();
    }

    public void removeTownFromNation(String nationId, String townId) {
        Nation nation = nations.get(nationId);
        if (nation == null) return;
        nation.removeTown(townId);
        Town town = plugin.getTownManager().getTown(townId);
        if (town != null) town.setNationId(null);
        saveData();
    }

    // ── Lookups ──
    public Nation getNation(String id) { return nations.get(id); }
    public Nation getNationByName(String name) {
        String lower = name.toLowerCase();
        return nations.values().stream()
            .filter(n -> n.getSearchKey().equalsIgnoreCase(lower) || n.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public Nation getPlayerNation(UUID player) {
        Town town = plugin.getTownManager().getPlayerTown(player);
        if (town == null || town.getNationId() == null) return null;
        return nations.get(town.getNationId());
    }

    public Collection<Nation> getAllNations() { return nations.values(); }
    public List<String> getAllNationNames() {
        return nations.values().stream().map(Nation::getName).collect(Collectors.toList());
    }

    public List<Nation> searchNations(String query) {
        String lower = query.toLowerCase();
        return nations.values().stream()
            .filter(n -> n.getSearchKey().contains(lower) || n.getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public int getMaxTowns() { return plugin.getConfig().getInt("nation.max-towns", 20); }

    public void reload() { loadData(); }
}