package com.isekai.nationsplus.managers;

import com.isekai.nationsplus.NationsPlus;
import com.isekai.nationsplus.data.*;
import com.isekai.nationsplus.utils.FontUtils;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;

import java.awt.Color;
import java.util.*;

public class DynmapManager {

    private final NationsPlus plugin;
    private DynmapAPI dynmapAPI;
    private MarkerAPI markerAPI;
    private MarkerSet townMarkerSet;
    private MarkerSet nationMarkerSet;
    private MarkerSet warMarkerSet;
    private boolean enabled;

    public DynmapManager(NationsPlus plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("dynmap.enabled", true);
        if (enabled) setup();
    }

    private void setup() {
        org.bukkit.plugin.Plugin dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null || !dynmapPlugin.isEnabled()) {
            plugin.getLogger().warning("Dynmap not found! Map features disabled.");
            enabled = false;
            return;
        }

        try {
            dynmapAPI = (DynmapAPI) dynmapPlugin;
            markerAPI = dynmapAPI.getMarkerAPI();
            if (markerAPI == null) {
                plugin.getLogger().warning("Dynmap MarkerAPI not available!");
                enabled = false;
                return;
            }

            // Create marker sets
            String townLayerName = plugin.getConfig().getString("dynmap.layer-towns.name", "Towns");
            townMarkerSet = markerAPI.getMarkerSet("nationsplus.towns");
            if (townMarkerSet == null) {
                townMarkerSet = markerAPI.createMarkerSet("nationsplus.towns", townLayerName, null, false);
            }
            townMarkerSet.setLayerPriority(plugin.getConfig().getInt("dynmap.layer-towns.priority", 10));
            townMarkerSet.setHideByDefault(!plugin.getConfig().getBoolean("dynmap.layer-towns.show-by-default", true));

            String nationLayerName = plugin.getConfig().getString("dynmap.layer-nations.name", "Nations");
            nationMarkerSet = markerAPI.getMarkerSet("nationsplus.nations");
            if (nationMarkerSet == null) {
                nationMarkerSet = markerAPI.createMarkerSet("nationsplus.nations", nationLayerName, null, false);
            }
            nationMarkerSet.setLayerPriority(plugin.getConfig().getInt("dynmap.layer-nations.priority", 11));
            nationMarkerSet.setHideByDefault(!plugin.getConfig().getBoolean("dynmap.layer-nations.show-by-default", true));

            String warLayerName = plugin.getConfig().getString("dynmap.layer-war.name", "War Zones");
            warMarkerSet = markerAPI.getMarkerSet("nationsplus.war");
            if (warMarkerSet == null) {
                warMarkerSet = markerAPI.createMarkerSet("nationsplus.war", warLayerName, null, false);
            }
            warMarkerSet.setLayerPriority(plugin.getConfig().getInt("dynmap.layer-war.priority", 12));
            warMarkerSet.setHideByDefault(!plugin.getConfig().getBoolean("dynmap.layer-war.show-by-default", false));

            plugin.getLogger().info("Dynmap integration enabled!");

            // Initial render
            plugin.getServer().getScheduler().runTaskLater(plugin, this::renderAll, 40L);

        } catch (Exception e) {
            plugin.getLogger().warning("Dynmap setup failed: " + e.getMessage());
            enabled = false;
        }
    }

    public void renderAll() {
        if (!enabled) return;

        // Clear and re-render all towns
        for (Town town : plugin.getTownManager().getAllTowns()) {
            updateTownClaims(town);
            updateTownIcon(town);
        }
    }

    public void updateTownClaims(Town town) {
        if (!enabled || townMarkerSet == null) return;

        // Remove old markers for this town
        String markerId = "town_area_" + town.getId();
        AreaMarker existing = townMarkerSet.findAreaMarker(markerId);
        if (existing != null) existing.deleteMarker();

        if (town.getClaims().isEmpty()) return;

        // Build polygon from chunks
        Set<ClaimChunk> claims = town.getClaims();
        List<double[]> outline = buildChunkOutline(claims);
        if (outline.isEmpty()) return;

        double[] xCoords = new double[outline.size()];
        double[] zCoords = new double[outline.size()];
        for (int i = 0; i < outline.size(); i++) {
            xCoords[i] = outline.get(i)[0];
            zCoords[i] = outline.get(i)[1];
        }

        String world = claims.iterator().next().world();
        AreaMarker area = townMarkerSet.createAreaMarker(markerId,
            FontUtils.stripColors(town.getName()), false, world, xCoords, zCoords, false);

        if (area != null) {
            // Use town's custom color if set, otherwise default
            int fillColor;
            if (town.getColorHex() != null && !town.getColorHex().isEmpty()) {
                fillColor = parseColor(town.getColorHex());
            } else {
                fillColor = parseColor(plugin.getConfig().getString("dynmap.default-fill-color", "#44AA44"));
            }

            double fillOpacity = plugin.getConfig().getDouble("dynmap.fill-opacity", 0.35);
            double borderOpacity = plugin.getConfig().getDouble("dynmap.border-opacity", 0.8);
            int borderWeight = plugin.getConfig().getInt("dynmap.border-weight", 3);
            double darkFactor = plugin.getConfig().getDouble("dynmap.border-darkness-factor", 0.6);

            // Check if at war
            if (town.getNationId() != null) {
                List<WarData> wars = plugin.getWarManager().getWarsForNation(town.getNationId());
                if (!wars.isEmpty()) {
                    fillColor = parseColor(plugin.getConfig().getString("dynmap.war-fill-color", "#FF4444"));
                }
            }

            int borderColor = darkenColor(fillColor, darkFactor);
            area.setFillStyle(fillOpacity, fillColor);
            area.setLineStyle(borderWeight, borderOpacity, borderColor);

            // Tooltip — include nation info below town info
            String tooltip = plugin.getConfig().getString("dynmap.tooltip-format",
                "<b>{name}</b><br>Leader: {leader}<br>Population: {population}<br>Bank: ${balance}");
            String leaderName = Bukkit.getOfflinePlayer(town.getLeader()).getName();
            tooltip = tooltip.replace("{name}", FontUtils.stripColors(town.getName()))
                .replace("{leader}", leaderName != null ? leaderName : "Unknown")
                .replace("{population}", String.valueOf(town.getMemberCount()))
                .replace("{balance}", String.format("%.0f", town.getBank()));

            // Append nation info if in a nation
            if (town.getNationId() != null) {
                Nation nation = plugin.getNationManager().getNation(town.getNationId());
                if (nation != null) {
                    tooltip += "<br>Nation: <b>" + FontUtils.stripColors(nation.getName()) + "</b>";
                }
            }

            area.setDescription(tooltip);
        }
    }

    public void updateTownIcon(Town town) {
        if (!enabled || townMarkerSet == null || town.getSpawn() == null) return;

        String iconId = "town_icon_" + town.getId();
        Marker existing = townMarkerSet.findMarker(iconId);
        if (existing != null) existing.deleteMarker();

        String iconName = plugin.getConfig().getString("dynmap.town-icon", "flag");
        MarkerIcon icon = markerAPI.getMarkerIcon(iconName);
        if (icon == null) icon = markerAPI.getMarkerIcon("default");

        townMarkerSet.createMarker(iconId,
            FontUtils.stripColors(town.getName()), town.getSpawn().getWorld().getName(),
            town.getSpawn().getX(), town.getSpawn().getY(), town.getSpawn().getZ(),
            icon, false);
    }

    public void removeTownMarker(String townId) {
        if (!enabled || townMarkerSet == null) return;
        AreaMarker area = townMarkerSet.findAreaMarker("town_area_" + townId);
        if (area != null) area.deleteMarker();
        Marker icon = townMarkerSet.findMarker("town_icon_" + townId);
        if (icon != null) icon.deleteMarker();
    }

    public void removeNationMarker(String nationId) {
        if (!enabled || nationMarkerSet == null) return;
        Marker icon = nationMarkerSet.findMarker("nation_icon_" + nationId);
        if (icon != null) icon.deleteMarker();
    }

    /**
     * Build a simple outline polygon from a set of chunk claims.
     * Returns list of [x, z] coordinates for the outline.
     */
    private List<double[]> buildChunkOutline(Set<ClaimChunk> claims) {
        if (claims.isEmpty()) return Collections.emptyList();

        Set<long[]> edges = new HashSet<>();
        List<double[]> points = new ArrayList<>();

        for (ClaimChunk chunk : claims) {
            int bx = chunk.x() * 16;
            int bz = chunk.z() * 16;
            points.add(new double[]{bx, bz});
            points.add(new double[]{bx + 16, bz});
            points.add(new double[]{bx + 16, bz + 16});
            points.add(new double[]{bx, bz + 16});
        }

        // Compute convex hull for a clean polygon
        return convexHull(points);
    }

    private List<double[]> convexHull(List<double[]> points) {
        if (points.size() < 3) return points;

        points.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));

        // Remove duplicates
        List<double[]> unique = new ArrayList<>();
        for (double[] p : points) {
            boolean dup = false;
            for (double[] u : unique) {
                if (u[0] == p[0] && u[1] == p[1]) { dup = true; break; }
            }
            if (!dup) unique.add(p);
        }
        if (unique.size() < 3) return unique;

        int n = unique.size();
        double[][] hull = new double[2 * n][];
        int k = 0;

        for (int i = 0; i < n; i++) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], unique.get(i)) <= 0) k--;
            hull[k++] = unique.get(i);
        }

        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(hull[k - 2], hull[k - 1], unique.get(i)) <= 0) k--;
            hull[k++] = unique.get(i);
        }

        return Arrays.asList(Arrays.copyOf(hull, k - 1));
    }

    private double cross(double[] O, double[] A, double[] B) {
        return (A[0] - O[0]) * (B[1] - O[1]) - (A[1] - O[1]) * (B[0] - O[0]);
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0x44AA44;
        }
    }

    private int darkenColor(int color, double factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    public boolean isEnabled() { return enabled; }
}