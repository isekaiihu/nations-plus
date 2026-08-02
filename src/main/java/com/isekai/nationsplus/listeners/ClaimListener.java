package com.isekai.nationsplus.listeners;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.ClaimChunk;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClaimListener implements Listener {

    private final NationsPlus plugin;

    public ClaimListener(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClaimTool(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null) return;

        Material claimTool = Material.valueOf(plugin.getConfig().getString("town.claim-tool", "GOLDEN_SHOVEL"));
        if (event.getItem().getType() != claimTool) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        event.setCancelled(true);
        ClaimChunk chunk = ClaimChunk.fromLocation(player.getLocation());

        // Check if already claimed by player's town — unclaim
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town != null && town.hasClaim(chunk)) {
            if (player.isSneaking()) {
                String result = plugin.getClaimManager().tryUnclaim(player.getUniqueId(), chunk);
                if (result == null) {
                    player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("claim.unclaimed",
                        "{x}", String.valueOf(chunk.x()), "{z}", String.valueOf(chunk.z()))));
                } else {
                    player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get(result)));
                }
            } else {
                player.sendMessage(FontUtils.colorize("&#AAAAAA" + FontUtils.toSmallCaps("Shift+Right-click to unclaim.")));
            }
            return;
        }

        // Claim
        String result = plugin.getClaimManager().tryClaim(player.getUniqueId(), chunk);
        if (result == null) {
            town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("claim.claimed",
                "{x}", String.valueOf(chunk.x()), "{z}", String.valueOf(chunk.z()),
                "{used}", String.valueOf(town != null ? town.getClaimCount() : 0),
                "{max}", String.valueOf(town != null ? plugin.getTownManager().getMaxClaims(town) : 0))));
        } else {
            String msg = plugin.getMessageManager().get(result);
            // Add town name if relevant
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            if (townId != null) {
                Town claimedTown = plugin.getTownManager().getTown(townId);
                if (claimedTown != null) msg = msg.replace("{town}", claimedTown.getName());
            }
            player.sendMessage(FontUtils.colorize(msg));
        }
    }
}
