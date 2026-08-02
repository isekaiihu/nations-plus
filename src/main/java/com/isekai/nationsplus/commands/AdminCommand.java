package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.Nation;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final NationsPlus plugin;

    public AdminCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("earthadmin")) {
            if (!(sender instanceof Player player)) return true;
            if (!player.hasPermission("nationsplus.admin")) {
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission"))); return true;
            }

            // Sub-commands for force delete
            if (args.length >= 2) {
                switch (args[0].toLowerCase()) {
                    case "forcedelete-town", "fdt" -> {
                        handleForceDeleteTown(sender, args[1]);
                        return true;
                    }
                    case "forcedelete-nation", "fdn" -> {
                        handleForceDeleteNation(sender, args[1]);
                        return true;
                    }
                    case "forcedelete" -> {
                        if (args.length >= 3) {
                            if (args[1].equalsIgnoreCase("town")) {
                                handleForceDeleteTown(sender, args[2]);
                            } else if (args[1].equalsIgnoreCase("nation")) {
                                handleForceDeleteNation(sender, args[2]);
                            }
                        } else {
                            sender.sendMessage(FontUtils.colorize("&c/earthadmin forcedelete <town|nation> <name>"));
                        }
                        return true;
                    }
                }
            }

            plugin.getAdminGUI().openMainMenu(player);
            return true;
        }

        if (cmd.equals("tnc")) {
            if (!sender.hasPermission("nationsplus.admin")) {
                sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission"))); return true;
            }
            if (args.length >= 1) {
                switch (args[0].toLowerCase()) {
                    case "reload" -> {
                        plugin.reload();
                        sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.reload")));
                    }
                    case "forcedelete" -> {
                        if (args.length >= 3) {
                            if (args[1].equalsIgnoreCase("town")) {
                                handleForceDeleteTown(sender, args[2]);
                            } else if (args[1].equalsIgnoreCase("nation")) {
                                handleForceDeleteNation(sender, args[2]);
                            } else {
                                sender.sendMessage(FontUtils.colorize("&c/tnc forcedelete <town|nation> <name>"));
                            }
                        } else {
                            sender.sendMessage(FontUtils.colorize("&c/tnc forcedelete <town|nation> <name>"));
                        }
                    }
                    default -> sendAdminHelp(sender);
                }
            } else {
                sendAdminHelp(sender);
            }
            return true;
        }

        if (cmd.equals("claim")) {
            if (!sender.hasPermission("nationsplus.admin.claims")) {
                sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission"))); return true;
            }
            if (args.length < 3) { sender.sendMessage(FontUtils.colorize("&c/claim <give|remove> <player> <amount>")); return true; }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.player-not-found"))); return true; }

            var town = plugin.getTownManager().getPlayerTown(target.getUniqueId());
            if (town == null) { sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return true; }

            try {
                int amount = Integer.parseInt(args[2]);
                if (args[0].equalsIgnoreCase("give")) {
                    town.addBonusClaims(amount);
                    plugin.getTownManager().saveData();
                    sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.claim-given", "{amount}", String.valueOf(amount), "{player}", target.getName())));
                } else if (args[0].equalsIgnoreCase("remove")) {
                    town.setBonusClaimBlocks(Math.max(0, town.getBonusClaimBlocks() - amount));
                    plugin.getTownManager().saveData();
                    sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.claim-removed", "{amount}", String.valueOf(amount), "{player}", target.getName())));
                }
            } catch (NumberFormatException e) { sender.sendMessage(FontUtils.colorize("&cɪɴᴠᴀʟɪᴅ ɴᴜᴍʙᴇʀ.")); }
            return true;
        }
        return true;
    }

    private void handleForceDeleteTown(CommandSender sender, String name) {
        Town town = plugin.getTownManager().getTownByName(name);
        if (town == null) {
            sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.town-not-found")));
            return;
        }
        String townName = town.getName();
        String townId = town.getId();
        plugin.getDynmapManager().removeTownMarker(townId);
        plugin.getTownManager().deleteTown(townId);
        sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.force-deleted", "{name}", townName)));
        // Broadcast
        Bukkit.broadcast(FontUtils.colorize(plugin.getMessageManager().getRaw("admin.force-deleted").replace("{name}", townName)));
    }

    private void handleForceDeleteNation(CommandSender sender, String name) {
        Nation nation = plugin.getNationManager().getNationByName(name);
        if (nation == null) {
            sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.nation-not-found")));
            return;
        }
        String nationName = nation.getName();
        String nationId = nation.getId();
        plugin.getDynmapManager().removeNationMarker(nationId);
        plugin.getNationManager().deleteNation(nationId);
        sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.force-deleted", "{name}", nationName)));
        Bukkit.broadcast(FontUtils.colorize(plugin.getMessageManager().getRaw("admin.force-deleted").replace("{name}", nationName)));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        sender.sendMessage(FontUtils.colorize("&#FF4444&l ɴᴀᴛɪᴏɴsᴘʟᴜs ᴀᴅᴍɪɴ"));
        sender.sendMessage(FontUtils.colorize("&#AAAAAA /tnc reload &7- &#AAAAAAʀᴇʟᴏᴀᴅ ᴄᴏɴꜰɪɢ"));
        sender.sendMessage(FontUtils.colorize("&#AAAAAA /tnc forcedelete town <name> &7- &#AAAAAAꜰᴏʀᴄᴇ ᴅᴇʟᴇᴛᴇ ᴛᴏᴡɴ"));
        sender.sendMessage(FontUtils.colorize("&#AAAAAA /tnc forcedelete nation <name> &7- &#AAAAAAꜰᴏʀᴄᴇ ᴅᴇʟᴇᴛᴇ ɴᴀᴛɪᴏɴ"));
        sender.sendMessage(FontUtils.colorize("&#AAAAAA /earthadmin &7- &#AAAAAAᴏᴘᴇɴ ᴀᴅᴍɪɴ ɢᴜɪ"));
        sender.sendMessage(FontUtils.colorize("&#AAAAAA /claim give <player> <amount> &7- &#AAAAAAɢɪᴠᴇ ᴄʟᴀɪᴍ ʙʟᴏᴄᴋs"));
        sender.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }
}
