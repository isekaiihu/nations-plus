package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final NationsPlus plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private final Map<String, String> cache = new HashMap<>();

    public MessageManager(NationsPlus plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        cache.clear();
    }

    public String getRaw(String path) {
        return cache.computeIfAbsent(path, p -> messages.getString(p, "&cMissing message: " + p));
    }

    public String get(String path) {
        return getRaw("prefix") + getRaw(path);
    }

    public String get(String path, String... replacements) {
        String msg = get(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public String getRawFormatted(String path, String... replacements) {
        String msg = getRaw(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public void reload() {
        loadMessages();
    }
}
