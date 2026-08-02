package com.isekai.nationsplus.gui;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import com.isekai.nationsplus.utils.FontUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AdminGUI {

    private final NationsPlus plugin;
    private final Map<UUID, String> searchQueries = new HashMap<>();
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, String> searchMode = new HashMap<>();

    public AdminGUI(NationsPlus plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        String title = plugin.getConfig().getString("gui.admin.title", "&#FF4444&l◈ &#FFD700ᴇᴀʀᴛʜ ᴀᴅᴍɪɴ &#FF4444◈");
        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize(title));

        Material borderMat = Material.valueOf(plugin.getConfig().getString("gui.admin.border-material", "RED_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, border);
        }

        gui.setItem(20, createItem(Material.CAMPFIRE, "&#FFD700&l" + FontUtils.toSmallCaps("Search Towns"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Browse and manage towns"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Total") + ": &f" + plugin.getTownManager().getAllTowns().size()));

        gui.setItem(22, createItem(Material.GOLDEN_HELMET, "&#4488FF&l" + FontUtils.toSmallCaps("Search Nations"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Browse and manage nations"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Total") + ": &f" + plugin.getNationManager().getAllNations().size()));

        gui.setItem(24, createItem(Material.NETHERITE_SWORD, "&#FF4444&l" + FontUtils.toSmallCaps("Active Wars"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Wars") + ": &f" + plugin.getWarManager().getAllActiveWars().size()));

        gui.setItem(31, createItem(Material.COMMAND_BLOCK, "&#44FF44&l" + FontUtils.toSmallCaps("Reload Plugin"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Reload config and messages")));

        gui.setItem(29, createItem(Material.BOOK, "&#AAAAFF&l" + FontUtils.toSmallCaps("Server Stats"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Towns") + ": &f" + plugin.getTownManager().getAllTowns().size(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Nations") + ": &f" + plugin.getNationManager().getAllNations().size(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Wars") + ": &f" + plugin.getWarManager().getAllActiveWars().size()));

        player.openInventory(gui);
    }

    public void openSearchResults(Player player, String mode, String query, int page) {
        searchQueries.put(player.getUniqueId(), query);
        pages.put(player.getUniqueId(), page);
        searchMode.put(player.getUniqueId(), mode);

        String titleStr = mode.equals("towns") ? "&#FFD700ᴛᴏᴡɴs" : "&#4488FF&lɴᴀᴛɪᴏɴs";
        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize("&#333333◈ " + titleStr + " &#333333◈"));

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        for (int i = 45; i < 54; i++) gui.setItem(i, createItem(filler, " "));

        List<ItemStack> items;
        int totalSize;

        if (mode.equals("towns")) {
            List<Town> results = query.isEmpty() ?
                new ArrayList<>(plugin.getTownManager().getAllTowns()) :
                plugin.getTownManager().searchTowns(query);
            totalSize = results.size();
            int start = page * 45;
            int end = Math.min(start + 45, totalSize);
            items = new ArrayList<>();
            for (int i = start; i < end; i++) {
                Town t = results.get(i);
                String leader = Bukkit.getOfflinePlayer(t.getLeader()).getName();
                items.add(createItem(Material.CAMPFIRE, "&f" + t.getName(),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Leader") + ": &f" + (leader != null ? leader : "?"),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Members") + ": &f" + t.getMemberCount(),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Claims") + ": &f" + t.getClaimCount(),
                    "&#FF4444" + FontUtils.toSmallCaps("Click to manage")));
            }
        } else {
            List<Nation> results = query.isEmpty() ?
                new ArrayList<>(plugin.getNationManager().getAllNations()) :
                plugin.getNationManager().searchNations(query);
            totalSize = results.size();
            int start = page * 45;
            int end = Math.min(start + 45, totalSize);
            items = new ArrayList<>();
            for (int i = start; i < end; i++) {
                Nation n = results.get(i);
                String ruler = Bukkit.getOfflinePlayer(n.getRuler()).getName();
                items.add(createItem(Material.GOLDEN_HELMET, "&f" + n.getName(),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Ruler") + ": &f" + (ruler != null ? ruler : "?"),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Towns") + ": &f" + n.getTownCount(),
                    "&#AAAAAA" + FontUtils.toSmallCaps("Bank") + ": &f$" + String.format("%.0f", n.getBank()),
                    "&#FF4444" + FontUtils.toSmallCaps("Click to manage")));
            }
        }

        for (int i = 0; i < items.size(); i++) gui.setItem(i, items.get(i));

        if (page > 0) gui.setItem(45, createItem(Material.ARROW, plugin.getConfig().getString("gui.prev-page-name", "&#FF4444◄ ᴘʀᴇᴠ ᴘᴀɢᴇ")));
        gui.setItem(48, createItem(Material.OAK_SIGN, "&#FFD700" + FontUtils.toSmallCaps("Search"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Click to search")));
        gui.setItem(49, createItem(Material.BARRIER, "&#FF4444" + FontUtils.toSmallCaps("Back")));
        if ((page + 1) * 45 < totalSize) gui.setItem(53, createItem(Material.ARROW, plugin.getConfig().getString("gui.next-page-name", "&#44FF44► ɴᴇxᴛ ᴘᴀɢᴇ")));

        player.openInventory(gui);
    }

    public String getSearchMode(UUID uuid) { return searchMode.getOrDefault(uuid, "towns"); }
    public int getPage(UUID uuid) { return pages.getOrDefault(uuid, 0); }
    public String getSearchQuery(UUID uuid) { return searchQueries.getOrDefault(uuid, ""); }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(FontUtils.colorizeItem(name));
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                if (!line.isEmpty()) loreComponents.add(FontUtils.colorizeItem(line));
            }
            meta.lore(loreComponents);
        }
        item.setItemMeta(meta);
        return item;
    }
}
