package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.PlayerProfile;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MarriageCommand implements CommandExecutor {

    private final NationsPlus plugin;

    public MarriageCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "marry" -> handlePropose(player, args);
            case "divorce" -> handleDivorce(player);
            case "marriage" -> {
                if (args.length == 0) { sendHelp(player); return true; }
                switch (args[0].toLowerCase()) {
                    case "accept" -> handleAccept(player);
                    case "deny" -> handleDeny(player);
                    case "info" -> handleInfo(player);
                    default -> sendHelp(player);
                }
            }
            case "gender" -> handleGender(player, args);
            case "hug" -> { plugin.getMarriageManager().performSpouseAction(player, "hug"); }
            case "kiss" -> { plugin.getMarriageManager().performSpouseAction(player, "kiss"); }
            case "reproduce" -> handleReproduce(player);
        }
        return true;
    }

    private void handlePropose(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage(FontUtils.colorize("&c/marry <player>")); return; }
        PlayerProfile profile = plugin.getMarriageManager().getProfile(player.getUniqueId());
        if (profile.isMarried()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.already-married"))); return; }
        if (!profile.hasGender()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("gender.must-set"))); return; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.player-not-found"))); return; }
        PlayerProfile targetProfile = plugin.getMarriageManager().getProfile(target.getUniqueId());
        if (targetProfile.isMarried()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().getRaw("marriage.already-married"))); return; }
        if (!targetProfile.hasGender()) { player.sendMessage(FontUtils.colorize("&cᴛʜᴀᴛ ᴘʟᴀʏᴇʀ ʜᴀsɴ'ᴛ sᴇᴛ ᴛʜᴇɪʀ ɢᴇɴᴅᴇʀ ʏᴇᴛ.")); return; }

        // Same-gender check
        if (!plugin.getMarriageManager().isSameGenderMarriageAllowed() &&
            profile.getGender() != null && profile.getGender().equals(targetProfile.getGender())) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.same-gender-denied"))); return;
        }

        plugin.getMarriageManager().propose(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.proposal-sent", "{player}", target.getName())));
        target.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.proposal-received", "{player}", player.getName())));
    }

    private void handleAccept(Player player) {
        UUID proposer = plugin.getMarriageManager().getPendingProposal(player.getUniqueId());
        if (proposer == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.no-pending"))); return; }

        plugin.getMarriageManager().acceptProposal(proposer, player.getUniqueId());
        String proposerName = Bukkit.getOfflinePlayer(proposer).getName();
        String msg = plugin.getMessageManager().getRawFormatted("marriage.accepted", "{player}", proposerName != null ? proposerName : "?", "{spouse}", player.getName());
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(FontUtils.colorize(msg));
    }

    private void handleDeny(Player player) {
        UUID proposer = plugin.getMarriageManager().getPendingProposal(player.getUniqueId());
        if (proposer == null) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.no-pending"))); return; }
        plugin.getMarriageManager().denyProposal(proposer);
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.denied", "{player}", player.getName())));
        Player prop = Bukkit.getPlayer(proposer);
        if (prop != null) prop.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.denied", "{player}", player.getName())));
    }

    private void handleDivorce(Player player) {
        PlayerProfile profile = plugin.getMarriageManager().getProfile(player.getUniqueId());
        if (!profile.isMarried()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.not-married"))); return; }

        double cost = plugin.getEconomyManager().getDivorceCost();
        if (!plugin.getEconomyManager().canAfford(player, cost)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-enough-money", "{cost}", String.format("%.0f", cost)))); return;
        }
        plugin.getEconomyManager().withdraw(player, cost);

        String spouseName = Bukkit.getOfflinePlayer(profile.getSpouse()).getName();
        plugin.getMarriageManager().divorce(player.getUniqueId());
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.divorced", "{player}", player.getName(), "{spouse}", spouseName != null ? spouseName : "?")));
    }

    private void handleReproduce(Player player) {
        PlayerProfile profile = plugin.getMarriageManager().getProfile(player.getUniqueId());
        if (!profile.isMarried()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.not-married"))); return; }
        if (!plugin.getMarriageManager().canReproduce(player.getUniqueId())) {
            long remaining = plugin.getMarriageManager().getReproduceCooldownRemaining(player.getUniqueId());
            long days = remaining / 86400000;
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("marriage.reproduce-cooldown", "{time}", days + " ᴅᴀʏs")));
            return;
        }

        Player spouse = Bukkit.getPlayer(profile.getSpouse());
        if (spouse == null || !spouse.isOnline()) { player.sendMessage(FontUtils.colorize("&csᴘᴏᴜsᴇ ᴍᴜsᴛ ʙᴇ ᴏɴʟɪɴᴇ.")); return; }

        plugin.getMarriageManager().reproduce(player.getUniqueId());
        int bonus = plugin.getConfig().getInt("marriage.reproduce-claim-bonus", 2);
        String spouseName = spouse.getName();
        String msg = plugin.getMessageManager().getRawFormatted("marriage.reproduce-success",
            "{player}", player.getName(), "{spouse}", spouseName, "{amount}", String.valueOf(bonus));
        player.sendMessage(FontUtils.colorize(msg));
        spouse.sendMessage(FontUtils.colorize(msg));
    }

    private void handleGender(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage(FontUtils.colorize("&c/gender <choice>")); return; }
        PlayerProfile profile = plugin.getMarriageManager().getProfile(player.getUniqueId());
        if (profile.hasGender()) { player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("gender.already-set"))); return; }

        String gender = args[0].toLowerCase();
        List<String> allowed = plugin.getMarriageManager().getAllowedGenders();
        if (!allowed.contains(gender)) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("gender.invalid", "{options}", String.join(", ", allowed)))); return;
        }

        plugin.getMarriageManager().setGender(player.getUniqueId(), gender);
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("gender.set", "{gender}", gender)));
    }

    private void handleInfo(Player player) {
        PlayerProfile profile = plugin.getMarriageManager().getProfile(player.getUniqueId());
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FFD700&l ᴍᴀʀʀɪᴀɢᴇ ɪɴꜰᴏ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA ɢᴇɴᴅᴇʀ: &f" + (profile.hasGender() ? profile.getGender() : "ɴᴏᴛ sᴇᴛ")));
        if (profile.isMarried()) {
            String name = Bukkit.getOfflinePlayer(profile.getSpouse()).getName();
            player.sendMessage(FontUtils.colorize("&#AAAAAA sᴘᴏᴜsᴇ: &f" + (name != null ? name : "?")));
        } else {
            player.sendMessage(FontUtils.colorize("&#AAAAAA sᴘᴏᴜsᴇ: &fɴᴏɴᴇ"));
        }
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
        player.sendMessage(FontUtils.colorize("&#FF69B4&l ᴍᴀʀʀɪᴀɢᴇ ᴄᴏᴍᴍᴀɴᴅs"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /gender <choice> &7- &#AAAAAAsᴇᴛ ɢᴇɴᴅᴇʀ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /marry <player> &7- &#AAAAAAᴘʀᴏᴘᴏsᴇ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /marriage accept/deny &7- &#AAAAAAʀᴇsᴘᴏɴᴅ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /divorce &7- &#AAAAAAᴅɪᴠᴏʀᴄᴇ"));
        player.sendMessage(FontUtils.colorize("&#AAAAAA /hug /kiss /reproduce &7- &#AAAAAAsᴘᴏᴜsᴇ ᴀᴄᴛɪᴏɴs"));
        player.sendMessage(FontUtils.colorize("&7&m─────────────────────────"));
    }
}
