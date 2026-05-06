package com.nemonichorse.util;

import com.nemonichorse.model.HorseData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {

    private TextUtil() {}

    public static String color(String s) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static Component colorComponent(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    /** Build the display name Component for a horse entity. */
    public static Component buildHorseDisplayName(HorseData data) {
        String name = data.getName() != null ? data.getName() : "Sem Nome";
        String colorCode = data.getRarity().getColorCode();
        String raw = colorCode + name + " &8[&eLv." + data.getLevel() + "&8]";
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    /** Replace placeholders in message templates from config.yml. */
    public static String format(String template, Object... pairs) {
        String result = template;
        for (int i = 0; i < pairs.length - 1; i += 2) {
            result = result.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return color(result);
    }

    public static TextColor hexColor(int rgb) {
        return TextColor.color(rgb);
    }
}
