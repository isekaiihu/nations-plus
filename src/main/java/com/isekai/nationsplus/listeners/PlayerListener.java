package com.isekai.nationsplus.listeners;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.ClaimChunk;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

public class PlayerListener implements Listener {

    private final NationsPlus plugin;

    public PlayerListener(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        ClaimChunk from = ClaimChunk.fromLocation(event.getFrom());
        ClaimChunk to = ClaimChunk.fromLocation(event.getTo());

        String fromTownId = plugin.getTownManager().getClaimOwner(from);
        String toTownId = plugin.getTownManager().getClaimOwner(to);

        if (java.util.Objects.equals(fromTownId, toTownId)) return;

        // Configure title times from config
        Title.Times times = Title.Times.times(
            Duration.ofMillis(250),
            Duration.ofMillis(1500),
            Duration.ofMillis(500)
        );

        if (toTownId != null) {
            Town town = plugin.getTownManager().getTown(toTownId);
            if (town != null) {
                // Get nation name if town is in a nation
                String nationSuffix = "";
                if (town.getNationId() != null) {
                    var nation = plugin.getNationManager().getNation(town.getNationId());
                    if (nation != null) {
                        nationSuffix = nation.getName();
                    }
                }

                Component titleComp = FontUtils.colorize("&#FFD700⚑ " + town.getName());
                Component subtitle = nationSuffix.isEmpty()
                    ? FontUtils.colorize("&#AAAAAA" + FontUtils.toSmallCaps("Town Territory"))
                    : FontUtils.colorize("&#AAAAAA" + FontUtils.toSmallCaps("Nation") + ": &f" + nationSuffix);

                player.showTitle(Title.title(titleComp, subtitle, times));
            }
        } else if (fromTownId != null) {
            Component titleComp = FontUtils.colorize("&#AAAAAA☁ " + FontUtils.toSmallCaps("Wilderness"));
            Component subtitle = FontUtils.colorize("&#777777" + FontUtils.toSmallCaps("Unclaimed Territory"));
            player.showTitle(Title.title(titleComp, subtitle, times));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getMarriageManager().getProfile(event.getPlayer().getUniqueId());
    }
}
