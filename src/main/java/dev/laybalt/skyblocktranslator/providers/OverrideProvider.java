package dev.laybalt.skyblocktranslator.providers;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;

/**
 * User-defined translations with the highest priority — the in-game string
 * editor writes here, and hand edits to the JSON file work too. Beats both the
 * bundled dictionary and the MT cache, so a bad machine translation can always
 * be corrected permanently.
 */
public final class OverrideProvider implements TranslationProvider {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final TypeToken<Map<String, String>> MAP_TYPE = new TypeToken<>() {
	};

	private final Path file;
	private final Map<String, String> entries = new LinkedHashMap<>();

	public OverrideProvider(Path configDir, String language) {
		this.file = configDir.resolve("overrides-" + language + ".json");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
				if (loaded != null) {
					entries.putAll(loaded);
				}
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.error("Failed to load overrides {}", file, e);
			}
		}
	}

	@Override
	@Nullable
	public synchronized String lookup(String templateKey) {
		return entries.get(templateKey);
	}

	public synchronized void put(String templateKey, String translation) {
		if (translation == null || translation.isBlank()) {
			entries.remove(templateKey);
		} else {
			entries.put(templateKey, translation);
		}
	}

	public synchronized void save() {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(entries, writer);
			}
		} catch (IOException e) {
			SkyblockTranslatorClient.LOGGER.error("Failed to save overrides {}", file, e);
		}
	}
}
