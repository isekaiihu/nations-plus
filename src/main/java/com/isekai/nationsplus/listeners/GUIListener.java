package com.isekai.nationsplus.listeners;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import com.isekai.nationsplus.gui.AnvilGUIManager;
import com.isekai.nationsplus.utils.FontUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;

public class GUIListener implements Listener {

    private final NationsPlus plugin;

    public GUIListener(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!plugin.getAnvilGUI().hasSession(player.getUniqueId())) return;

        AnvilGUIManager.AnvilSession session = plugin.getAnvilGUI().getSession(player.getUniqueId());
        if (session == null) return;

        if (event.getView() instanceof AnvilView anvilView) {
            anvilView.setRepairCost(0);
            anvilView.setMaximumRepairCost(0);

            String renameText = anvilView.getRenameText();
            if (renameText != null && !renameText.isEmpty()) {
                session.setLastRenameText(renameText);
            }
        }

        ItemStack input = event.getInventory().getItem(0);
        if (input != null && event.getResult() == null) {
            event.setResult(input.clone());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = "";
        if (event.getView().title() != null) {
            title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        }

        // ── Anvil GUI ──
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            if (plugin.getAnvilGUI().hasSession(player.getUniqueId())) {
                event.setCancelled(true);

                if (event.getSlot() == 2) { // Result slot clicked
                    AnvilGUIManager.AnvilSession session = plugin.getAnvilGUI().getSession(player.getUniqueId());
                    if (session == null) return;

                    // Method 1: Get rename text from AnvilView
                    String inputText = "";
                    if (event.getView() instanceof AnvilView anvilView) {
                        String rename = anvilView.getRenameText();
                        if (rename != null && !rename.isEmpty()) {
                            inputText = rename;
                        }
                    }

                    // Method 2: Fallback to stored rename text
                    if (inputText.isEmpty()) {
                        inputText = session.getLastRenameText();
                    }

                    // Method 3: Fallback to result item display name
                    if (inputText.isEmpty()) {
                        ItemStack resultItem = event.getCurrentItem();
                        if (resultItem != null && resultItem.hasItemMeta() && resultItem.getItemMeta().hasDisplayName()) {
                            inputText = PlainTextComponentSerializer.plainText().serialize(resultItem.getItemMeta().displayName());
                        }
                    }

                    // Method 4: Fallback to input item (slot 0) display name
                    if (inputText.isEmpty()) {
                        ItemStack inputItem = event.getInventory().getItem(0);
                        if (inputItem != null && inputItem.hasItemMeta() && inputItem.getItemMeta().hasDisplayName()) {
                            inputText = PlainTextComponentSerializer.plainText().serialize(inputItem.getItemMeta().displayName());
                        }
                    }

                    // Remove session and clear inventory BEFORE closing to prevent item leak
                    plugin.getAnvilGUI().removeSession(player.getUniqueId());
                    event.getInventory().clear(); // Prevent map from being given to player
                    player.closeInventory();

                    if (!inputText.isEmpty() && session.getCallback() != null) {
                        session.getCallback().accept(player, inputText);
                    }
                }
                return;
            }
        }

        if (event.getCurrentItem() == null) return;

        // ── Town GUI ──
        if (title.contains("ᴛᴏᴡɴ ᴍᴀɴᴀɢᴇʀ")) {
            event.setCancelled(true);
            handleTownGUIClick(player, event.getSlot());
            return;
        }

