package com.isekai.nationsplus.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Town law flags — control what non-members can do in the town.
 * Members always follow PVP laws but can build/break freely.
 */
public class TownFlags {

    private boolean pvp;
    private boolean mobSpawning;
    private boolean blockBreak;
    private boolean blockPlace;
    private boolean interact;
    private boolean explosionDamage;

    public TownFlags() {
        this.pvp = false;
        this.mobSpawning = true;
        this.blockBreak = false;
        this.blockPlace = false;
        this.interact = false;
        this.explosionDamage = false;
    }

    public TownFlags(ConfigurationSection defaults) {
        this.pvp = defaults.getBoolean("pvp", false);
        this.mobSpawning = defaults.getBoolean("mob-spawning", true);
        this.blockBreak = defaults.getBoolean("block-break", false);
        this.blockPlace = defaults.getBoolean("block-place", false);
        this.interact = defaults.getBoolean("interact", false);
        this.explosionDamage = defaults.getBoolean("explosion-damage", false);
    }

    public void save(ConfigurationSection section) {
        section.set("pvp", pvp);
        section.set("mob-spawning", mobSpawning);
        section.set("block-break", blockBreak);
        section.set("block-place", blockPlace);
        section.set("interact", interact);
        section.set("explosion-damage", explosionDamage);
    }

    public static TownFlags load(ConfigurationSection section) {
        TownFlags flags = new TownFlags();
        if (section == null) return flags;
        flags.pvp = section.getBoolean("pvp", false);
        flags.mobSpawning = section.getBoolean("mob-spawning", true);
        flags.blockBreak = section.getBoolean("block-break", false);
        flags.blockPlace = section.getBoolean("block-place", false);
        flags.interact = section.getBoolean("interact", false);
        flags.explosionDamage = section.getBoolean("explosion-damage", false);
        return flags;
    }

    public Map<String, Boolean> toMap() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("pvp", pvp);
        map.put("mob-spawning", mobSpawning);
        map.put("block-break", blockBreak);
        map.put("block-place", blockPlace);
        map.put("interact", interact);
        map.put("explosion-damage", explosionDamage);
        return map;
    }

    public boolean toggle(String key) {
        return switch (key.toLowerCase()) {
            case "pvp" -> { pvp = !pvp; yield pvp; }
            case "mob-spawning" -> { mobSpawning = !mobSpawning; yield mobSpawning; }
            case "block-break" -> { blockBreak = !blockBreak; yield blockBreak; }
            case "block-place" -> { blockPlace = !blockPlace; yield blockPlace; }
            case "interact" -> { interact = !interact; yield interact; }
            case "explosion-damage" -> { explosionDamage = !explosionDamage; yield explosionDamage; }
            default -> false;
        };
    }

    // Getters
    public boolean isPvp() { return pvp; }
    public boolean isMobSpawning() { return mobSpawning; }
    public boolean isBlockBreak() { return blockBreak; }
    public boolean isBlockPlace() { return blockPlace; }
    public boolean isInteract() { return interact; }
    public boolean isExplosionDamage() { return explosionDamage; }
}
