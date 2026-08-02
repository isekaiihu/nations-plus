package com.isekai.nationsplus.hooks;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.Nation;
import com.isekai.nationsplus.data.PlayerProfile;
import com.isekai.nationsplus.data.Role;
import com.isekai.nationsplus.data.Town;
import com.isekai.nationsplus.utils.FontUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for NationsPlus.
 *
 * Placeholders (all return §-formatted strings for TAB/Essentials compatibility):
 *   %nationsplus_town_name%             - Town name (with color codes applied via §)
 *   %nationsplus_town_name_raw%         - Town name (raw, no processing)
 *   %nationsplus_town_name_stripped%     - Town name (all color codes removed)
 *   %nationsplus_town_leader%           - Town leader name
 *   %nationsplus_town_member_count%     - Number of town members
 *   %nationsplus_town_claim_count%      - Number of claimed chunks
 *   %nationsplus_town_bank%             - Town bank balance
 *   %nationsplus_town_role%             - Player's role display name
 *   %nationsplus_town_role_weight%      - Player's role weight
 *   %nationsplus_town_is_leader%        - Whether the player is a town leader
 *   %nationsplus_town_neutral%          - Whether the town is neutral
 *   %nationsplus_has_town%              - Whether the player has a town
 *
 *   %nationsplus_nation_name%           - Nation name (with color codes via §)
 *   %nationsplus_nation_name_raw%       - Nation name (raw)
 *   %nationsplus_nation_name_stripped%   - Nation name (stripped)
 *   %nationsplus_nation_ruler%          - Nation ruler name
 *   %nationsplus_nation_town_count%     - Number of towns in nation
 *   %nationsplus_nation_bank%           - Nation bank balance
 *   %nationsplus_nation_neutral%        - Whether nation is neutral
 *   %nationsplus_has_nation%            - Whether the player has a nation
 *
 *   %nationsplus_gender%                - Player's gender
 *   %nationsplus_married%               - Whether the player is married
 *   %nationsplus_spouse%                - Spouse's name
 *
 *   %nationsplus_wars_active%           - Number of active wars for the player's nation
 */
public class NationsPlusExpansion extends PlaceholderExpansion {

    private final NationsPlus plugin;

    public NationsPlusExpansion(NationsPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nationsplus";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ISekai";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        String key = params.toLowerCase(java.util.Locale.ROOT);
        java.util.UUID uuid = player.getUniqueId();

        // ── Town Placeholders ──
        if (key.startsWith("town_") || key.equals("has_town")) {
            Town town = plugin.getTownManager().getPlayerTown(uuid);

            return switch (key) {
                case "has_town" -> town != null ? "true" : "false";
                // Return §-formatted string for TAB/Essentials compatibility
                case "town_name" -> town != null ? FontUtils.colorizeLegacy(town.getName()) : "";
                case "town_name_raw" -> town != null ? town.getName() : "";
                case "town_name_stripped" -> town != null ? FontUtils.stripColors(town.getName()) : "";
                case "town_leader" -> {
                    if (town == null) yield "";
                    OfflinePlayer leader = Bukkit.getOfflinePlayer(town.getLeader());
                    yield leader.getName() != null ? leader.getName() : "Unknown";
                }
                case "town_member_count" -> town != null ? String.valueOf(town.getMemberCount()) : "0";
                case "town_claim_count" -> town != null ? String.valueOf(town.getClaimCount()) : "0";
                case "town_bank" -> town != null ? String.format("%.2f", town.getBank()) : "0.00";
                case "town_role" -> {
                    if (town == null) yield "";
                    Role role = town.getRole(uuid);
                    if (role == null) yield "none";
                    // Return the configured display name with § colors for TAB
                    String configRole = plugin.getConfig().getString("roles." + role.getConfigKey(), role.getConfigKey());
                    yield FontUtils.colorizeLegacy(configRole);
                }
                case "town_role_weight" -> {
                    if (town == null) yield "0";
                    Role role = town.getRole(uuid);
                    yield role != null ? String.valueOf(role.getWeight()) : "0";
                }
                case "town_is_leader" -> {
                    if (town == null) yield "false";
                    Role role = town.getRole(uuid);
                    yield role != null && role.isLeader() ? "true" : "false";
                }
                case "town_neutral" -> town != null ? String.valueOf(town.isNeutral()) : "false";
                case "town_bonus_claims" -> town != null ? String.valueOf(town.getBonusClaimBlocks()) : "0";
                case "town_created" -> {
                    if (town == null) yield "";
                    long ms = town.getCreatedAt();
                    yield new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(ms));
                }
                default -> null;
            };
        }

        // ── Nation Placeholders ──
        if (key.startsWith("nation_") || key.equals("has_nation")) {
            Nation nation = plugin.getNationManager().getPlayerNation(uuid);

            return switch (key) {
                case "has_nation" -> nation != null ? "true" : "false";
                case "nation_name" -> nation != null ? FontUtils.colorizeLegacy(nation.getName()) : "";
                case "nation_name_raw" -> nation != null ? nation.getName() : "";
                case "nation_name_stripped" -> nation != null ? FontUtils.stripColors(nation.getName()) : "";
                case "nation_ruler" -> {
                    if (nation == null) yield "";
                    OfflinePlayer ruler = Bukkit.getOfflinePlayer(nation.getRuler());
                    yield ruler.getName() != null ? ruler.getName() : "Unknown";
                }
                case "nation_town_count" -> nation != null ? String.valueOf(nation.getTownCount()) : "0";
                case "nation_bank" -> nation != null ? String.format("%.2f", nation.getBank()) : "0.00";
                case "nation_neutral" -> nation != null ? String.valueOf(nation.isNeutral()) : "false";
                case "nation_created" -> {
                    if (nation == null) yield "";
                    long ms = nation.getCreatedAt();
                    yield new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(ms));
                }
                default -> null;
            };
        }

        // ── Marriage / Gender Placeholders ──
        if (key.equals("gender")) {
            PlayerProfile profile = plugin.getMarriageManager().getProfile(uuid);
            return profile.hasGender() ? profile.getGender() : "";
        }
        if (key.equals("married")) {
            return plugin.getMarriageManager().getProfile(uuid).isMarried() ? "true" : "false";
        }
        if (key.equals("spouse")) {
            PlayerProfile profile = plugin.getMarriageManager().getProfile(uuid);
            if (!profile.isMarried()) return "";
            OfflinePlayer spouse = Bukkit.getOfflinePlayer(profile.getSpouse());
            return spouse.getName() != null ? spouse.getName() : "Unknown";
        }

        // ── War Placeholders ──
        if (key.equals("wars_active")) {
            Nation nation = plugin.getNationManager().getPlayerNation(uuid);
            if (nation == null) return "0";
            return String.valueOf(plugin.getWarManager().getWarsForNation(nation.getId()).size());
        }

        return null;
    }
}
