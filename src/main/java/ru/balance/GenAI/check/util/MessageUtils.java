package ru.balance.GenAI.check.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

public class MessageUtils {
    public static Component parseMessage(String raw) {
        TextComponent.Builder builder = Component.text();
        int i = 0;
        TextColor currentColor = null;
        StringBuilder sb = new StringBuilder();

        while (i < raw.length()) {
            char c = raw.charAt(i);

            if (c == '#' && i + 6 < raw.length()) {
                String hex = raw.substring(i, i + 7);
                if (hex.matches("#[A-Fa-f0-9]{6}")) {
                    if (sb.length() > 0) {
                        builder.append(Component.text(sb.toString(), currentColor));
                        sb = new StringBuilder();
                    }
                    currentColor = TextColor.fromHexString(hex);
                    i += 7;
                    continue;
                }
            }

            if (c == '&' && i + 1 < raw.length()) {
                char code = raw.charAt(i + 1);
                currentColor = TextColor.color(net.md_5.bungee.api.ChatColor.of("&" + code).getColor().getRGB());
                i += 2;
                continue;
            }

            sb.append(c);
            i++;
        }

        if (sb.length() > 0) {
            builder.append(Component.text(sb.toString(), currentColor));
        }

        return builder.build();
    }
}

