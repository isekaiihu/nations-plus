package com.isekai.nationsplus.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;

public class FontUtils {

    private static final Map<Character, Character> SMALLCAPS = new HashMap<>();

    static {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";
        for (int i = 0; i < upper.length(); i++) {
            SMALLCAPS.put(upper.charAt(i), small.charAt(i));
            SMALLCAPS.put(Character.toLowerCase(upper.charAt(i)), small.charAt(i));
        }
    }

    public static String toSmallCaps(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(SMALLCAPS.getOrDefault(c, c));
        }
        return sb.toString();
    }

    /**
     * Converts legacy color formats (&, &#RRGGBB) into section-color text (§, §x§R§R...).
     */
    public static String colorizeLegacy(String text) {
        if (text == null || text.isEmpty()) return "";

        String processed = text.replaceAll("&#([A-Fa-f0-9]{6})", "&x&$1");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < processed.length(); i++) {
            if (i + 8 < processed.length()
                && processed.charAt(i) == '&'
                && processed.charAt(i + 1) == 'x'
                && processed.charAt(i + 2) == '&') {
                sb.append('§').append('x');
                String hex = processed.substring(i + 3, i + 9);
                for (char c : hex.toCharArray()) {
                    sb.append('§').append(c);
                }
                i += 8;
            } else {
                sb.append(processed.charAt(i));
            }
        }

        return sb.toString().replace('&', '§');
    }

    /**
     * Colorize text into a Component (for chat messages).
     */
    public static Component colorize(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(colorizeLegacy(text));
    }

    /**
     * Colorize text into a Component with italic explicitly disabled (for GUI items).
     * Minecraft adds italic by default to custom item names — this removes it.
     */
    public static Component colorizeItem(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(colorizeLegacy(text))
            .decoration(TextDecoration.ITALIC, false);
    }

    public static String stripColors(String text) {
        return text.replaceAll("(?i)(&[0-9a-fk-or]|&#[a-f0-9]{6}|§[0-9a-fk-or]|§x(§[0-9a-f]){6})", "");
    }
}
