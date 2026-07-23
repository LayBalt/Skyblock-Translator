package dev.laybalt.skyblocktranslator.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns concrete strings into placeholder templates and back.
 *
 * <p>{@code "§7Damage: §c+1,024"} becomes template {@code "Damage: {0}"} with the
 * argument keeping its sign and color codes ({@code "§c+1,024"}). One dictionary entry then
 * covers every value the game can show, and numbers stay colored after
 * substitution — SkyBlock loves coloring the value differently from the label.
 *
 * <p>Template keys are always §-stripped; the input may be a plain string or a
 * legacy string with codes.
 */
public final class Normalizer {
	private static final char SECTION = '§';

	private Normalizer() {
	}

	/**
	 * @param raw   the exact matched substring, codes included ("§c1,024")
	 * @param after codes re-applied after the value so the following text gets its
	 *              original color back; empty when the value had no codes of its own
	 */
	public record Arg(String raw, String after) {
	}

	public record Template(String key, List<Arg> args) {
	}

	/** Extracts numbers (with their adjacent §-codes) into {0}, {1}, ... placeholders. */
	public static Template normalize(String legacy) {
		StringBuilder key = new StringBuilder(legacy.length());
		List<Arg> args = null;

		// Codes seen since the last plain char; they color whatever comes next.
		StringBuilder pending = new StringBuilder();
		// Codes that were active before the pending run — the "surrounding" style.
		String activeBefore = "";
		StringBuilder active = new StringBuilder();

		int i = 0;
		while (i < legacy.length()) {
			char c = legacy.charAt(i);
			if (c == SECTION && i + 1 < legacy.length()) {
				if (pending.isEmpty()) {
					activeBefore = active.toString();
				}
				pending.append(c).append(legacy.charAt(i + 1));
				i += 2;
				continue;
			}
			boolean signedNumber = (c == '+' || c == '-')
					&& i + 1 < legacy.length() && Character.isDigit(legacy.charAt(i + 1));
			if (Character.isDigit(c) || signedNumber) {
				int end = i + 1;
				while (end < legacy.length() && (Character.isDigit(legacy.charAt(end))
						|| legacy.charAt(end) == ',' || legacy.charAt(end) == '.')) {
					end++;
				}
				// trailing . or , belongs to the sentence, not the number
				while (legacy.charAt(end - 1) == ',' || legacy.charAt(end - 1) == '.') {
					end--;
				}
				if (end < legacy.length() && legacy.charAt(end) == '%') {
					end++;
				}
				if (args == null) {
					args = new ArrayList<>();
				}
				String number = legacy.substring(i, end);
				String after = pending.isEmpty() ? "" : activeBefore;
				key.append('{').append(args.size()).append('}');
				args.add(new Arg(pending + number, after));
				applyCodes(active, pending);
				pending.setLength(0);
				i = end;
				continue;
			}
			applyCodes(active, pending);
			pending.setLength(0);
			key.append(c);
			i++;
		}
		return new Template(key.toString(), args == null ? List.of() : List.copyOf(args));
	}

	/** Legacy semantics: a color code resets the run, format codes stack, §r clears. */
	private static void applyCodes(StringBuilder active, CharSequence codes) {
		for (int i = 0; i + 1 < codes.length(); i += 2) {
			char code = Character.toLowerCase(codes.charAt(i + 1));
			if (code == 'r') {
				active.setLength(0);
			} else if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
				active.setLength(0);
				active.append(SECTION).append(code);
			} else {
				active.append(SECTION).append(code);
			}
		}
	}

	/** Substitutes {n} placeholders in a translated template with the original values. */
	public static String restore(String translated, List<Arg> args) {
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
							Arg arg = args.get(idx);
							out.append(arg.raw()).append(arg.after());
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

	/** Number of distinct placeholders a template refers to; used to validate MT output. */
	public static int placeholderCount(String template) {
		int count = 0;
		for (int i = 0; i < template.length() - 1; i++) {
			if (template.charAt(i) == '{' && Character.isDigit(template.charAt(i + 1))) {
				int close = template.indexOf('}', i);
				if (close > i) {
					count++;
					i = close;
				}
			}
		}
		return count;
	}
}
