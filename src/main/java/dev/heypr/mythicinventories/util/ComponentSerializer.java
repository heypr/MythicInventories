package dev.heypr.mythicinventories.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ComponentSerializer {

    private ComponentSerializer() { }
    /**
     * Deserialize a text component from a string.
     *
     * @param text The text to deserialize.
     * @return The deserialized text component.
     */
    public static TextComponent deserializeText(String text) {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage mm = MiniMessage.miniMessage();
        return legacy.deserialize(legacy.serialize(mm.deserialize(text).asComponent()));
    }

    public static Component applyDefaultFormatting(String text) {
        return deserializeText(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
