package com.isekai.nationsplus;

import com.isekai.nationsplus.commands.*;
import com.isekai.nationsplus.gui.*;
import com.isekai.nationsplus.hooks.NationsPlusExpansion;
import com.isekai.nationsplus.listeners.*;
import com.isekai.nationsplus.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NationsPlus extends JavaPlugin {

    private static NationsPlus instance;

    private MessageManager messageManager;
    private EconomyManager economyManager;
    private TownManager townManager;
    private NationManager nationManager;
    private ClaimManager claimManager;
    private WarManager warManager;
    private MarriageManager marriageManager;
    private DynmapManager dynmapManager;

    private AnvilGUIManager anvilGUI;
    private TownGUI townGUI;
    private NationGUI nationGUI;
    private AdminGUI adminGUI;

    private final Map<UUID, String> invites = new HashMap<>();
    private final Map<String, String> nationInvites = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("");
        getLogger().info("╔═══════════════════════════════════════════════════════════════╗");
        getLogger().info("║                  NATIONSPLUS v1.0.0                           ║");
        getLogger().info("║     Towns · Nations · Dynmap · War · Marriage                  ║");
        getLogger().info("║                                                               ║");
        getLogger().info("║                  PLUGIN BY ISekai                              ║");
        getLogger().info("║                 Discord: akumasekai                            ║");
        getLogger().info("╚═══════════════════════════════════════════════════════════════╝");
        getLogger().info("");

        saveDefaultConfig();

        // Initialize managers
        this.messageManager = new MessageManager(this);
        this.economyManager = new EconomyManager(this);
        this.townManager = new TownManager(this);
        this.nationManager = new NationManager(this);
        this.claimManager = new ClaimManager(this);
        this.warManager = new WarManager(this);
        this.marriageManager = new MarriageManager(this);
        this.dynmapManager = new DynmapManager(this);

        // Initialize GUIs
        this.anvilGUI = new AnvilGUIManager(this);
        this.townGUI = new TownGUI(this);
        this.nationGUI = new NationGUI(this);
        this.adminGUI = new AdminGUI(this);

        // Register commands
        TownCommand townCmd = new TownCommand(this);
        NationCommand nationCmd = new NationCommand(this);
        WarCommand warCmd = new WarCommand(this);
        MarriageCommand marriageCmd = new MarriageCommand(this);
        AdminCommand adminCmd = new AdminCommand(this);
        NationsPlusTabCompleter tabCompleter = new NationsPlusTabCompleter(this);

        registerCommand("town", townCmd, tabCompleter);
        registerCommand("nation", nationCmd, tabCompleter);
        registerCommand("war", warCmd, tabCompleter);
        registerCommand("marry", marriageCmd, tabCompleter);
        registerCommand("divorce", marriageCmd, tabCompleter);
        registerCommand("marriage", marriageCmd, tabCompleter);
        registerCommand("gender", marriageCmd, tabCompleter);
        registerCommand("hug", marriageCmd, tabCompleter);
        registerCommand("kiss", marriageCmd, tabCompleter);
        registerCommand("reproduce", marriageCmd, tabCompleter);
        registerCommand("earthadmin", adminCmd, tabCompleter);
        registerCommand("tnc", adminCmd, tabCompleter);
        registerCommand("claim", adminCmd, tabCompleter);

        MigrateCommand migrateCmd = new MigrateCommand(this);
        registerCommand("migrate", migrateCmd, tabCompleter);

        // Register PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NationsPlusExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getLogger().info("NationsPlus enabled! Towns: " + townManager.getAllTowns().size() +
            " | Nations: " + nationManager.getAllNations().size());
    }

    private void registerCommand(String name, Object executor, NationsPlusTabCompleter tab) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
            cmd.setTabCompleter(tab);
        }
    }

    @Override
    public void onDisable() {
        if (warManager != null) warManager.shutdown();
        if (townManager != null) townManager.saveData();
        if (nationManager != null) nationManager.saveData();
        if (marriageManager != null) marriageManager.saveData();

        getLogger().info("NationsPlus disabled! Thank you for using NationsPlus by ISekai!");
    }

    public void reload() {
        reloadConfig();
        messageManager.reload();
        townManager.reload();
        nationManager.reload();
        marriageManager.reload();
        dynmapManager = new DynmapManager(this);
    }

    // Getters
    public static NationsPlus getInstance() { return instance; }
    public MessageManager getMessageManager() { return messageManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public TownManager getTownManager() { return townManager; }
    public NationManager getNationManager() { return nationManager; }
    public ClaimManager getClaimManager() { return claimManager; }
    public WarManager getWarManager() { return warManager; }
    public MarriageManager getMarriageManager() { return marriageManager; }
    public DynmapManager getDynmapManager() { return dynmapManager; }
    public AnvilGUIManager getAnvilGUI() { return anvilGUI; }
    public TownGUI getTownGUI() { return townGUI; }
    public NationGUI getNationGUI() { return nationGUI; }
    public AdminGUI getAdminGUI() { return adminGUI; }
    public Map<UUID, String> getInvites() { return invites; }
    public Map<String, String> getNationInvites() { return nationInvites; }
}
