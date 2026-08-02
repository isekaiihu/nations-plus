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

import java.util.List;

public class NationCommand implements CommandExecutor {

    private final NationsPlus plugin;

    public NationCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().getRaw("error.console-only")));
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "delete" -> handleDelete(player);
            case "gui" -> plugin.getNationGUI().openMainMenu(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "kick" -> handleKick(player, args);
            case "spawn" -> handleSpawn(player);
            case "setspawn" -> handleSetSpawn(player);
            case "info" -> handleInfo(player, args);
            case "list" -> handleList(player);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "neutral" -> handleNeutral(player);
            case "rename" -> handleRename(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player) {
        if (!player.hasPermission("nationsplus.nation.create")) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission"))); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        if (!town.getRole(player.getUniqueId()).isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        if (town.getNationId() != null) { player.sendMessage(FontUtils.colorize("&cʏᴏᴜʀ ᴛᴏᴡɴ ɪs ᴀʟʀᴇᴀᴅʏ ɪɴ ᴀ ɴᴀᴛɪᴏɴ!")); return; }

        List<String> allowedWorlds = plugin.getConfig().getStringList("general.allowed-worlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.wrong-world"))); return;
        }

        double cost = plugin.getEconomyManager().getNationCreationCost();
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost)))); return;
        }

        plugin.getAnvilGUI().openAnvil(player, "nation-create-prompt", (p, name) -> {
            name = name.trim();
            String stripped = FontUtils.stripColors(name);
            int minLen = plugin.getConfig().getInt("general.min-name-length", 3);
            int maxLen = plugin.getConfig().getInt("general.max-name-length", 24);
            if (stripped.length() < minLen || stripped.length() > maxLen) {
                p.sendMessage(FontUtils.colorize("&cɴᴀᴍᴇ ᴍᴜsᴛ ʙᴇ " + minLen + "-" + maxLen + " ᴄʜᴀʀᴀᴄᴛᴇʀs.")); return;
            }
            if (plugin.getNationManager().getNationByName(stripped) != null) {
                p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-taken"))); return;
            }
            plugin.getEconomyManager().withdraw(p, cost);
            Nation nation = plugin.getNationManager().createNation(name, p.getUniqueId(), town.getId());
            plugin.getTownManager().saveData(); // Save town's nation reference
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.created", "{nation}", name, "{cost}", String.format("%.0f", cost))));
        });
    }

    private void handleDelete(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }
        if (!nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        plugin.getAnvilGUI().openAnvil(player, "nation-delete-prompt", (p, text) -> {
            if (!text.trim().equalsIgnoreCase(plugin.getConfig().getString("anvil.delete-confirm-text", "DELETE"))) return;
            String name = nation.getName();
            plugin.getDynmapManager().removeNationMarker(nation.getId());
            plugin.getNationManager().deleteNation(nation.getId());
            plugin.getNationManager().saveData(); // Force immediate save
            plugin.getTownManager().saveData(); // Save town nation references
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.deleted", "{nation}", name)));
        });
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/nation invite <town>")); return; }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || !nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Town target = plugin.getTownManager().getTownByName(args[1]);
        if (target == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.town-not-found"))); return; }
        if (target.getNationId() != null) { player.sendMessage(FontUtils.colorize("&cᴛʜᴀᴛ ᴛᴏᴡɴ ɪs ᴀʟʀᴇᴀᴅʏ ɪɴ ᴀ ɴᴀᴛɪᴏɴ.")); return; }

        plugin.getNationInvites().put(target.getId(), nation.getId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.town-invited", "{town}", target.getName(), "{nation}", nation.getName())));

        Player townLeader = Bukkit.getPlayer(target.getLeader());
        if (townLeader != null) {
            townLeader.sendMessage(FontUtils.colorize("&eʏᴏᴜʀ ᴛᴏᴡɴ ʜᴀs ʙᴇᴇɴ ɪɴᴠɪᴛᴇᴅ ᴛᴏ &f" + nation.getName() + "&e! /nation accept ᴏʀ /nation deny"));
        }
    }

    private void handleAccept(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town"))); return; }
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        String nationId = plugin.getNationInvites().remove(town.getId());
        if (nationId == null) { player.sendMessage(FontUtils.colorize("&cɴᴏ ᴘᴇɴᴅɪɴɢ ɴᴀᴛɪᴏɴ ɪɴᴠɪᴛᴇ.")); return; }

        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) return;

        plugin.getNationManager().addTownToNation(nationId, town.getId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.town-joined", "{town}", town.getName(), "{nation}", nation.getName())));

        Player ruler = Bukkit.getPlayer(nation.getRuler());
        if (ruler != null) {
            ruler.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.town-joined", "{town}", town.getName(), "{nation}", nation.getName())));
        }
    }

    private void handleDeny(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) return;
        plugin.getNationInvites().remove(town.getId());
        player.sendMessage(FontUtils.colorize("&cɴᴀᴛɪᴏɴ ɪɴᴠɪᴛᴇ ᴅᴇɴɪᴇᴅ."));
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/nation kick <town>")); return; }
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || !nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Town target = plugin.getTownManager().getTownByName(args[1]);
        if (target == null || !nation.hasTown(target.getId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.town-not-found"))); return; }

        plugin.getNationManager().removeTownFromNation(nation.getId(), target.getId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.town-kicked", "{town}", target.getName(), "{nation}", nation.getName())));
    }

    /**
     * Nation spawn with countdown — teleports AFTER the countdown.
     */
    private void handleSpawn(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || nation.getSpawn() == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }

        int countdown = plugin.getConfig().getInt("nation.spawn-cooldown", 10);
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
                player.teleport(nation.getSpawn());
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.spawn-teleport")));
            }
        }, 20L, 20L);
    }

    private void handleSetSpawn(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || !nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        nation.setSpawn(player.getLocation());
        plugin.getNationManager().saveData();
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.spawn-set")));
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) return;
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0 || !plugin.getEconomyManager().canAfford(player, amount)) return;
            plugin.getEconomyManager().withdraw(player, amount);
            nation.deposit(amount);
            plugin.getNationManager().saveData();
            player.sendMessage(FontUtils.colorize("&a$" + String.format("%.0f", amount) + " ᴅᴇᴘᴏsɪᴛᴇᴅ ᴛᴏ ɴᴀᴛɪᴏɴ ʙᴀɴᴋ."));
        } catch (NumberFormatException ignored) {}
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) return;
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || !nation.getRuler().equals(player.getUniqueId())) return;
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0 || !nation.withdraw(amount)) return;
            plugin.getEconomyManager().deposit(player, amount);
            plugin.getNationManager().saveData();
            player.sendMessage(FontUtils.colorize("&a$" + String.format("%.0f", amount) + " ᴡɪᴛʜᴅʀᴀᴡɴ ꜰʀᴏᴍ ɴᴀᴛɪᴏɴ ʙᴀɴᴋ."));
        } catch (NumberFormatException ignored) {}
    }

    private void handleNeutral(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null || !nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        nation.setNeutral(!nation.isNeutral());
        plugin.getNationManager().saveData();
        player.sendMessage(FontUtils.colorize(nation.isNeutral() ? "&a☮ ɴᴀᴛɪᴏɴ ɪs ɴᴏᴡ ɴᴇᴜᴛʀᴀʟ." : "&c⚔ ɴᴀᴛɪᴏɴ ɪs ɴᴏ ʟᴏɴɢᴇʀ ɴᴇᴜᴛʀᴀʟ."));
    }

    /**
     * /nation rename — opens anvil GUI to rename the nation (costs money).
     */
    private void handleRename(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }
        if (!nation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        double cost = plugin.getConfig().getDouble("economy.nation-rename-cost", 10000.0);
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost))));
            return;
        }

        plugin.getAnvilGUI().openAnvil(player, "nation-rename-prompt", (p, name) -> {
            name = name.trim();
            String stripped = FontUtils.stripColors(name);
            int minLen = plugin.getConfig().getInt("general.min-name-length", 3);
            int maxLen = plugin.getConfig().getInt("general.max-name-length", 24);
            if (stripped.length() < minLen || stripped.length() > maxLen) {
                p.sendMessage(FontUtils.colorize("&cɴᴀᴍᴇ ᴍᴜsᴛ ʙᴇ " + minLen + "-" + maxLen + " ᴄʜᴀʀᴀᴄᴛᴇʀs.")); return;
            }
            if (plugin.getNationManager().getNationByName(stripped) != null) {
                p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("anvil.name-taken"))); return;
            }
            plugin.getEconomyManager().withdraw(p, cost);
            String oldName = nation.getName();
            nation.setName(name);
            plugin.getNationManager().saveData();
            plugin.getDynmapManager().removeNationMarker(nation.getId());
            p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.renamed", "{old}", oldName, "{nation}", name, "{cost}", String.format("%.0f", cost))));
        });
    }

    private void handleInfo(Player player, String[] args) {
        Nation nation = args.length >= 2 ? plugin.getNationManager().getNationByName(args[1]) : plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.nation-not-found"))); return; }
        String ruler = Bukkit.getOfflinePlayer(nation.getRuler()).getName();
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#4488FF&l " + nation.getName()));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ʀᴜʟᴇʀ: &f" + (ruler != null ? ruler : "?")));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ᴛᴏᴡɴs: &f" + nation.getTownCount()));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ʙᴀɴᴋ: &f$" + String.format("%.0f", nation.getBank())));
        player.sendMessage(FontUtils.colorize("&#AAAAAA sᴛᴀᴛᴜs: " + (nation.isNeutral() ? "&a☮ ɴᴇᴜᴛʀᴀʟ" : "&f⚔ ᴀᴄᴛɪᴠᴇ")));
        // List towns
        if (nation.getTownCount() > 0) {
            StringBuilder townList = new StringBuilder("&#AAAAAA ᴛᴏᴡɴ ʟɪsᴛ: ");
            for (String townId : nation.getTownIds()) {
                Town t = plugin.getTownManager().getTown(townId);
                if (t != null) townList.append("&f").append(FontUtils.stripColors(t.getName())).append("&#AAAAAA, ");
            }
            String list = townList.toString();
            if (list.endsWith(", ")) list = list.substring(0, list.length() - 2);
            player.sendMessage(FontUtils.colorize(list));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void handleList(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#4488FF&l ɴᴀᴛɪᴏɴ ʟɪsᴛ"));
        for (Nation n : plugin.getNationManager().getAllNations()) {
            player.sendMessage(FontUtils.colorize("&#AAAAAA • &f" + n.getName() + " &#AAAAAA(" + n.getTownCount() + " ᴛᴏᴡɴs)"));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#4488FF&l ɴᴀᴛɪᴏɴsᴘʟᴜs — ɴᴀᴛɪᴏɴ ᴄᴏᴍᴍᴀɴᴅs"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation create &7- &#AAAAAAᴄʀᴇᴀᴛᴇ ᴀ ɴᴀᴛɪᴏɴ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation gui &7- &#AAAAAAᴏᴘᴇɴ ɴᴀᴛɪᴏɴ ᴍᴇɴᴜ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation invite <town> &7- &#AAAAAAɪɴᴠɪᴛᴇ ᴀ ᴛᴏᴡɴ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation accept &7- &#AAAAAAᴀᴄᴄᴇᴘᴛ ɴᴀᴛɪᴏɴ ɪɴᴠɪᴛᴇ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation deny &7- &#AAAAAAᴅᴇɴʏ ɴᴀᴛɪᴏɴ ɪɴᴠɪᴛᴇ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation spawn &7- &#AAAAAAᴛᴇʟᴇᴘᴏʀᴛ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /nation rename &7- &#AAAAAAʀᴇɴᴀᴍᴇ ʏᴏᴜʀ ɴᴀᴛɪᴏɴ"));
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }
}