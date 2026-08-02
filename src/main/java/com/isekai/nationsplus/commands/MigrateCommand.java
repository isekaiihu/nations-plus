package com.isekai.nationsplus.commands;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.migration.TownyMigrator;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MigrateCommand implements CommandExecutor {

    private final NationsPlus plugin;
    private boolean migrationInProgress = false;

    public MigrateCommand(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nationsplus.admin")) {
            sender.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.no-permission")));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(FontUtils.colorize("&c[NationsPlus] Usage: /migrate <plugin>"));
            sender.sendMessage(FontUtils.colorize("&7Available plugins: &fTowny"));
            return true;
        }

        String pluginName = args[0].toLowerCase();

        if (migrationInProgress) {
            sender.sendMessage(FontUtils.colorize("&c[NationsPlus] A migration is already in progress!"));
            return true;
        }

        switch (pluginName) {
            case "towny" -> {
                migrationInProgress = true;
                TownyMigrator migrator = new TownyMigrator(plugin);
                sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &fInitiating Towny migration..."));
                sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &7This will import all towns, nations, residents, claims, and bank balances."));
                try {
                    migrator.migrate(sender);
                } finally {
                    migrationInProgress = false;
                }
            }
            default -> {
                sender.sendMessage(FontUtils.colorize("&c[NationsPlus] Unknown plugin: " + args[0]));
                sender.sendMessage(FontUtils.colorize("&7Available plugins: &fTowny"));
            }
        }
        return true;
    }
}
