package dev.laybalt.skyblocktranslator.providers;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;

/**
 * Per-user translation cache persisted in the config directory.
 *
 * <p>Remote providers (free MT now, premium later) drop their results here so a
 * string is only ever translated once. The file is plain JSON, so users can also
 * hand-edit it to override or add translations.
 */
public final class LocalCacheProvider implements TranslationProvider {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final TypeToken<Map<String, String>> MAP_TYPE = new TypeToken<>() {
	};

	private final Path file;
	private final Map<String, String> entries = new ConcurrentHashMap<>();
	private volatile boolean dirty;

	public LocalCacheProvider(Path configDir, String language) {
		this.file = configDir.resolve("cache-" + language + ".json");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
				if (loaded != null) {
					entries.putAll(loaded);
				}
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.error("Failed to load translation cache {}", file, e);
			}
		}
	}

	@Override
	@Nullable
	public String lookup(String templateKey) {
		return entries.get(templateKey);
	}

	public void put(String templateKey, String translation) {
		entries.put(templateKey, translation);
		dirty = true;
	}

	/** Writes the cache back to disk if anything changed since the last save. */
	public void save() {
		if (!dirty) {
			return;
		}
		dirty = false;
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(entries, writer);
			}
		} catch (IOException e) {
			SkyblockTranslatorClient.LOGGER.error("Failed to save translation cache {}", file, e);
		}
	}
}
