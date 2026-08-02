package com.isekai.nationsplus.listeners;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.ClaimChunk;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    private final NationsPlus plugin;

    public ProtectionListener(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("nationsplus.admin")) return;
        ClaimChunk chunk = ClaimChunk.fromLocation(event.getBlock().getLocation());
        if (!plugin.getClaimManager().canBreak(player.getUniqueId(), chunk)) {
            event.setCancelled(true);
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            Town town = townId != null ? plugin.getTownManager().getTown(townId) : null;
            String name = town != null ? town.getName() : "?";
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("protection.no-break", "{town}", name)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("nationsplus.admin")) return;
        ClaimChunk chunk = ClaimChunk.fromLocation(event.getBlock().getLocation());
        if (!plugin.getClaimManager().canBuild(player.getUniqueId(), chunk)) {
            event.setCancelled(true);
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            Town town = townId != null ? plugin.getTownManager().getTown(townId) : null;
            String name = town != null ? town.getName() : "?";
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("protection.no-build", "{town}", name)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission("nationsplus.admin")) return;
        ClaimChunk chunk = ClaimChunk.fromLocation(event.getClickedBlock().getLocation());
        if (!plugin.getClaimManager().canInteract(player.getUniqueId(), chunk)) {
            event.setCancelled(true);
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            Town town = townId != null ? plugin.getTownManager().getTown(townId) : null;
            String name = town != null ? town.getName() : "?";
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("protection.no-interact", "{town}", name)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;
        ClaimChunk chunk = ClaimChunk.fromLocation(victim.getLocation());
        if (!plugin.getClaimManager().canPvP(attacker.getUniqueId(), victim.getUniqueId(), chunk)) {
            event.setCancelled(true);
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            Town town = townId != null ? plugin.getTownManager().getTown(townId) : null;
            String name = town != null ? town.getName() : "?";
            attacker.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("protection.no-pvp", "{town}", name)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;

        ClaimChunk chunk = ClaimChunk.fromLocation(event.getLocation());
        String townId = plugin.getTownManager().getClaimOwner(chunk);
        if (townId == null) return;
        Town town = plugin.getTownManager().getTown(townId);
        if (town != null && !town.getFlags().isMobSpawning()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            ClaimChunk chunk = ClaimChunk.fromLocation(block.getLocation());
            String townId = plugin.getTownManager().getClaimOwner(chunk);
            if (townId == null) return false;
            Town town = plugin.getTownManager().getTown(townId);
            return town != null && !town.getFlags().isExplosionDamage();
        });
    }
}
