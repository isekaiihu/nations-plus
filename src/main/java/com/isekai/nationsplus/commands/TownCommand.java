package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TownCommand implements CommandExecutor {

    private final NationsPlus plugin;

    // Available town colors with their hex values
    private static final Map<String, String> TOWN_COLORS = new LinkedHashMap<>();
    static {
        TOWN_COLORS.put("red", "#FF4444");
        TOWN_COLORS.put("dark_red", "#AA0000");
        TOWN_COLORS.put("orange", "#FF8800");
        TOWN_COLORS.put("gold", "#FFD700");
        TOWN_COLORS.put("yellow", "#FFFF44");
        TOWN_COLORS.put("green", "#44FF44");
        TOWN_COLORS.put("dark_green", "#00AA00");
        TOWN_COLORS.put("lime", "#88FF00");
        TOWN_COLORS.put("aqua", "#44FFFF");
        TOWN_COLORS.put("cyan", "#00CCCC");
        TOWN_COLORS.put("blue", "#4444FF");
        TOWN_COLORS.put("dark_blue", "#0000AA");
        TOWN_COLORS.put("light_blue", "#5599FF");
        TOWN_COLORS.put("purple", "#AA44FF");
        TOWN_COLORS.put("magenta", "#FF44FF");
        TOWN_COLORS.put("pink", "#FF88CC");
        TOWN_COLORS.put("amethyst", "#9966CC");
        TOWN_COLORS.put("white", "#FFFFFF");
        TOWN_COLORS.put("gray", "#AAAAAA");
        TOWN_COLORS.put("dark_gray", "#555555");
        TOWN_COLORS.put("black", "#333333");
        TOWN_COLORS.put("brown", "#8B4513");
        TOWN_COLORS.put("teal", "#008080");
        TOWN_COLORS.put("navy", "#000080");
    }

    public TownCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().getRaw("error.console-only")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "delete" -> handleDelete(player);
            case "gui" -> plugin.getTownGUI().openMainMenu(player);
            case "invite" -> handleInvite(player, args);
            case "kick" -> handleKick(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "spawn" -> handleSpawn(player);
            case "setspawn" -> handleSetSpawn(player);
            case "claim" -> handleClaim(player);
            case "unclaim" -> handleUnclaim(player);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "info" -> handleInfo(player, args);
            case "list" -> handleList(player);
            case "role" -> handleRole(player, args);
            case "color" -> handleColor(player, args);
            case "rename" -> handleRename(player);
            case "settings", "laws" -> plugin.getTownGUI().openSettingsMenu(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player) {
        if (!player.hasPermission("nationsplus.town.create")) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission")));
            return;
        }
        if (plugin.getTownManager().getPlayerTown(player.getUniqueId()) != null) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.already-in-town")));
            return;
        }
        List<String> allowedWorlds = plugin.getConfig().getStringList("general.allowed-worlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.wrong-world")));
            return;
        }
        double cost = plugin.getEconomyManager().getTownCreationCost();
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost))));
            return;
        }

        plugin.getAnvilGUI().openAnvil(player, "town-create-prompt", (p, name) -> {
            name = name.trim();
            if (!validateName(p, name)) return;
            if (plugin.getTownManager().getTownByName(name) != null) {
                p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-taken")));
                return;
            }
            plugin.getEconomyManager().withdraw(p, cost);
            Town town = plugin.getTownManager().createTown(name, p.getUniqueId());
            ClaimChunk chunk = ClaimChunk.fromLocation(p.getLocation());
            plugin.getTownManager().addClaim(town.getId(), chunk);
            town.setSpawn(p.getLocation());
            plugin.getTownManager().saveData();
            // Update dynmap
            plugin.getDynmapManager().updateTownClaims(town);
            plugin.getDynmapManager().updateTownIcon(town);
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.created", "{town}", name, "{cost}", String.format("%.0f", cost))));
        });
    }

    private void handleDelete(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        plugin.getAnvilGUI().openAnvil(player, "town-delete-prompt", (p, text) -> {
            String confirm = plugin.getConfig().getString("anvil.delete-confirm-text", "DELETE");
            if (!text.trim().equalsIgnoreCase(confirm)) return;
            String name = town.getName();
            plugin.getDynmapManager().removeTownMarker(town.getId());
            plugin.getTownManager().deleteTown(town.getId());
            plugin.getTownManager().saveData(); // Force immediate save
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.deleted", "{town}", name)));
        });
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/town invite <player>")); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.canManageMembers()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.player-not-found"))); return; }
        if (plugin.getTownManager().getPlayerTown(target.getUniqueId()) != null) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.already-in-town"))); return;
        }

        plugin.getInvites().put(target.getUniqueId(), town.getId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.invite-sent", "{player}", target.getName())));
        target.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.invited", "{town}", town.getName())));
    }

    private void handleAccept(Player player) {
        String townId = plugin.getInvites().remove(player.getUniqueId());
        if (townId == null) { player.sendMessage(FontUtils.colorize("&cɴᴏ ᴘᴇɴᴅɪɴɢ ɪɴᴠɪᴛᴇ.")); return; }
        Town town = plugin.getTownManager().getTown(townId);
        if (town == null) return;
        plugin.getTownManager().addMember(townId, player.getUniqueId(), Role.CITIZEN);
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.joined", "{player}", player.getName(), "{town}", town.getName())));
    }

    private void handleDeny(Player player) {
        plugin.getInvites().remove(player.getUniqueId());
        player.sendMessage(FontUtils.colorize("&cɪɴᴠɪᴛᴇ ᴅᴇɴɪᴇᴅ."));
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/town kick <player>")); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.canManageMembers()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.player-not-found"))); return; }
        if (!town.hasMember(target.getUniqueId())) return;

        plugin.getTownManager().removeMember(town.getId(), target.getUniqueId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.kicked", "{player}", target.getName())));
        target.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.kicked", "{player}", target.getName())));
    }

    private void handleLeave(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role != null && role.isLeader()) { player.sendMessage(FontUtils.colorize("&cʟᴇᴀᴅᴇʀs ᴍᴜsᴛ ᴅᴇʟᴇᴛᴇ ᴏʀ ᴛʀᴀɴsꜰᴇʀ ᴛʜᴇ ᴛᴏᴡɴ.")); return; }
        plugin.getTownManager().removeMember(town.getId(), player.getUniqueId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.left", "{player}", player.getName(), "{town}", town.getName())));
    }

    /**
     * Town spawn with countdown — teleports AFTER the countdown, no cooldown blocking.
     */
    private void handleSpawn(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null || town.getSpawn() == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }

        int countdown = plugin.getConfig().getInt("town.spawn-cooldown", 5);
        Location startLoc = player.getLocation().clone();

        // Send initial countdown message (NOT a cooldown error)
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("teleport.countdown", "{time}", String.valueOf(countdown))));

        final int[] remaining = {countdown};
        final int[] taskId = new int[1];
        taskId[0] = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (player.getLocation().getBlockX() != startLoc.getBlockX()
                || player.getLocation().getBlockY() != startLoc.getBlockY()
                || player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("teleport.cancelled")));
                plugin.getServer().getScheduler().cancelTask(taskId[0]);
                return;
            }
            if (!player.isOnline()) {
                plugin.getServer().getScheduler().cancelTask(taskId[0]);
                return;
            }
            remaining[0]--;
            if (remaining[0] > 0) {
                player.sendActionBar(FontUtils.colorize("&#FFD700" + FontUtils.toSmallCaps("Teleporting in") + " &f" + remaining[0] + "s"));
            } else {
                plugin.getServer().getScheduler().cancelTask(taskId[0]);
                player.teleport(town.getSpawn());
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.spawn-teleport")));
            }
        }, 20L, 20L);
    }

    private void handleSetSpawn(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        town.setSpawn(player.getLocation());
        plugin.getTownManager().saveData();
        plugin.getDynmapManager().updateTownIcon(town);
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.spawn-set")));
    }

    private void handleClaim(Player player) {
        ClaimChunk chunk = ClaimChunk.fromLocation(player.getLocation());
        String result = plugin.getClaimManager().tryClaim(player.getUniqueId(), chunk);
        if (result == null) {
            Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
            plugin.getDynmapManager().updateTownClaims(town);
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("claim.claimed",
                "{x}", String.valueOf(chunk.x()), "{z}", String.valueOf(chunk.z()),
                "{used}", String.valueOf(town.getClaimCount()),
                "{max}", String.valueOf(plugin.getTownManager().getMaxClaims(town)))));
        } else {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get(result)));
        }
    }

    private void handleUnclaim(Player player) {
        ClaimChunk chunk = ClaimChunk.fromLocation(player.getLocation());
        String result = plugin.getClaimManager().tryUnclaim(player.getUniqueId(), chunk);
        if (result == null) {
            Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
            plugin.getDynmapManager().updateTownClaims(town);
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("claim.unclaimed",
                "{x}", String.valueOf(chunk.x()), "{z}", String.valueOf(chunk.z()))));
        } else {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get(result)));
        }
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/town deposit <amount>")); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) return;
            if (!plugin.getEconomyManager().canAfford(player, amount)) {
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", amount))));
                return;
            }
            plugin.getEconomyManager().withdraw(player, amount);
            town.deposit(amount);
            plugin.getTownManager().saveData();
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.deposit", "{amount}", String.format("%.0f", amount), "{balance}", String.format("%.0f", town.getBank()))));
        } catch (NumberFormatException e) { player.sendMessage(FontUtils.colorize("&cɪɴᴠᴀʟɪᴅ ᴀᴍᴏᴜɴᴛ.")); }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/town withdraw <amount>")); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.canManageBank()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0 || !town.withdraw(amount)) {
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", amount))));
                return;
            }
            plugin.getEconomyManager().deposit(player, amount);
            plugin.getTownManager().saveData();
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.withdraw", "{amount}", String.format("%.0f", amount), "{balance}", String.format("%.0f", town.getBank()))));
        } catch (NumberFormatException e) { player.sendMessage(FontUtils.colorize("&cɪɴᴠᴀʟɪᴅ ᴀᴍᴏᴜɴᴛ.")); }
    }

    private void handleInfo(Player player, String[] args) {
        Town town;
        if (args.length >= 2) {
            town = plugin.getTownManager().getTownByName(args[1]);
        } else {
            town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        }
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.town-not-found"))); return; }
        String leader = Bukkit.getOfflinePlayer(town.getLeader()).getName();
        String nationName = "";
        if (town.getNationId() != null) {
            var nation = plugin.getNationManager().getNation(town.getNationId());
            if (nation != null) nationName = nation.getName();
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FFD700&l " + town.getName()));
        if (!nationName.isEmpty()) {
            player.sendMessage(FontUtils.colorize("&#AAAAAA ɴᴀᴛɪᴏɴ: &f" + nationName));
        }
        player.sendMessage(FontUtils.colorize("&#AAAAAA ʟᴇᴀᴅᴇʀ: &f" + (leader != null ? leader : "?")));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ᴍᴇᴍʙᴇʀs: &f" + town.getMemberCount()));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ᴄʟᴀɪᴍs: &f" + town.getClaimCount()));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ʙᴀɴᴋ: &f$" + String.format("%.0f", town.getBank())));
        if (town.getColor() != null) {
            player.sendMessage(FontUtils.colorize("&#AAAAAA ᴄᴏʟᴏʀ: &f" + town.getColor()));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void handleList(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FFD700&l ᴛᴏᴡɴ ʟɪsᴛ"));
        for (Town t : plugin.getTownManager().getAllTowns()) {
            player.sendMessage(FontUtils.colorize("&#AAAAAA • &f" + t.getName() + " &#AAAAAA(" + t.getMemberCount() + " ᴍᴇᴍʙᴇʀs)"));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void handleRole(Player player, String[] args) {
        if (args.length < 3) { player.sendMessage(FontUtils.colorize("&c/town role <player> <role>")); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role myRole = town.getRole(player.getUniqueId());
        if (myRole == null || !myRole.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !town.hasMember(target.getUniqueId())) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.player-not-found"))); return;
        }

        Role newRole = Role.fromString(args[2]);
        if (newRole == null) { player.sendMessage(FontUtils.colorize("&cɪɴᴠᴀʟɪᴅ ʀᴏʟᴇ.")); return; }
        if (newRole.isLeader()) { player.sendMessage(FontUtils.colorize("&cᴄᴀɴɴᴏᴛ ᴀssɪɢɴ ʟᴇᴀᴅᴇʀ ʀᴏʟᴇ.")); return; }

        town.addMember(target.getUniqueId(), newRole);
        plugin.getTownManager().saveData();
        String configRole = plugin.getConfig().getString("roles." + newRole.getConfigKey(), newRole.getConfigKey());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.role-changed", "{player}", target.getName(), "{role}", configRole)));
    }

    /**
     * /town color <colorName> — set the town's Dynmap area color.
     */
    private void handleColor(Player player, String[] args) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        if (args.length < 2) {
            // Show available colors
            StringBuilder sb = new StringBuilder();
            sb.append("&7&m─────────────────────────\n");
            sb.append("&#FFD700&l ᴀᴠᴀɪʟᴀʙʟᴇ ᴄᴏʟᴏʀs\n");
            for (Map.Entry<String, String> entry : TOWN_COLORS.entrySet()) {
                String hex = entry.getValue().replace("#", "");
                sb.append(" &#").append(hex).append("■ &f").append(entry.getKey()).append("\n");
            }
            sb.append("&7&m─────────────────────────\n");
            sb.append("&#AAAAAA ᴜsᴀɢᴇ: &f/town color <name>");
            for (String line : sb.toString().split("\n")) {
                player.sendMessage(FontUtils.colorize(line));
            }
            return;
        }

        String colorName = args[1].toLowerCase();
        String hex = TOWN_COLORS.get(colorName);
        if (hex == null) {
            // Try to parse as custom hex
            if (args[1].matches("#?[0-9a-fA-F]{6}")) {
                hex = args[1].startsWith("#") ? args[1] : "#" + args[1];
            } else {
                player.sendMessage(FontUtils.colorize("&cɪɴᴠᴀʟɪᴅ ᴄᴏʟᴏʀ! ᴜsᴇ &f/town color &cᴛᴏ sᴇᴇ ᴀᴠᴀɪʟᴀʙʟᴇ ᴄᴏʟᴏʀs."));
                return;
            }
        }

        town.setColor(colorName);
        town.setColorHex(hex);
        plugin.getTownManager().saveData();
        plugin.getDynmapManager().updateTownClaims(town);

        String hexClean = hex.replace("#", "");
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.color-changed", "{color}", "&#" + hexClean + colorName)));
    }

    /**
     * /town rename — opens anvil GUI to rename the town (costs money).
     */
    private void handleRename(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        double cost = plugin.getConfig().getDouble("economy.town-rename-cost", 2500.0);
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost))));
            return;
        }

        plugin.getAnvilGUI().openAnvil(player, "town-rename-prompt", (p, name) -> {
            name = name.trim();
            if (!validateName(p, name)) return;
            if (plugin.getTownManager().getTownByName(name) != null) {
                p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-taken")));
                return;
            }
            plugin.getEconomyManager().withdraw(p, cost);
            String oldName = town.getName();
            town.setName(name);
            plugin.getTownManager().saveData();
            plugin.getDynmapManager().updateTownClaims(town);
            plugin.getDynmapManager().updateTownIcon(town);
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.renamed", "{old}", oldName, "{town}", name, "{cost}", String.format("%.0f", cost))));
        });
    }

    private boolean validateName(Player player, String name) {
        String stripped = FontUtils.stripColors(name);
        int minLen = plugin.getConfig().getInt("general.min-name-length", 3);
        int maxLen = plugin.getConfig().getInt("general.max-name-length", 24);
        if (stripped.length() < minLen) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-too-short", "{min}", String.valueOf(minLen))));
            return false;
        }
        if (stripped.length() > maxLen) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-too-long", "{max}", String.valueOf(maxLen))));
            return false;
        }
        for (String blacklisted : plugin.getConfig().getStringList("general.blacklisted-names")) {
            if (stripped.equalsIgnoreCase(blacklisted)) {
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-blacklisted")));
                return false;
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FFD700&l ɴᴀᴛɪᴏɴsᴘʟᴜs — ᴛᴏᴡɴ ᴄᴏᴍᴍᴀɴᴅs"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town create &7- &#AAAAAAᴄʀᴇᴀᴛᴇ ᴀ ᴛᴏᴡɴ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town gui &7- &#AAAAAAᴏᴘᴇɴ ᴛᴏᴡɴ ᴍᴇɴᴜ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town invite <player> &7- &#AAAAAAɪɴᴠɪᴛᴇ ᴀ ᴘʟᴀʏᴇʀ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town spawn &7- &#AAAAAAᴛᴇʟᴇᴘᴏʀᴛ ᴛᴏ sᴘᴀᴡɴ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town claim &7- &#AAAAAAᴄʟᴀɪᴍ ᴄʜᴜɴᴋ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town color <color> &7- &#AAAAAAsᴇᴛ ᴍᴀᴘ ᴄᴏʟᴏʀ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town rename &7- &#AAAAAAʀᴇɴᴀᴍᴇ ʏᴏᴜʀ ᴛᴏᴡɴ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /town laws &7- &#AAAAAAᴇᴅɪᴛ ᴛᴏᴡɴ ʟᴀᴡs"));
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    public static Map<String, String> getTownColors() {
        return TOWN_COLORS;
    }
}