package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarCommand implements CommandExecutor {

    private final NationsPlus plugin;

    public WarCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "declare" -> handleDeclare(player, args);
            case "surrender" -> handleSurrender(player, args);
            case "status" -> handleStatus(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleDeclare(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/war declare <nation>")); return; }
        if (!player.hasPermission("nationsplus.war.declare")) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission"))); return; }

        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }
        if (!myNation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }
        if (myNation.isNeutral()) { player.sendMessage(FontUtils.colorize("&cʏᴏᴜʀ ɴᴀᴛɪᴏɴ ɪs ɴᴇᴜᴛʀᴀʟ! ᴅɪsᴀʙʟᴇ ɴᴇᴜᴛʀᴀʟɪᴛʏ ꜰɪʀsᴛ.")); return; }

        Nation targetNation = plugin.getNationManager().getNationByName(args[1]);
        if (targetNation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.nation-not-found"))); return; }
        if (targetNation.getId().equals(myNation.getId())) { player.sendMessage(FontUtils.colorize("&cʏᴏᴜ ᴄᴀɴ'ᴛ ᴅᴇᴄʟᴀʀᴇ ᴡᴀʀ ᴏɴ ʏᴏᴜʀsᴇʟꜰ!")); return; }
        if (targetNation.isNeutral()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().getRaw("war.neutral-zone"))); return; }

        if (plugin.getWarManager().areAtWar(myNation.getId(), targetNation.getId())) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("war.already-at-war", "{nation}", targetNation.getName()))); return;
        }

        double cost = plugin.getEconomyManager().getWarCost();
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost)))); return;
        }
        plugin.getEconomyManager().withdraw(player, cost);

        plugin.getWarManager().declareWar(myNation.getId(), targetNation.getId());

        long graceHours = plugin.getConfig().getLong("war.grace-period-hours", 24);
        String graceMsg = plugin.getMessageManager().getRawFormatted("war.grace-period",
            "{time}", graceHours + "ʜ",
            "{nation1}", myNation.getName(), "{nation2}", targetNation.getName());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(FontUtils.colorize(graceMsg));
        }
    }

    private void handleSurrender(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(FontUtils.colorize("&c/war surrender <nation>")); return; }
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null || !myNation.getRuler().equals(player.getUniqueId())) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-leader"))); return; }

        Nation targetNation = plugin.getNationManager().getNationByName(args[1]);
        if (targetNation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.nation-not-found"))); return; }

        WarData war = plugin.getWarManager().getWarBetween(myNation.getId(), targetNation.getId());
        if (war == null || !war.isActive()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("war.not-at-war", "{nation}", targetNation.getName()))); return; }

        plugin.getWarManager().endWar(war.getWarKey());
    }

    private void handleStatus(Player player) {
        Nation myNation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (myNation == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation"))); return; }

        var wars = plugin.getWarManager().getWarsForNation(myNation.getId());
        if (wars.isEmpty()) { player.sendMessage(FontUtils.colorize("&aʏᴏᴜʀ ɴᴀᴛɪᴏɴ ɪs ᴀᴛ ᴘᴇᴀᴄᴇ.")); return; }

        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FF4444&l⚔ ᴀᴄᴛɪᴠᴇ ᴡᴀʀs"));
        for (WarData war : wars) {
            String opponent = war.getOpponent(myNation.getId());
            Nation opNation = plugin.getNationManager().getNation(opponent);
            String name = opNation != null ? opNation.getName() : opponent;
            String status = war.isInGracePeriod() ? "&eɢʀᴀᴄᴇ ᴘᴇʀɪᴏᴅ" : "&cᴀᴄᴛɪᴠᴇ";
            long remainHours = war.getRemainingMs() / 3600000;
            player.sendMessage(FontUtils.colorize("&#AAAAAA • &f" + name + " " + status + " &7(" + remainHours + "ʜ ʀᴇᴍᴀɪɴɪɴɢ)"));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FF4444&l ᴡᴀʀ ᴄᴏᴍᴍᴀɴᴅs"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /war declare <nation> &7- &#AAAAAAᴅᴇᴄʟᴀʀᴇ ᴡᴀʀ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /war surrender <nation> &7- &#AAAAAAsᴜʀʀᴇɴᴅᴇʀ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /war status &7- &#AAAAAAᴠɪᴇᴡ ᴀᴄᴛɪᴠᴇ ᴡᴀʀs"));
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }
}
