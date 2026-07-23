package dev.laybalt.skyblocktranslator.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns concrete strings into placeholder templates and back.
 *
 * <p>{@code "Damage: +1,024"} becomes template {@code "Damage: +{0}"} with args
 * {@code ["1,024"]}. One dictionary entry then covers every value the game can
 * show, which is what makes the dictionary + cache approach viable: SkyBlock
 * text is highly repetitive once numbers are factored out.
 */
public final class Normalizer {
	/** Numbers incl. thousands separators, decimals and a trailing %: 1,024 / 3.5 / 100% */
	private static final Pattern NUMBER = Pattern.compile("\\d[\\d,.]*%?");

	private Normalizer() {
	}

	public record Template(String key, List<String> args) {
	}

	/** Extracts numbers into {0}, {1}, ... placeholders. */
	public static Template normalize(String plain) {
		Matcher m = NUMBER.matcher(plain);
		if (!m.find()) {
			return new Template(plain, List.of());
		}
		List<String> args = new ArrayList<>();
		StringBuilder key = new StringBuilder();
		int last = 0;
		do {
			key.append(plain, last, m.start());
			key.append('{').append(args.size()).append('}');
			args.add(m.group());
			last = m.end();
		} while (m.find());
		key.append(plain, last, plain.length());
		return new Template(key.toString(), List.copyOf(args));
	}

	/** Substitutes {n} placeholders in a translated template with the original values. */
	public static String restore(String translated, List<String> args) {
		if (args.isEmpty()) {
			return translated;
		}
		StringBuilder out = new StringBuilder(translated.length());
		for (int i = 0; i < translated.length(); i++) {
			char c = translated.charAt(i);
			if (c == '{') {
				int close = translated.indexOf('}', i);
				if (close > i) {
					try {
						int idx = Integer.parseInt(translated, i + 1, close, 10);
						if (idx >= 0 && idx < args.size()) {
							out.append(args.get(idx));
							i = close;
							continue;
						}
					} catch (NumberFormatException ignored) {
						// not a placeholder, fall through
					}
				}
			}
			out.append(c);
		}
		return out.toString();
	}
}
