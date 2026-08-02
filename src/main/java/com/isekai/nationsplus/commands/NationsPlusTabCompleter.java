package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.Role;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class NationsPlusTabCompleter implements TabCompleter {

    private final NationsPlus plugin;

    public NationsPlusTabCompleter(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            case "town", "t" -> completeTown(args);
            case "nation", "n" -> completeNation(args);
            case "war" -> completeWar(args);
            case "marry" -> completeOnlinePlayers(args);
            case "marriage" -> completeMarriage(args);
            case "gender" -> completeGender(args);
            case "earthadmin", "ea" -> Collections.emptyList();
            case "tnc" -> args.length == 1 ? filter(List.of("reload"), args[0]) : Collections.emptyList();
            case "claim" -> completeClaim(args);
            case "migrate" -> completeMigrate(args);
            case "hug", "kiss", "reproduce", "divorce" -> Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private List<String> completeTown(String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "gui", "invite", "kick", "accept", "deny",
                "leave", "spawn", "setspawn", "claim", "unclaim", "deposit", "withdraw",
                "info", "list", "role", "settings", "laws"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "invite", "kick" -> filterOnlinePlayers(args[1]);
                case "info" -> filter(plugin.getTownManager().getAllTownNames(), args[1]);
                case "role" -> filterOnlinePlayers(args[1]);
                case "deposit", "withdraw" -> filter(List.of("100", "500", "1000", "5000", "10000"), args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("role")) {
            return filter(Arrays.stream(Role.values())
                .filter(r -> !r.isLeader())
                .map(r -> r.name().toLowerCase())
                .collect(Collectors.toList()), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> completeNation(String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "gui", "invite", "kick", "spawn",
                "setspawn", "info", "list", "deposit", "withdraw", "neutral"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "invite", "kick" -> filter(plugin.getTownManager().getAllTownNames(), args[1]);
                case "info" -> filter(plugin.getNationManager().getAllNationNames(), args[1]);
                case "deposit", "withdraw" -> filter(List.of("100", "500", "1000", "5000", "10000"), args[1]);
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    private List<String> completeWar(String[] args) {
        if (args.length == 1) return filter(List.of("declare", "surrender", "status"), args[0]);
        if (args.length == 2) return filter(plugin.getNationManager().getAllNationNames(), args[1]);
        return Collections.emptyList();
    }

    private List<String> completeMarriage(String[] args) {
        if (args.length == 1) return filter(List.of("accept", "deny", "info"), args[0]);
        return Collections.emptyList();
    }

    private List<String> completeGender(String[] args) {
        if (args.length == 1) return filter(plugin.getMarriageManager().getAllowedGenders(), args[0]);
        return Collections.emptyList();
    }

    private List<String> completeClaim(String[] args) {
        if (args.length == 1) return filter(List.of("give", "remove"), args[0]);
        if (args.length == 2) return filterOnlinePlayers(args[1]);
        if (args.length == 3) return filter(List.of("1", "5", "10", "50", "100"), args[2]);
        return Collections.emptyList();
    }

    private List<String> completeOnlinePlayers(String[] args) {
        if (args.length == 1) return filterOnlinePlayers(args[0]);
        return Collections.emptyList();
    }

    private List<String> completeMigrate(String[] args) {
        if (args.length == 1) return filter(List.of("Towny"), args[0]);
        return Collections.emptyList();
    }

    private List<String> filterOnlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
}
