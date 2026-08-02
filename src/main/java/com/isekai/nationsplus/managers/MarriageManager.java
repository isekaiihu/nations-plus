package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.PlayerProfile;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarriageManager {

    private final NationsPlus plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingProposals = new ConcurrentHashMap<>(); // proposer -> target
    private final Map<UUID, Long> proposalTimestamps = new ConcurrentHashMap<>();
    private File dataFile;

    public MarriageManager(NationsPlus plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "profiles.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        profiles.clear();

        ConfigurationSection sec = config.getConfigurationSection("profiles");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                profiles.put(uuid, PlayerProfile.load(uuid, sec.getConfigurationSection(key)));
            }
        }
    }

    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection sec = config.createSection("profiles");
        for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
            entry.getValue().save(sec.createSection(entry.getKey().toString()));
        }
        try { config.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("Failed to save profiles: " + e.getMessage());
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, PlayerProfile::new);
    }

    public void setGender(UUID uuid, String gender) {
        getProfile(uuid).setGender(gender);
        saveDataAsync();
    }

    public boolean propose(UUID proposer, UUID target) {
        int timeout = plugin.getConfig().getInt("marriage.proposal-timeout-seconds", 120);
        pendingProposals.put(proposer, target);
        proposalTimestamps.put(proposer, System.currentTimeMillis() + (timeout * 1000L));
        return true;
    }

    public UUID getPendingProposal(UUID target) {
        for (Map.Entry<UUID, UUID> entry : pendingProposals.entrySet()) {
            if (entry.getValue().equals(target)) {
                Long expires = proposalTimestamps.get(entry.getKey());
                if (expires != null && System.currentTimeMillis() < expires) {
                    return entry.getKey();
                } else {
                    pendingProposals.remove(entry.getKey());
                    proposalTimestamps.remove(entry.getKey());
                }
            }
        }
        return null;
    }

    public void acceptProposal(UUID proposer, UUID target) {
        getProfile(proposer).setSpouse(target);
        getProfile(target).setSpouse(proposer);
        pendingProposals.remove(proposer);
        proposalTimestamps.remove(proposer);
        saveDataAsync();
    }

    public void denyProposal(UUID proposer) {
        pendingProposals.remove(proposer);
        proposalTimestamps.remove(proposer);
    }

    public void divorce(UUID player) {
        PlayerProfile profile = getProfile(player);
        UUID spouse = profile.getSpouse();
        if (spouse != null) {
            getProfile(spouse).setSpouse(null);
        }
        profile.setSpouse(null);
        saveDataAsync();
    }

    public boolean canReproduce(UUID player) {
        PlayerProfile profile = getProfile(player);
        if (!profile.isMarried()) return false;
        long cooldownDays = plugin.getConfig().getLong("marriage.reproduce-cooldown-days", 7);
        long cooldownMs = cooldownDays * 86400000L;
        return System.currentTimeMillis() - profile.getLastReproduceTime() >= cooldownMs;
    }

    public long getReproduceCooldownRemaining(UUID player) {
        PlayerProfile profile = getProfile(player);
        long cooldownDays = plugin.getConfig().getLong("marriage.reproduce-cooldown-days", 7);
        long cooldownMs = cooldownDays * 86400000L;
        long remaining = (profile.getLastReproduceTime() + cooldownMs) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void reproduce(UUID player) {
        PlayerProfile profile = getProfile(player);
        profile.setLastReproduceTime(System.currentTimeMillis());

        int bonus = plugin.getConfig().getInt("marriage.reproduce-claim-bonus", 2);

        // Find the town of the player and add bonus claims to the king
        Town town = plugin.getTownManager().getPlayerTown(player);
        if (town != null) {
            town.addBonusClaims(bonus);
        }

        saveDataAsync();
        plugin.getTownManager().saveData();
    }

    public boolean isSameGenderMarriageAllowed() {
        return plugin.getConfig().getBoolean("marriage.default-allow-same-gender-marriage", true);
    }

    public List<String> getAllowedGenders() {
        return plugin.getConfig().getStringList("marriage.allowed-genders");
    }

    public void performSpouseAction(Player player, String action) {
        PlayerProfile profile = getProfile(player.getUniqueId());
        if (!profile.isMarried()) return;

        Player spouse = Bukkit.getPlayer(profile.getSpouse());
        if (spouse == null || !spouse.isOnline()) return;

        String particleName = plugin.getConfig().getString("marriage.spouse-actions." + action + ".particle", "HEART");
        try {
            Particle particle = Particle.valueOf(particleName);
            player.getWorld().spawnParticle(particle, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
            spouse.getWorld().spawnParticle(particle, spouse.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
        } catch (Exception ignored) {}

        String msgKey = "marriage." + action;
        String msg = plugin.getMessageManager().getRawFormatted(msgKey,
            "{player}", player.getName(), "{spouse}", spouse.getName());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().distance(player.getLocation()) < 50 ||
                p.getLocation().distance(spouse.getLocation()) < 50) {
                p.sendMessage(FontUtils.colorize(msg));
            }
        }
    }

    public void reload() { loadData(); }

    private void saveDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }
}
