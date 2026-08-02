package com.isekai.nationsplus.migration;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.ClaimChunk;
import com.isekai.nationsplus.data.Relation;
import com.isekai.nationsplus.data.Role;
import com.isekai.nationsplus.utils.FontUtils;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 * Migrates all data from Towny into NationsPlus.
 * Towns, Nations, Residents, Claims, and Bank balances are transferred.
 */
public class TownyMigrator {

    private final NationsPlus plugin;

    public TownyMigrator(NationsPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if Towny is present and enabled.
     */
    public boolean isTownyAvailable() {
        org.bukkit.plugin.Plugin towny = Bukkit.getPluginManager().getPlugin("Towny");
        return towny != null && towny.isEnabled();
    }

    /**
     * Perform full migration, reporting progress to sender.
     */
    public void migrate(CommandSender sender) {
        if (!isTownyAvailable()) {
            sender.sendMessage(FontUtils.colorize("&c[NationsPlus] Towny is not installed or enabled!"));
            return;
        }

        sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &fStarting Towny migration..."));

        // Run on main thread since Towny API may not be thread-safe
        Bukkit.getScheduler().runTask(plugin, () -> runMigration(sender));
    }

    private void runMigration(CommandSender sender) {
        try {
            int townCount = 0;
            int nationCount = 0;
            int residentCount = 0;
            int claimCount = 0;

            TownyUniverse universe = TownyUniverse.getInstance();

            // Map of Towny IDs -> our IDs (for linking + relations)
            Map<UUID, String> townyTownToOurId = new HashMap<>();
            Map<UUID, String> townyNationToOurId = new HashMap<>();

            // ═══════════════════════════════════════════
            // STEP 1: Migrate all Towny towns
            // ═══════════════════════════════════════════
            sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &7Migrating towns..."));

            Collection<com.palmergames.bukkit.towny.object.Town> townyTowns = universe.getTowns();
            for (com.palmergames.bukkit.towny.object.Town townyTown : townyTowns) {
                String townName = townyTown.getName();

                // Skip if a town with this name already exists
                if (plugin.getTownManager().getTownByName(townName) != null) {
                    sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &7Skipping town '" + townName + "' (already exists)"));
                    continue;
                }

                Resident mayor = safeGetMayor(townyTown);
                UUID mayorUUID = mayor != null ? mayor.getUUID() : null;
                if (mayorUUID == null) {
                    sender.sendMessage(FontUtils.colorize("&c[NationsPlus] &7Skipping town '" + townName + "' (no valid mayor)"));
                    continue;
                }

                // Ensure mayor can lead migrated town
                removeFromExistingTown(mayorUUID);

                com.isekai.nationsplus.data.Town npTown = plugin.getTownManager().createTown(townName, mayorUUID);
                townyTownToOurId.put(townyTown.getUUID(), npTown.getId());
                townCount++;

                // Transfer bank balance
                npTown.setBank(readBankBalance(townyTown));

                // Transfer spawn (if present)
                Location townSpawn = readSpawn(townyTown);
                if (townSpawn != null) {
                    npTown.setSpawn(townSpawn);
                }

                // Transfer residents
                for (Resident resident : safeGetResidents(townyTown)) {
                    UUID resUUID = resident.getUUID();
                    if (resUUID == null || resUUID.equals(mayorUUID)) continue;

                    removeFromExistingTown(resUUID);

                    Role role = mapResidentRole(resident);
                    plugin.getTownManager().addMember(npTown.getId(), resUUID, role);
                    residentCount++;
                }

                // Transfer claimed chunks (TownBlocks)
                for (TownBlock block : safeGetTownBlocks(townyTown)) {
                    ClaimChunk chunk = convertTownBlock(block);
                    if (chunk == null) continue;

                    if (!plugin.getTownManager().isClaimed(chunk)) {
                        plugin.getTownManager().addClaim(npTown.getId(), chunk);
                        claimCount++;
                    }
                }
            }

            // ═══════════════════════════════════════════
            // STEP 2: Migrate all Towny nations
            // ═══════════════════════════════════════════
            sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &7Migrating nations..."));

            Collection<com.palmergames.bukkit.towny.object.Nation> townyNations = universe.getNations();
            for (com.palmergames.bukkit.towny.object.Nation townyNation : townyNations) {
                String nationName = townyNation.getName();

                if (plugin.getNationManager().getNationByName(nationName) != null) {
                    sender.sendMessage(FontUtils.colorize("&e[NationsPlus] &7Skipping nation '" + nationName + "' (already exists)"));
                    continue;
                }

                Resident king = safeGetKing(townyNation);
                UUID kingUUID = king != null ? king.getUUID() : null;
                if (kingUUID == null) {
                    sender.sendMessage(FontUtils.colorize("&c[NationsPlus] &7Skipping nation '" + nationName + "' (no valid king)"));
                    continue;
                }

                String capitalOurId = resolveCapitalTownId(townyNation, townyTownToOurId);
                if (capitalOurId == null) {
                    sender.sendMessage(FontUtils.colorize("&c[NationsPlus] &7Skipping nation '" + nationName + "' (no migrated towns found)"));
                    continue;
                }

                com.isekai.nationsplus.data.Nation npNation = plugin.getNationManager().createNation(nationName, kingUUID, capitalOurId);
                townyNationToOurId.put(townyNation.getUUID(), npNation.getId());
                nationCount++;

                // Transfer bank + spawn
                npNation.setBank(readBankBalance(townyNation));
                Location nationSpawn = readSpawn(townyNation);
                if (nationSpawn != null) {
                    npNation.setSpawn(nationSpawn);
                }

                // Add non-capital towns
                for (com.palmergames.bukkit.towny.object.Town townyTown : safeGetNationTowns(townyNation)) {
                    String ourTownId = townyTownToOurId.get(townyTown.getUUID());
                    if (ourTownId != null && !ourTownId.equals(capitalOurId)) {
                        plugin.getNationManager().addTownToNation(npNation.getId(), ourTownId);
                    }
                }
            }

            // ═══════════════════════════════════════════
            // STEP 3: Migrate nation relations
            // ═══════════════════════════════════════════
            for (com.palmergames.bukkit.towny.object.Nation townyNation : townyNations) {
                String ourNationId = townyNationToOurId.get(townyNation.getUUID());
                if (ourNationId == null) continue;

                com.isekai.nationsplus.data.Nation ourNation = plugin.getNationManager().getNation(ourNationId);
                if (ourNation == null) continue;

                for (com.palmergames.bukkit.towny.object.Nation ally : safeGetNationRelations(townyNation, "getAllies")) {
                    String allyId = townyNationToOurId.get(ally.getUUID());
                    if (allyId != null && !allyId.equals(ourNationId)) {
                        ourNation.setRelation(allyId, Relation.ALLY);
                    }
                }

                for (com.palmergames.bukkit.towny.object.Nation enemy : safeGetNationRelations(townyNation, "getEnemies")) {
                    String enemyId = townyNationToOurId.get(enemy.getUUID());
                    if (enemyId != null && !enemyId.equals(ourNationId)) {
                        ourNation.setRelation(enemyId, Relation.WAR);
                    }
                }
            }

            // Save all data
            plugin.getTownManager().saveData();
            plugin.getNationManager().saveData();

            // Refresh dynmap after migration
            plugin.getDynmapManager().renderAll();

            // Report results
            sender.sendMessage(FontUtils.colorize(""));
            sender.sendMessage(FontUtils.colorize("&a&l[NationsPlus] ᴍɪɢʀᴀᴛɪᴏɴ ᴄᴏᴍᴘʟᴇᴛᴇ!"));
            sender.sendMessage(FontUtils.colorize("&a  ► Towns migrated: &f" + townCount));
            sender.sendMessage(FontUtils.colorize("&a  ► Nations migrated: &f" + nationCount));
            sender.sendMessage(FontUtils.colorize("&a  ► Residents migrated: &f" + residentCount));
            sender.sendMessage(FontUtils.colorize("&a  ► Claims migrated: &f" + claimCount));
            sender.sendMessage(FontUtils.colorize(""));

            plugin.getLogger().info("Towny migration complete! Towns: " + townCount +
                ", Nations: " + nationCount + ", Residents: " + residentCount +
                ", Claims: " + claimCount);

        } catch (Exception e) {
            sender.sendMessage(FontUtils.colorize("&c[NationsPlus] Migration failed: " + e.getMessage()));
            plugin.getLogger().severe("Towny migration error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeFromExistingTown(UUID playerId) {
        String existingTownId = plugin.getTownManager().getPlayerTownId(playerId);
        if (existingTownId != null) {
            plugin.getTownManager().removeMember(existingTownId, playerId);
        }
    }

    private Resident safeGetMayor(com.palmergames.bukkit.towny.object.Town townyTown) {
        try {
            return townyTown.getMayor();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Resident safeGetKing(com.palmergames.bukkit.towny.object.Nation townyNation) {
        try {
            return townyNation.getKing();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Collection<Resident> safeGetResidents(com.palmergames.bukkit.towny.object.Town townyTown) {
        try {
            return townyTown.getResidents();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Collection<TownBlock> safeGetTownBlocks(com.palmergames.bukkit.towny.object.Town townyTown) {
        try {
            return townyTown.getTownBlocks();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private Collection<com.palmergames.bukkit.towny.object.Town> safeGetNationTowns(com.palmergames.bukkit.towny.object.Nation townyNation) {
        try {
            return townyNation.getTowns();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String resolveCapitalTownId(com.palmergames.bukkit.towny.object.Nation townyNation, Map<UUID, String> townyTownToOurId) {
        try {
            com.palmergames.bukkit.towny.object.Town capital = townyNation.getCapital();
            if (capital != null) {
                String capitalId = townyTownToOurId.get(capital.getUUID());
                if (capitalId != null) return capitalId;
            }
        } catch (Exception ignored) {}

        for (com.palmergames.bukkit.towny.object.Town townyTown : safeGetNationTowns(townyNation)) {
            String townId = townyTownToOurId.get(townyTown.getUUID());
            if (townId != null) return townId;
        }
        return null;
    }

    private Role mapResidentRole(Resident resident) {
        for (String rank : readTownRanks(resident)) {
            String normalized = rank.toLowerCase(Locale.ROOT);
            if (normalized.contains("assistant") || normalized.contains("co-mayor")) return Role.RIGHT_HAND;
            if (normalized.contains("sheriff") || normalized.contains("guard")) return Role.COMMANDER;
            if (normalized.contains("helper")) return Role.LIEUTENANT;
        }
        return Role.CITIZEN;
    }

    @SuppressWarnings("unchecked")
    private Collection<String> readTownRanks(Resident resident) {
        try {
            Object result = resident.getClass().getMethod("getTownRanks").invoke(resident);
            if (result instanceof Collection<?> collection) {
                List<String> ranks = new ArrayList<>();
                for (Object item : collection) {
                    if (item != null) ranks.add(item.toString());
                }
                return ranks;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private ClaimChunk convertTownBlock(TownBlock block) {
        try {
            String worldName = null;

            try {
                Object world = block.getWorld();
                if (world != null) {
                    Object name = world.getClass().getMethod("getName").invoke(world);
                    if (name != null) worldName = name.toString();
                }
            } catch (Exception ignored) {}

            if (worldName == null || worldName.isEmpty()) {
                try {
                    Object worldCoord = block.getClass().getMethod("getWorldCoord").invoke(block);
                    if (worldCoord != null) {
                        Object name = worldCoord.getClass().getMethod("getWorldName").invoke(worldCoord);
                        if (name != null) worldName = name.toString();
                    }
                } catch (Exception ignored) {}
            }

            if (worldName == null || worldName.isEmpty()) return null;
            return new ClaimChunk(worldName, block.getX(), block.getZ());
        } catch (Exception ignored) {
            return null;
        }
    }

    private double readBankBalance(Object accountHolder) {
        try {
            Object account = accountHolder.getClass().getMethod("getAccount").invoke(accountHolder);
            if (account == null) return 0.0;

            Object balance = account.getClass().getMethod("getHoldingBalance").invoke(account);
            if (balance instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private Location readSpawn(Object source) {
        try {
            Object spawn = source.getClass().getMethod("getSpawn").invoke(source);
            if (spawn instanceof Location location) {
                return location;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private Collection<com.palmergames.bukkit.towny.object.Nation> safeGetNationRelations(
        com.palmergames.bukkit.towny.object.Nation townyNation,
        String relationMethodName
    ) {
        try {
            Object result = townyNation.getClass().getMethod(relationMethodName).invoke(townyNation);
            if (!(result instanceof Collection<?> collection)) {
                return Collections.emptyList();
            }

            List<com.palmergames.bukkit.towny.object.Nation> nations = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof com.palmergames.bukkit.towny.object.Nation nation) {
                    nations.add(nation);
                }
            }
            return nations;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
