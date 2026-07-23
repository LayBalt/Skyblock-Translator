package dev.laybalt.skyblocktranslator.pipeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;
import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.providers.DictionaryProvider;
import dev.laybalt.skyblocktranslator.providers.LocalCacheProvider;
import dev.laybalt.skyblocktranslator.providers.TranslationProvider;

/**
 * Render-side translation: takes the {@link Component} that is about to be drawn
 * and returns a translated replacement, or the original when nothing matches.
 *
 * <p>Resolution order: bundled dictionary → local cache (→ remote providers in
 * later phases). Results are memoized per flattened legacy string because
 * tooltips are rebuilt every frame. Unknown templates are appended to an
 * {@code untranslated-<lang>.txt} dump, which is how the dictionaries get grown.
 */
public final class TranslationEngine {
	private static final int MEMO_LIMIT = 4096;

	private static TranslationEngine instance;

	private final List<TranslationProvider> providers;
	private final LocalCacheProvider cache;

	/** Legacy string -> rendered component. Guarded by itself (render thread + safety). */
	private final Map<String, Component> memo = new LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
			return size() > MEMO_LIMIT;
		}
	};

	private final Set<String> dumped = ConcurrentHashMap.newKeySet();
	private final Path dumpFile;

	private TranslationEngine(String language) {
		this.cache = new LocalCacheProvider(ModConfig.directory(), language);
		this.providers = List.of(new DictionaryProvider(language), cache);
		this.dumpFile = ModConfig.directory().resolve("untranslated-" + language + ".txt");
		if (Files.exists(dumpFile)) {
			try {
				dumped.addAll(Files.readAllLines(dumpFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				SkyblockTranslatorClient.LOGGER.warn("Could not read {}", dumpFile, e);
			}
		}
	}

	public static void init() {
		instance = new TranslationEngine(ModConfig.get().language);
	}

	public static TranslationEngine get() {
		if (instance == null) {
			init();
		}
		return instance;
	}

	public LocalCacheProvider cache() {
		return cache;
	}

	/** Translates one component; returns the original instance when there is no translation. */
	public Component translate(Component original) {
		String legacy = LegacyText.toLegacy(original);
		if (legacy.isBlank()) {
			return original;
		}
		synchronized (memo) {
			Component known = memo.get(legacy);
			if (known != null) {
				return known;
			}
		}
		Component result = compute(legacy, original);
		synchronized (memo) {
			memo.put(legacy, result);
		}
		return result;
	}

	/** Translates a tooltip line list; returns the same list instance when nothing changed. */
	public List<Component> translateLines(List<Component> lines) {
		List<Component> out = null;
		for (int i = 0; i < lines.size(); i++) {
			Component original = lines.get(i);
			Component translated = translate(original);
			if (translated != original && out == null) {
				out = new ArrayList<>(lines);
			}
			if (out != null) {
				out.set(i, translated);
			}
		}
		return out != null ? out : lines;
	}

	private Component compute(String legacy, Component original) {
		String plain = LegacyText.stripCodes(legacy);
		if (plain.isBlank()) {
			return original;
		}
		Normalizer.Template template = Normalizer.normalize(plain);
		for (TranslationProvider provider : providers) {
			String translation = provider.lookup(template.key());
			if (translation != null) {
				String restored = Normalizer.restore(translation, template.args());
				return Component.literal(LegacyText.leadingCodes(legacy) + restored);
			}
		}
		recordMissing(template.key());
		return original;
	}

	private void recordMissing(String templateKey) {
		if (!ModConfig.get().dumpUntranslated || !hasLetters(templateKey) || !dumped.add(templateKey)) {
			return;
		}
		try {
			Files.createDirectories(dumpFile.getParent());
			Files.writeString(dumpFile, templateKey + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			SkyblockTranslatorClient.LOGGER.warn("Could not append to {}", dumpFile, e);
		}
	}

	private static boolean hasLetters(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isLetter(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
