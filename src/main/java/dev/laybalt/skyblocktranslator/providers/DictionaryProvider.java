package dev.laybalt.skyblocktranslator.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;

/**
 * Bundled, curated dictionaries shipped inside the mod jar.
 *
 * <p>Layout: {@code assets/skyblock-translator/dict/<lang>/_index.json} lists the
 * dictionary files of that language; each file is a flat
 * {@code "english template" -> "translation"} JSON object. Listing files in an
 * index avoids classpath directory enumeration and keeps loading trivial.
 */
public final class DictionaryProvider implements TranslationProvider {
	private static final Gson GSON = new Gson();
	private static final TypeToken<Map<String, String>> MAP_TYPE = new TypeToken<>() {
	};
	private static final TypeToken<java.util.List<String>> LIST_TYPE = new TypeToken<>() {
	};

	private final Map<String, String> entries = new HashMap<>();

	public DictionaryProvider(String language) {
		String base = "/assets/skyblock-translator/dict/" + language + "/";
		for (String file : readIndex(base)) {
			try (Reader reader = open(base + file)) {
				if (reader == null) {
					SkyblockTranslatorClient.LOGGER.warn("Dictionary file listed in index but missing: {}{}", base, file);
					continue;
				}
				Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
				if (loaded != null) {
					entries.putAll(loaded);
				}
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.error("Failed to load dictionary {}{}", base, file, e);
			}
		}
		SkyblockTranslatorClient.LOGGER.info("Loaded {} dictionary entries for {}", entries.size(), language);
	}

	private static java.util.List<String> readIndex(String base) {
		try (Reader reader = open(base + "_index.json")) {
			if (reader == null) {
				return java.util.List.of();
			}
			java.util.List<String> index = GSON.fromJson(reader, LIST_TYPE);
			return index != null ? index : java.util.List.of();
		} catch (IOException | RuntimeException e) {
			SkyblockTranslatorClient.LOGGER.error("Failed to read dictionary index {}", base, e);
			return java.util.List.of();
		}
	}

	@Nullable
	private static Reader open(String resourcePath) {
		InputStream in = DictionaryProvider.class.getResourceAsStream(resourcePath);
		return in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	@Override
	@Nullable
	public String lookup(String templateKey) {
		return entries.get(templateKey);
	}

	public int size() {
		return entries.size();
	}
}
