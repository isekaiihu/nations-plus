package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.Nation;
import com.isekai.nationsplus.data.Relation;
import com.isekai.nationsplus.data.WarData;
import com.isekai.nationsplus.utils.FontUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final NationsPlus plugin;
    private final Map<String, WarData> activeWars = new ConcurrentHashMap<>(); // warKey -> WarData
    private File dataFile;
    private BukkitTask warTickTask;

    public WarManager(NationsPlus plugin) {
        this.plugin = plugin;
        loadData();
        startWarTick();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "wars.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        activeWars.clear();

        ConfigurationSection sec = config.getConfigurationSection("wars");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                WarData war = WarData.load(sec.getConfigurationSection(key));
                if (war.isActive() && !war.isExpired()) {
                    activeWars.put(war.getWarKey(), war);
                }
            }
        }
        plugin.getLogger().info("Loaded " + activeWars.size() + " active wars.");
    }

    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("wars");
        int i = 0;
        for (WarData war : activeWars.values()) {
            war.save(sec.createSection("war-" + (i++)));
        }
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Failed to save wars: " + e.getMessage());
        }
    }

    public WarData declareWar(String attackerId, String defenderId) {
        long gracePeriodMs = plugin.getConfig().getLong("war.grace-period-hours", 24) * 3600000L;
        long maxDurationMs = plugin.getConfig().getLong("war.max-duration-hours", 168) * 3600000L;

        WarData war = new WarData(attackerId, defenderId, gracePeriodMs, maxDurationMs);
        activeWars.put(war.getWarKey(), war);

        // Set relations
        Nation attacker = plugin.getNationManager().getNation(attackerId);
        Nation defender = plugin.getNationManager().getNation(defenderId);
        if (attacker != null) attacker.setRelation(defenderId, Relation.WAR);
        if (defender != null) defender.setRelation(attackerId, Relation.WAR);

        // Broadcast
        String msg = plugin.getMessageManager().getRawFormatted("war.declared",
            "{nation1}", attacker != null ? attacker.getName() : attackerId,
            "{nation2}", defender != null ? defender.getName() : defenderId);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(FontUtils.colorize(msg));
        }

        saveDataAsync();
        plugin.getNationManager().saveData();
        return war;
    }

    public void endWar(String warKey) {
        WarData war = activeWars.remove(warKey);
        if (war == null) return;

        war.setActive(false);

        Nation attacker = plugin.getNationManager().getNation(war.getAttackerId());
        Nation defender = plugin.getNationManager().getNation(war.getDefenderId());

        if (attacker != null) attacker.setRelation(war.getDefenderId(), Relation.NEUTRAL);
        if (defender != null) defender.setRelation(war.getAttackerId(), Relation.NEUTRAL);

        String msg = plugin.getMessageManager().getRawFormatted("war.surrender-accepted",
            "{nation1}", attacker != null ? attacker.getName() : war.getAttackerId(),
            "{nation2}", defender != null ? defender.getName() : war.getDefenderId());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(FontUtils.colorize(msg));
        }

        saveDataAsync();
        plugin.getNationManager().saveData();
    }

    public WarData getWarBetween(String nationId1, String nationId2) {
        String key = nationId1.compareTo(nationId2) < 0 ?
            nationId1 + ":" + nationId2 : nationId2 + ":" + nationId1;
        return activeWars.get(key);
    }

    public boolean areAtWar(String nationId1, String nationId2) {
        WarData war = getWarBetween(nationId1, nationId2);
        return war != null && war.isActive();
    }

    public List<WarData> getWarsForNation(String nationId) {
        List<WarData> result = new ArrayList<>();
        for (WarData war : activeWars.values()) {
            if (war.involves(nationId) && war.isActive()) result.add(war);
        }
        return result;
    }

    public void removeWarsForNation(String nationId) {
        activeWars.entrySet().removeIf(e -> e.getValue().involves(nationId));
        saveDataAsync();
    }

    public Collection<WarData> getAllActiveWars() { return activeWars.values(); }

    private void startWarTick() {
        warTickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<String, WarData>> it = activeWars.entrySet().iterator();
            while (it.hasNext()) {
                WarData war = it.next().getValue();

                // Check grace period transition
                if (!war.isGraceOver() && !war.isInGracePeriod()) {
                    war.setGraceOver(true);
                    Nation a = plugin.getNationManager().getNation(war.getAttackerId());
                    Nation d = plugin.getNationManager().getNation(war.getDefenderId());
                    String msg = plugin.getMessageManager().getRawFormatted("war.grace-ended",
                        "{nation1}", a != null ? a.getName() : war.getAttackerId(),
                        "{nation2}", d != null ? d.getName() : war.getDefenderId());
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(FontUtils.colorize(msg));
                    }
                }

                // Check expiry (auto-peace)
                if (war.isExpired()) {
                    war.setActive(false);
                    Nation a = plugin.getNationManager().getNation(war.getAttackerId());
                    Nation d = plugin.getNationManager().getNation(war.getDefenderId());
                    if (a != null) a.setRelation(war.getDefenderId(), Relation.NEUTRAL);
                    if (d != null) d.setRelation(war.getAttackerId(), Relation.NEUTRAL);
                    it.remove();
                }
            }
        }, 20L * 60, 20L * 60); // check every minute
    }

    public void shutdown() {
        if (warTickTask != null) warTickTask.cancel();
        saveData();
    }

    private void saveDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }
}
