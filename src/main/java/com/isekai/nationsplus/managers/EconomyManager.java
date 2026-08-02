package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final NationsPlus plugin;
    private Economy economy;
    private boolean hasVault;

    public EconomyManager(NationsPlus plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            hasVault = true;
            plugin.getLogger().info("Vault economy hooked successfully!");
        } else {
            plugin.getLogger().warning("No economy provider found for Vault!");
        }
    }

    public double getBalance(Player player) {
        if (!hasVault || economy == null) return 0;
        return economy.getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (!hasVault || economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!hasVault || economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean canAfford(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public boolean hasVault() { return hasVault; }

    public double getTownCreationCost() { return plugin.getConfig().getDouble("economy.town-creation-cost", 5000.0); }
    public double getNationCreationCost() { return plugin.getConfig().getDouble("economy.nation-creation-cost", 150000.0); }
    public double getClaimCost() { return plugin.getConfig().getDouble("economy.claim-cost-per-chunk", 500.0); }
    public int getFreeClaims() { return plugin.getConfig().getInt("economy.free-claim-blocks", 10); }
    public double getWarCost() { return plugin.getConfig().getDouble("economy.war-declaration-cost", 25000.0); }
    public double getDivorceCost() { return plugin.getConfig().getDouble("economy.divorce-cost", 1000.0); }
}
