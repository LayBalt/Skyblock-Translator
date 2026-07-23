package dev.laybalt.skyblocktranslator.pipeline;

import java.util.Map;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Converts between {@link Component} trees and "legacy" strings with §-codes.
 *
 * <p>Hypixel text reaches the client either as literals with embedded §-codes or as
 * styled component siblings. We flatten everything into one legacy string so the
 * rest of the pipeline works on plain strings. The client still renders §-codes
 * inside literal components, so the translated result can be a single literal.
 */
public final class LegacyText {
	private static final char SECTION = '§';

	/** Legacy color table: ARGB-less RGB value -> §-code char. */
	private static final Map<Integer, Character> COLOR_CODES = Map.ofEntries(
		Map.entry(0x000000, '0'), Map.entry(0x0000AA, '1'), Map.entry(0x00AA00, '2'),
		Map.entry(0x00AAAA, '3'), Map.entry(0xAA0000, '4'), Map.entry(0xAA00AA, '5'),
		Map.entry(0xFFAA00, '6'), Map.entry(0xAAAAAA, '7'), Map.entry(0x555555, '8'),
		Map.entry(0x5555FF, '9'), Map.entry(0x55FF55, 'a'), Map.entry(0x55FFFF, 'b'),
		Map.entry(0xFF5555, 'c'), Map.entry(0xFF55FF, 'd'), Map.entry(0xFFFF55, 'e'),
		Map.entry(0xFFFFFF, 'f'));

	private LegacyText() {
	}

	/** Flattens a component tree into a single legacy string with §-codes. */
	public static String toLegacy(Component component) {
		StringBuilder out = new StringBuilder();
		component.visit((style, text) -> {
			out.append(codesFor(style));
			out.append(text);
			return Optional.empty();
		}, Style.EMPTY);
		return out.toString();
	}

	private static String codesFor(Style style) {
		if (style.isEmpty()) {
			return "";
		}
		StringBuilder codes = new StringBuilder();
		TextColor color = style.getColor();
		if (color != null) {
			Character code = COLOR_CODES.get(color.getValue() & 0xFFFFFF);
			if (code != null) {
				codes.append(SECTION).append(code);
			}
		}
		if (style.isObfuscated()) codes.append(SECTION).append('k');
		if (style.isBold()) codes.append(SECTION).append('l');
		if (style.isStrikethrough()) codes.append(SECTION).append('m');
		if (style.isUnderlined()) codes.append(SECTION).append('n');
		if (style.isItalic()) codes.append(SECTION).append('o');
		return codes.toString();
	}

	/** Removes every §-code from a legacy string. */
	public static String stripCodes(String legacy) {
		if (legacy.indexOf(SECTION) < 0) {
			return legacy;
		}
		StringBuilder out = new StringBuilder(legacy.length());
		for (int i = 0; i < legacy.length(); i++) {
			char c = legacy.charAt(i);
			if (c == SECTION && i + 1 < legacy.length()) {
				i++; // skip the code char
			} else if (c != SECTION) {
				out.append(c);
			}
		}
		return out.toString();
	}

	/** Returns the run of §-codes at the very start of a legacy string ("§7§o" for "§7§oText"). */
	public static String leadingCodes(String legacy) {
		int i = 0;
		while (i + 1 < legacy.length() && legacy.charAt(i) == SECTION) {
			i += 2;
		}
		return legacy.substring(0, i);
	}
}
