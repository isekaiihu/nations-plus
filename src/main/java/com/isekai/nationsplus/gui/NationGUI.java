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

public class NationGUI {

    private final NationsPlus plugin;

    public NationGUI(NationsPlus plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Nation nation = plugin.getNationManager().getPlayerNation(player.getUniqueId());
        if (nation == null) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-nation")));
            return;
        }

        String title = plugin.getConfig().getString("gui.nation.title", "&#333333&l◈ &#4488FF&lɴᴀᴛɪᴏɴ ᴍᴀɴᴀɢᴇʀ &#333333◈");
        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize(title));

        Material borderMat = Material.valueOf(plugin.getConfig().getString("gui.nation.border-material", "BLUE_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, border);
        }

        String rulerName = Bukkit.getOfflinePlayer(nation.getRuler()).getName();
        gui.setItem(13, createItem(Material.GOLDEN_HELMET,
            "&#4488FF&l" + FontUtils.toSmallCaps("Nation Info"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Name") + ": &f" + nation.getName(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Ruler") + ": &f" + (rulerName != null ? rulerName : "Unknown"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Towns") + ": &f" + nation.getTownCount(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Bank") + ": &f$" + String.format("%.0f", nation.getBank()),
            nation.isNeutral() ? "&#44FF44☮ ɴᴇᴜᴛʀᴀʟ" : ""
        ));

        gui.setItem(20, createItem(Material.CAMPFIRE, "&#FFD700&l" + FontUtils.toSmallCaps("Member Towns"),
            "&#AAAAAA" + FontUtils.toSmallCaps("View all towns")));

        gui.setItem(22, createItem(Material.GOLD_BLOCK, "&#FFD700&l" + FontUtils.toSmallCaps("Nation Bank"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Balance") + ": &f$" + String.format("%.0f", nation.getBank())));

        gui.setItem(24, createItem(Material.SHIELD, "&#FF4444&l" + FontUtils.toSmallCaps("Relations"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Wars & alliances")));

        gui.setItem(29, createItem(Material.WRITABLE_BOOK, "&#44FF44&l" + FontUtils.toSmallCaps("Invite Town"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Add a town to the nation")));

        gui.setItem(31, createItem(Material.ENDER_EYE, "&#AAAAFF&l" + FontUtils.toSmallCaps("Nation Spawn"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Teleport to capital")));

        gui.setItem(33, createItem(Material.WHITE_BANNER, "&#44FF44&l" + FontUtils.toSmallCaps("Toggle Neutral"),
            nation.isNeutral() ? "&#44FF44☮ ᴄᴜʀʀᴇɴᴛʟʏ ɴᴇᴜᴛʀᴀʟ" : "&#FF4444⚔ ɴᴏᴛ ɴᴇᴜᴛʀᴀʟ"));

        if (nation.getRuler().equals(player.getUniqueId())) {
            gui.setItem(40, createItem(Material.TNT, "&#FF4444&l" + FontUtils.toSmallCaps("Delete Nation"),
                "&#AAAAAA&l" + FontUtils.toSmallCaps("This cannot be undone!")));
        }

        player.openInventory(gui);
    }

    public void openTownListMenu(Player player, String nationId, int page) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        if (nation == null) return;

        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize("&#333333◈ &#AAAAAAᴛᴏᴡɴs &#333333◈"));

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        for (int i = 45; i < 54; i++) gui.setItem(i, createItem(filler, " "));

        List<String> townIds = new ArrayList<>(nation.getTownIds());
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, townIds.size());

        for (int i = startIndex; i < endIndex; i++) {
            Town town = plugin.getTownManager().getTown(townIds.get(i));
            if (town == null) continue;
            String leaderName = Bukkit.getOfflinePlayer(town.getLeader()).getName();
            gui.setItem(i - startIndex, createItem(Material.CAMPFIRE,
                "&f" + town.getName(),
                "&#AAAAAA" + FontUtils.toSmallCaps("Leader") + ": &f" + (leaderName != null ? leaderName : "Unknown"),
                "&#AAAAAA" + FontUtils.toSmallCaps("Members") + ": &f" + town.getMemberCount()));
        }

        if (page > 0) gui.setItem(45, createItem(Material.ARROW, plugin.getConfig().getString("gui.prev-page-name")));
        gui.setItem(49, createItem(Material.BARRIER, "&#FF4444" + FontUtils.toSmallCaps("Back")));
        if (endIndex < townIds.size()) gui.setItem(53, createItem(Material.ARROW, plugin.getConfig().getString("gui.next-page-name")));

        player.openInventory(gui);
    }

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
