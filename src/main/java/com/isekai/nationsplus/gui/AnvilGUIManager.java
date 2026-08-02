package com.isekai.nationsplus.gui;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.utils.FontUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Anvil GUI system for name input (town/nation creation, delete confirmation).
 * Uses Paper's real anvil API (player.openAnvil) so players can actually type.
 * Zero XP cost enforced via PrepareAnvilEvent in GUIListener.
 * Items are NOT given to the player — inventory is cleared before closing.
 */
public class AnvilGUIManager {

    private final NationsPlus plugin;
    private final Map<UUID, AnvilSession> activeSessions = new HashMap<>();

    public AnvilGUIManager(NationsPlus plugin) {
        this.plugin = plugin;
    }

    public void openAnvil(Player player, String promptKey, BiConsumer<Player, String> callback) {
        InventoryView view = player.openAnvil(null, true);
        if (view == null) {
            plugin.getLogger().warning("Failed to open anvil for " + player.getName());
            return;
        }

        // Set up input item (locked MAP) in slot 0
        Material inputMat = Material.valueOf(plugin.getConfig().getString("anvil.input-item", "MAP"));
        ItemStack inputItem = new ItemStack(inputMat);
        ItemMeta meta = inputItem.getItemMeta();

        String promptText = plugin.getMessageManager().getRaw("anvil." + promptKey);
        if (promptText == null || promptText.contains("Missing message")) {
            promptText = plugin.getConfig().getString("anvil.input-name", "&#AAAAAA✎ ᴛʏᴘᴇ ʜᴇʀᴇ");
        }
        meta.displayName(FontUtils.colorizeItem(promptText));
        inputItem.setItemMeta(meta);

        view.getTopInventory().setItem(0, inputItem);

        // Set repair cost to 0
        if (view instanceof AnvilView anvilView) {
            anvilView.setRepairCost(0);
            anvilView.setMaximumRepairCost(0);
        }

        activeSessions.put(player.getUniqueId(), new AnvilSession(promptKey, callback));
    }

    public boolean hasSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public AnvilSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public static class AnvilSession {
        private final String promptKey;
        private final BiConsumer<Player, String> callback;
        private String lastRenameText = "";

        public AnvilSession(String promptKey, BiConsumer<Player, String> callback) {
            this.promptKey = promptKey;
            this.callback = callback;
        }

        public String getPromptKey() { return promptKey; }
        public BiConsumer<Player, String> getCallback() { return callback; }
        public String getLastRenameText() { return lastRenameText; }
        public void setLastRenameText(String text) { this.lastRenameText = text; }
    }
}
