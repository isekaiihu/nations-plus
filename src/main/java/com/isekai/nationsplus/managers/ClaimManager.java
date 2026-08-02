package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;

import java.util.*;

public class ClaimManager {

    private final NationsPlus plugin;

    public ClaimManager(NationsPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempt to claim a chunk for a town. Returns a result string key or null on success.
     */
    public String tryClaim(UUID player, ClaimChunk chunk) {
        Town town = plugin.getTownManager().getPlayerTown(player);
        if (town == null) return "error.not-in-town";

        Role role = town.getRole(player);
        if (role == null || !role.canClaim()) return "error.not-leader";

        if (plugin.getTownManager().isClaimed(chunk)) return "claim.already-claimed";

        int freeClaims = plugin.getEconomyManager().getFreeClaims();
        int maxClaims = plugin.getTownManager().getMaxClaims(town);
        int totalAllowed = freeClaims + town.getBonusClaimBlocks();

        if (town.getClaimCount() >= maxClaims) return "claim.no-claims-left";

        // If beyond free claims, charge money
        if (town.getClaimCount() >= totalAllowed) {
            double cost = plugin.getEconomyManager().getClaimCost();
            if (town.getBank() < cost) {
                // Try player wallet
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
                if (p != null && plugin.getEconomyManager().canAfford(p, cost)) {
                    plugin.getEconomyManager().withdraw(p, cost);
                } else {
                    return "error.not-enough-money";
                }
            } else {
                town.withdraw(cost);
            }
        }

        plugin.getTownManager().addClaim(town.getId(), chunk);
        return null; // success
    }

    public String tryUnclaim(UUID player, ClaimChunk chunk) {
        Town town = plugin.getTownManager().getPlayerTown(player);
        if (town == null) return "error.not-in-town";

        Role role = town.getRole(player);
        if (role == null || !role.canClaim()) return "error.not-leader";

        if (!town.hasClaim(chunk)) return "claim.not-claimed";

        plugin.getTownManager().removeClaim(town.getId(), chunk);
        return null;
    }

    public boolean canBuild(UUID player, ClaimChunk chunk) {
        String townId = plugin.getTownManager().getClaimOwner(chunk);
        if (townId == null) return true; // unclaimed

        Town town = plugin.getTownManager().getTown(townId);
        if (town == null) return true;

        // Town members can always build
        if (town.hasMember(player)) return true;

        // Check if town allows block placement for outsiders
        if (town.getFlags().isBlockPlace()) return true;

        // Check war: if at war, check grace period
        return isWarPermitted(player, town, "block-place");
    }

    public boolean canBreak(UUID player, ClaimChunk chunk) {
        String townId = plugin.getTownManager().getClaimOwner(chunk);
        if (townId == null) return true;

        Town town = plugin.getTownManager().getTown(townId);
        if (town == null) return true;

        if (town.hasMember(player)) return true;
        if (town.getFlags().isBlockBreak()) return true;

        return isWarPermitted(player, town, "block-break");
    }

    public boolean canInteract(UUID player, ClaimChunk chunk) {
        String townId = plugin.getTownManager().getClaimOwner(chunk);
        if (townId == null) return true;

        Town town = plugin.getTownManager().getTown(townId);
        if (town == null) return true;

        if (town.hasMember(player)) return true;
        if (town.getFlags().isInteract()) return true;

        return false;
    }

    public boolean canPvP(UUID attacker, UUID defender, ClaimChunk chunk) {
        String townId = plugin.getTownManager().getClaimOwner(chunk);
        if (townId == null) return true;

        Town town = plugin.getTownManager().getTown(townId);
        if (town == null) return true;

        // PVP law applies to everyone (including members)
        if (!town.getFlags().isPvp()) {
            // Check if at war — war overrides pvp flag after grace
            return isWarPermitted(attacker, town, "pvp");
        }
        return true;
    }

    private boolean isWarPermitted(UUID player, Town claimedTown, String action) {
        if (claimedTown.isNeutral()) return false;

        Nation claimedNation = claimedTown.getNationId() != null ?
            plugin.getNationManager().getNation(claimedTown.getNationId()) : null;
        if (claimedNation == null || claimedNation.isNeutral()) return false;

        Nation playerNation = plugin.getNationManager().getPlayerNation(player);
        if (playerNation == null) return false;

        WarData war = plugin.getWarManager().getWarBetween(playerNation.getId(), claimedNation.getId());
        if (war == null || !war.isActive()) return false;

        // Must be past grace period
        if (war.isInGracePeriod()) return false;

        // Check config permission for this action during war
        return switch (action) {
            case "block-break" -> plugin.getConfig().getBoolean("war.allow-block-break-after-grace", true);
            case "block-place" -> plugin.getConfig().getBoolean("war.allow-block-break-after-grace", true);
            case "pvp" -> plugin.getConfig().getBoolean("war.allow-pvp-after-grace", true);
            default -> false;
        };
    }
}