        if (title.contains("ᴍᴇᴍʙᴇʀs") || title.contains("ᴍᴇᴍʙᴇʀ")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) { plugin.getTownGUI().openMainMenu(player); return; }
            return;
        }

        if (title.contains("ᴛᴏᴡɴ ʟᴀᴡs")) {
            event.setCancelled(true);
            handleTownLawsClick(player, event.getSlot());
            return;
        }

        if (title.contains("ɴᴀᴛɪᴏɴ ᴍᴀɴᴀɢᴇʀ")) {
            event.setCancelled(true);
            handleNationGUIClick(player, event.getSlot());
            return;
        }

        if (title.contains("ᴇᴀʀᴛʜ ᴀᴅᴍɪɴ")) {
            event.setCancelled(true);
            handleAdminGUIClick(player, event.getSlot());
            return;
        }

        if (title.contains("ᴛᴏᴡɴs") || title.contains("ɴᴀᴛɪᴏɴs")) {
            event.setCancelled(true);
            if (event.getSlot() == 49) { plugin.getAdminGUI().openMainMenu(player); return; }
            if (event.getSlot() == 45) {
                int page = Math.max(0, plugin.getAdminGUI().getPage(player.getUniqueId()) - 1);
                plugin.getAdminGUI().openSearchResults(player, plugin.getAdminGUI().getSearchMode(player.getUniqueId()),
                    plugin.getAdminGUI().getSearchQuery(player.getUniqueId()), page);
            }
            if (event.getSlot() == 53) {
                int page = plugin.getAdminGUI().getPage(player.getUniqueId()) + 1;
                plugin.getAdminGUI().openSearchResults(player, plugin.getAdminGUI().getSearchMode(player.getUniqueId()),
                    plugin.getAdminGUI().getSearchQuery(player.getUniqueId()), page);
            }
            return;
        }
    }

    private void handleTownGUIClick(Player player, int slot) {
        switch (slot) {
            case 20 -> plugin.getTownGUI().openMemberList(player, 0);
            case 29 -> plugin.getTownGUI().openSettingsMenu(player);
            case 33 -> {
                Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
                if (town != null && town.getSpawn() != null) {
                    player.closeInventory();
                    startTeleportCountdown(player, town.getSpawn(), "town");
                }
            }
            case 40 -> {
                player.closeInventory();
                Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
                if (town != null && town.getRole(player.getUniqueId()).isLeader()) {
                    plugin.getAnvilGUI().openAnvil(player, "town-delete-prompt", (p, text) -> {
                        if (!text.trim().equalsIgnoreCase(plugin.getConfig().getString("anvil.delete-confirm-text", "DELETE"))) return;
                        String name = town.getName();
                        plugin.getTownManager().deleteTown(town.getId());
                        plugin.getDynmapManager().removeTownMarker(town.getId());
                        p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.deleted", "{town}", name)));
                    });
                }
            }
        }
    }

    private void handleTownLawsClick(Player player, int slot) {
        if (slot == 31) { plugin.getTownGUI().openMainMenu(player); return; }
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) return;
        Role role = town.getRole(player.getUniqueId());
        if (role == null || !role.isLeader()) return;

        Map<String, Boolean> flags = town.getFlags().toMap();
        String[] keys = flags.keySet().toArray(new String[0]);
        int idx = -1;
        if (slot >= 10 && slot <= 15) idx = slot - 10;
        else if (slot >= 19 && slot <= 20) idx = slot - 19 + 6;

        if (idx >= 0 && idx < keys.length) {
            boolean newVal = town.getFlags().toggle(keys[idx]);
            plugin.getTownManager().saveData();
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("town.law-changed",
                "{setting}", keys[idx], "{value}", newVal ? "ᴇɴᴀʙʟᴇᴅ" : "ᴅɪsᴀʙʟᴇᴅ")));
            plugin.getTownGUI().openSettingsMenu(player);
        }
    }

    private void handleNationGUIClick(Player player, int slot) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) return;

        switch (slot) {
            case 20 -> plugin.getNationGUI().openTownListMenu(player, nation.getId(), 0);
            case 31 -> {
                if (nation.getSpawn() != null) {
                    player.closeInventory();
                    startTeleportCountdown(player, nation.getSpawn(), "nation");
                }
            }
            case 33 -> {
                if (nation.getRuler().equals(player.getUniqueId())) {
                    nation.setNeutral(!nation.isNeutral());
                    plugin.getNationManager().saveData();
                    plugin.getNationGUI().openMainMenu(player);
                }
            }
            case 40 -> {
                if (nation.getRuler().equals(player.getUniqueId())) {
                    player.closeInventory();
                    plugin.getAnvilGUI().openAnvil(player, "nation-delete-prompt", (p, text) -> {
                        if (!text.trim().equalsIgnoreCase(plugin.getConfig().getString("anvil.delete-confirm-text", "DELETE"))) return;
                        String name = nation.getName();
                        plugin.getDynmapManager().removeNationMarker(nation.getId());
                        plugin.getNationManager().deleteNation(nation.getId());
                        p.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("nation.deleted", "{nation}", name)));
                    });
                }
            }
        }
    }

    private void handleAdminGUIClick(Player player, int slot) {
        switch (slot) {
            case 20 -> plugin.getAdminGUI().openSearchResults(player, "towns", "", 0);
            case 22 -> plugin.getAdminGUI().openSearchResults(player, "nations", "", 0);
            case 31 -> {
                player.closeInventory();
                plugin.reload();
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("admin.reload")));
            }
        }
    }

    /**
     * Teleport countdown. Player must not move during countdown. Teleports AFTER countdown.
     */
    private void startTeleportCountdown(Player player, org.bukkit.Location destination, String type) {
        int countdown = type.equals("town")
            ? plugin.getConfig().getInt("town.spawn-cooldown", 5)
            : plugin.getConfig().getInt("nation.spawn-cooldown", 10);

        org.bukkit.Location startLoc = player.getLocation().clone();

        String msgKey = type.equals("town") ? "town.spawn-teleport" : "nation.spawn-teleport";

        // Send initial countdown message (NOT a cooldown)
        player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("teleport.countdown",
            "{time}", String.valueOf(countdown))));

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
                player.teleport(destination);
                player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get(msgKey)));
            }
        }, 20L, 20L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            if (plugin.getAnvilGUI().hasSession(player.getUniqueId())) {
                // Clear anvil inventory to prevent item leak
                event.getInventory().clear();
                plugin.getAnvilGUI().removeSession(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains("ᴛᴏᴡɴ") || title.contains("ɴᴀᴛɪᴏɴ") || title.contains("ᴀᴅᴍɪɴ") ||
            title.contains("ᴍᴇᴍʙᴇʀ") || title.contains("ʀᴏʟᴇ") || title.contains("ʟᴀᴡs")) {
            event.setCancelled(true);
        }
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            if (event.getWhoClicked() instanceof Player p && plugin.getAnvilGUI().hasSession(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
