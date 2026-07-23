package dev.laybalt.skyblocktranslator.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;

/**
 * Mod configuration, stored as {@code config/skyblock-translator/config.json}.
 * A missing file is created with defaults on first launch.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance;

	public boolean enabled = true;
	/** Dictionary/cache language, matches the dict folder name. */
	public String language = "ru_ru";
	/** Only translate while connected to Hypixel. Turn off to test in singleplayer. */
	public boolean onlyOnHypixel = true;
	public boolean translateItems = true;
	public boolean translateMenus = true;
	/** NPC dialogues and other server system messages in chat. */
	public boolean translateDialogs = true;
	/** Messages written by other players; off by default. */
	public boolean translatePlayerChat = false;
	/** Send unknown strings to an online translator and cache the results. */
	public boolean translateOnline = true;
	/** "google" (free, zero setup) or "libretranslate" (own instance below). */
	public String onlineProvider = "google";
	public String libreTranslateUrl = "";
	public String libreTranslateApiKey = "";
	/** Max online translation requests per day. */
	public int dailyOnlineBudget = 2000;
	/** Append unknown templates to config/skyblock-translator/untranslated-<lang>.txt. */
	public boolean dumpUntranslated = true;
	/** Reserved for Phase 3 (premium cloud translation). */
	public String premiumKey = "";

	public static ModConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("skyblock-translator");
	}

	private static Path file() {
		return directory().resolve("config.json");
	}

	private static ModConfig load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.error("Failed to load config, using defaults", e);
			}
		}
		ModConfig fresh = new ModConfig();
		fresh.save();
		return fresh;
	}

	public void save() {
		try {
			Files.createDirectories(directory());
			try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			SkyblockTranslatorClient.LOGGER.error("Failed to save config", e);
		}
	}
}
