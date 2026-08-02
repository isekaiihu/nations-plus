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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class TownGUI {

    private final NationsPlus plugin;

    public TownGUI(NationsPlus plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) {
            player.sendMessage(FontUtils.colorize(plugin.getMessageManager().get("error.not-in-town")));
            return;
        }

        String title = plugin.getConfig().getString("gui.town.title", "&#333333&l◈ &#FFD700ᴛᴏᴡɴ ᴍᴀɴᴀɢᴇʀ &#333333◈");
        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize(title));

        Material borderMat = Material.valueOf(plugin.getConfig().getString("gui.town.border-material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack border = createItem(borderMat, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, border);
        }

        String leaderName = Bukkit.getOfflinePlayer(town.getLeader()).getName();
        gui.setItem(13, createItem(Material.WRITTEN_BOOK,
            "&#FFD700&l" + FontUtils.toSmallCaps("Town Info"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Name") + ": &f" + town.getName(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Leader") + ": &f" + (leaderName != null ? leaderName : "Unknown"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Members") + ": &f" + town.getMemberCount(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Claims") + ": &f" + town.getClaimCount() + "/" + plugin.getTownManager().getMaxClaims(town),
            "&#AAAAAA" + FontUtils.toSmallCaps("Bank") + ": &f$" + String.format("%.0f", town.getBank()),
            town.getNationId() != null ?
                "&#AAAAAA" + FontUtils.toSmallCaps("Nation") + ": &f" + getNationName(town.getNationId()) : ""
        ));

        gui.setItem(20, createItem(Material.PLAYER_HEAD, "&#44FF44&l" + FontUtils.toSmallCaps("Members"),
            "&#AAAAAA" + FontUtils.toSmallCaps("View and manage members")));

        gui.setItem(22, createItem(Material.GOLD_INGOT, "&#FFD700&l" + FontUtils.toSmallCaps("Bank"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Balance") + ": &f$" + String.format("%.0f", town.getBank())));

        gui.setItem(24, createItem(Material.FILLED_MAP, "&#44AAFF&l" + FontUtils.toSmallCaps("Claims"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Used") + ": &f" + town.getClaimCount(),
            "&#AAAAAA" + FontUtils.toSmallCaps("Bonus") + ": &f" + town.getBonusClaimBlocks()));

        gui.setItem(29, createItem(Material.COMPARATOR, "&#FF4444&l" + FontUtils.toSmallCaps("Town Laws"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Configure town settings")));

        gui.setItem(31, createItem(Material.WRITABLE_BOOK, "&#44FF44&l" + FontUtils.toSmallCaps("Invite Player"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Send a town invitation")));

        gui.setItem(33, createItem(Material.ENDER_PEARL, "&#AAAAFF&l" + FontUtils.toSmallCaps("Town Spawn"),
            "&#AAAAAA" + FontUtils.toSmallCaps("Teleport to spawn")));

        Role role = town.getRole(player.getUniqueId());
        if (role != null && role.isLeader()) {
            gui.setItem(38, createItem(Material.NAME_TAG, "&#FFAA44&l" + FontUtils.toSmallCaps("Rename Town"),
                "&#AAAAAA" + FontUtils.toSmallCaps("Change town name")));
            gui.setItem(40, createItem(Material.TNT, "&#FF4444&l" + FontUtils.toSmallCaps("Delete Town"),
                "&#AAAAAA&l" + FontUtils.toSmallCaps("This cannot be undone!")));
        }

        player.openInventory(gui);
    }

    public void openMemberList(Player player, int page) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) return;

        String title = plugin.getConfig().getString("gui.member-list.title", "&#333333◈ &#AAAAAAᴍᴇᴍʙᴇʀs &#333333◈");
        Inventory gui = Bukkit.createInventory(null, 54, FontUtils.colorize(title));

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        for (int i = 45; i < 54; i++) gui.setItem(i, createItem(filler, " "));

        List<Map.Entry<UUID, Role>> members = new ArrayList<>(town.getMembers().entrySet());
        members.sort((a, b) -> Integer.compare(b.getValue().getWeight(), a.getValue().getWeight()));

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, members.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Role> entry = members.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            String roleName = plugin.getConfig().getString("roles." + entry.getValue().getConfigKey(), entry.getValue().name());
            meta.displayName(FontUtils.colorizeItem("&f" + (playerName != null ? playerName : "Unknown")));
            meta.lore(List.of(
                FontUtils.colorizeItem(roleName),
                FontUtils.colorizeItem("&#AAAAAA" + FontUtils.toSmallCaps("Click to manage"))
            ));
            head.setItemMeta(meta);
            gui.setItem(i - startIndex, head);
        }

        if (page > 0) {
            gui.setItem(45, createItem(Material.ARROW, plugin.getConfig().getString("gui.prev-page-name", "&#FF4444◄ ᴘʀᴇᴠ ᴘᴀɢᴇ")));
        }
        gui.setItem(49, createItem(Material.BARRIER, "&#FF4444" + FontUtils.toSmallCaps("Back")));
        if (endIndex < members.size()) {
            gui.setItem(53, createItem(Material.ARROW, plugin.getConfig().getString("gui.next-page-name", "&#44FF44► ɴᴇxᴛ ᴘᴀɢᴇ")));
        }

        player.openInventory(gui);
    }

    public void openSettingsMenu(Player player) {
        Town town = plugin.getTownManager().getPlayerTown(player.getUniqueId());
        if (town == null) return;

        String title = plugin.getConfig().getString("gui.settings.title", "&#333333◈ &#44FF44ᴛᴏᴡɴ ʟᴀᴡs &#333333◈");
        Inventory gui = Bukkit.createInventory(null, 36, FontUtils.colorize(title));

        Material filler = Material.valueOf(plugin.getConfig().getString("gui.filler-material", "BLACK_STAINED_GLASS_PANE"));
        for (int i = 27; i < 36; i++) gui.setItem(i, createItem(filler, " "));

        TownFlags flags = town.getFlags();
        Map<String, Boolean> flagMap = flags.toMap();

        int slot = 10;
        Map<String, Material> icons = Map.of(
            "pvp", Material.DIAMOND_SWORD,
            "mob-spawning", Material.ZOMBIE_HEAD,
            "block-break", Material.IRON_PICKAXE,
            "block-place", Material.GRASS_BLOCK,
            "interact", Material.CHEST,
            "explosion-damage", Material.TNT
        );

        for (Map.Entry<String, Boolean> entry : flagMap.entrySet()) {
            Material icon = icons.getOrDefault(entry.getKey(), Material.PAPER);
            String status = entry.getValue() ? "&a✔ ᴇɴᴀʙʟᴇᴅ" : "&c✖ ᴅɪsᴀʙʟᴇᴅ";
            gui.setItem(slot, createItem(icon,
                "&#FFD700&l" + FontUtils.toSmallCaps(entry.getKey().replace("-", " ")),
                status,
                "&#AAAAAA" + FontUtils.toSmallCaps("Click to toggle")));
            slot++;
            if (slot == 17) slot = 19;
        }

        gui.setItem(31, createItem(Material.BARRIER, "&#FF4444" + FontUtils.toSmallCaps("Back")));
        player.openInventory(gui);
    }

    public void openRoleSelectMenu(Player player, UUID targetPlayer) {
        String title = plugin.getConfig().getString("gui.role-select.title", "&#333333◈ &#FFD700sᴇʟᴇᴄᴛ ʀᴏʟᴇ &#333333◈");
        Inventory gui = Bukkit.createInventory(null, 18, FontUtils.colorize(title));

        int slot = 0;
        for (Role role : Role.values()) {
            if (role == Role.KING || role == Role.QUEEN) continue;
            String roleName = plugin.getConfig().getString("roles." + role.getConfigKey(), role.name());
            gui.setItem(slot++, createItem(Material.PAPER, roleName,
                "&#AAAAAA" + FontUtils.toSmallCaps("Click to assign")));
        }

        gui.setItem(17, createItem(Material.BARRIER, "&#FF4444" + FontUtils.toSmallCaps("Cancel")));
        player.openInventory(gui);
    }

    private String getNationName(String nationId) {
        Nation nation = plugin.getNationManager().getNation(nationId);
        return nation != null ? nation.getName() : "Unknown";
    }

    /**
     * Creates an ItemStack with colorized name/lore and italic explicitly disabled.
     */
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
