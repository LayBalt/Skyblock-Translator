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
import org.jetbrains.annotations.Nullable;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;
import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.providers.DictionaryProvider;
import dev.laybalt.skyblocktranslator.providers.LocalCacheProvider;
import dev.laybalt.skyblocktranslator.providers.OverrideProvider;
import dev.laybalt.skyblocktranslator.providers.TranslationProvider;
import dev.laybalt.skyblocktranslator.remote.GoogleFreeTranslator;
import dev.laybalt.skyblocktranslator.remote.LibreTranslator;
import dev.laybalt.skyblocktranslator.remote.RemoteQueue;
import dev.laybalt.skyblocktranslator.remote.RemoteTranslator;

/**
 * Render-side translation: takes the {@link Component} that is about to be drawn
 * and returns a translated replacement, or the original when nothing matches yet.
 *
 * <p>Resolution order: user overrides → bundled dictionary → local cache. Misses
 * are queued for online translation; when a result lands in the cache the memo is
 * flushed, and because tooltips are rebuilt every frame the text updates live.
 * Unknown templates are also appended to {@code untranslated-<lang>.txt}, which
 * is how the bundled dictionaries get grown.
 */
public final class TranslationEngine {
	private static final int MEMO_LIMIT = 4096;
	private static final int RECENT_LIMIT = 200;

	private static volatile TranslationEngine instance;

	private final List<TranslationProvider> providers;
	private final OverrideProvider overrides;
	private final LocalCacheProvider cache;
	@Nullable
	private final RemoteQueue remoteQueue;

	/** Legacy string -> rendered component. Guarded by itself. */
	private final Map<String, Component> memo = new LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
			return size() > MEMO_LIMIT;
		}
	};

	/** Recently seen templates (translation or null) — feeds the in-game string editor. */
	private final Map<String, String> recentSeen = new LinkedHashMap<>(64, 0.75f, false) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
			return size() > RECENT_LIMIT;
		}
	};

	/** Templates the MT provider mangled (placeholder mismatch) — don't retry, don't cache. */
	private final Set<String> rejected = ConcurrentHashMap.newKeySet();

	private final Set<String> dumped = ConcurrentHashMap.newKeySet();
	private final Path dumpFile;

	private TranslationEngine(ModConfig config) {
		String language = config.language;
		this.overrides = new OverrideProvider(ModConfig.directory(), language);
		this.cache = new LocalCacheProvider(ModConfig.directory(), language);
		this.providers = List.of(overrides, new DictionaryProvider(language), cache);
		this.dumpFile = ModConfig.directory().resolve("untranslated-" + language + ".txt");
		if (Files.exists(dumpFile)) {
			try {
				dumped.addAll(Files.readAllLines(dumpFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				SkyblockTranslatorClient.LOGGER.warn("Could not read {}", dumpFile, e);
			}
		}
		this.remoteQueue = config.translateOnline
				? new RemoteQueue(createTranslator(config), mtLang(language), config.dailyOnlineBudget,
						ModConfig.directory(), this::onRemoteResult)
				: null;
	}

	private static RemoteTranslator createTranslator(ModConfig config) {
		if ("libretranslate".equals(config.onlineProvider) && !config.libreTranslateUrl.isBlank()) {
			return new LibreTranslator(config.libreTranslateUrl, config.libreTranslateApiKey);
		}
		return new GoogleFreeTranslator();
	}

	/** "ru_ru" -> "ru" — MT services want plain ISO codes. */
	private static String mtLang(String language) {
		int idx = language.indexOf('_');
		return idx > 0 ? language.substring(0, idx) : language;
	}

	public static void init() {
		instance = new TranslationEngine(ModConfig.get());
	}

	/** Recreates the engine after config changes (language, provider, toggles). */
	public static synchronized void reload() {
		TranslationEngine old = instance;
		if (old != null) {
			old.shutdown();
		}
		instance = new TranslationEngine(ModConfig.get());
	}

	public static TranslationEngine get() {
		if (instance == null) {
			init();
		}
		return instance;
	}

	public OverrideProvider overrides() {
		return overrides;
	}

	public LocalCacheProvider cache() {
		return cache;
	}

	/** Newest-first snapshot of recently seen templates for the string editor UI. */
	public List<Map.Entry<String, String>> recentSeenSnapshot() {
		synchronized (recentSeen) {
			List<Map.Entry<String, String>> list = new ArrayList<>(recentSeen.size());
			for (Map.Entry<String, String> e : recentSeen.entrySet()) {
				list.add(Map.entry(e.getKey(), e.getValue() == null ? "" : e.getValue()));
			}
			java.util.Collections.reverse(list);
			return list;
		}
	}

	public void shutdown() {
		if (remoteQueue != null) {
			remoteQueue.shutdown();
		}
		cache.save();
		overrides.save();
	}

	/** Drops memoized results so edited/new translations take effect immediately. */
	public void flushMemo() {
		synchronized (memo) {
			memo.clear();
		}
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
		Normalizer.Template template = Normalizer.normalize(legacy);
		for (TranslationProvider provider : providers) {
			String translation = provider.lookup(template.key());
			if (translation != null) {
				remember(template.key(), translation);
				String restored = Normalizer.restore(translation, template.args());
				return Component.literal(LegacyText.leadingCodes(legacy) + restored);
			}
		}
		remember(template.key(), null);
		recordMissing(template.key());
		if (remoteQueue != null && !rejected.contains(template.key()) && hasLetters(template.key())) {
			remoteQueue.submit(template.key());
		}
		return original;
	}

	private int remoteResults;

	/** Called from the remote worker thread when an online translation arrives. */
	private void onRemoteResult(String templateKey, String translation) {
		if (Normalizer.placeholderCount(translation) != Normalizer.placeholderCount(templateKey)) {
			rejected.add(templateKey);
			SkyblockTranslatorClient.LOGGER.debug("MT mangled placeholders, rejected: {}", templateKey);
			return;
		}
		cache.put(templateKey, translation);
		if (++remoteResults % 20 == 0) {
			cache.save(); // don't lose a session's worth of MT on a crash
		}
		remember(templateKey, translation);
		flushMemo();
	}

	private void remember(String templateKey, @Nullable String translation) {
		synchronized (recentSeen) {
			recentSeen.remove(templateKey);
			recentSeen.put(templateKey, translation);
		}
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
