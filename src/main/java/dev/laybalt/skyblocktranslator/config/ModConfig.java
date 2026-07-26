package dev.laybalt.skyblocktranslator.config;

import java.nio.file.Path;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import dev.laybalt.skyblocktranslator.ui.ConfigScreens;

/**
 * Mod configuration, rendered with MoulConfig (the NEU/SkyHanni config UI) and
 * persisted by {@link ConfigHolder} as {@code config/skyblock-translator/config.json}.
 *
 * <p>Dropdowns are index-based; use {@link #languageCode()} and
 * {@link #onlineProviderCode()} instead of reading the raw indices.
 */
public class ModConfig extends Config {
	/** Order must match the "Language" dropdown values below. */
	private static final String[] LANGUAGE_CODES = {
			"ru_ru", "uk_ua", "de_de", "fr_fr", "es_es", "pt_br", "pl_pl", "tr_tr", "zh_cn", "ja_jp", "ko_kr"
	};

	@Expose
	@Category(name = "General", desc = "Master switch, language and where to translate")
	public General general = new General();

	@Expose
	@Category(name = "Surfaces", desc = "What gets translated")
	public Surfaces surfaces = new Surfaces();

	@Expose
	@Category(name = "Online Translation", desc = "Free machine translation for unknown text")
	public Online online = new Online();

	/** Reserved for Phase 3 (premium cloud translation). */
	@Expose
	public String premiumKey = "";

	public static class General {
		@Expose
		@ConfigOption(name = "Enable Translation", desc = "Master switch for the whole mod.")
		@ConfigEditorBoolean
		public boolean enabled = true;

		@Expose
		@ConfigOption(name = "Language", desc = "Target language. Russian ships with a curated dictionary; other languages rely on online translation.")
		@ConfigEditorDropdown(values = {
				"Русский", "Українська", "Deutsch", "Français", "Español",
				"Português (BR)", "Polski", "Türkçe", "中文", "日本語", "한국어"
		})
		public int languageIndex = 0;

		@Expose
		@ConfigOption(name = "Only on Hypixel", desc = "Translate only while connected to Hypixel. Disable to test in singleplayer.")
		@ConfigEditorBoolean
		public boolean onlyOnHypixel = true;

		@ConfigOption(name = "Translation Editor", desc = "Edit the most recent strings by hand. Your edits become overrides and always win over the dictionary and the machine translation.")
		@ConfigEditorButton(runnableId = ModConfig.OPEN_EDITOR_RUNNABLE, buttonText = "OPEN")
		public int openEditor = 0;
	}

	public static class Surfaces {
		@Expose
		@ConfigOption(name = "Item Tooltips", desc = "Item names and lore in container screens.")
		@ConfigEditorBoolean
		public boolean items = true;

		@Expose
		@ConfigOption(name = "Menu Titles", desc = "Container titles and labels.")
		@ConfigEditorBoolean
		public boolean menus = true;

		@Expose
		@ConfigOption(name = "NPC Dialogues & System Messages", desc = "Server messages in chat: dialogues, quests, announcements.")
		@ConfigEditorBoolean
		public boolean dialogs = true;

		@Expose
		@ConfigOption(name = "Player Chat", desc = "Messages written by other players.")
		@ConfigEditorBoolean
		public boolean playerChat = false;

		@Expose
		@ConfigOption(name = "Scoreboard Sidebar", desc = "The SKYBLOCK sidebar on the right.")
		@ConfigEditorBoolean
		public boolean scoreboard = true;

		@Expose
		@ConfigOption(name = "Tab List", desc = "Tab entries, header and footer. Player names are never sent to an online translator.")
		@ConfigEditorBoolean
		public boolean tabList = true;

		@Expose
		@ConfigOption(name = "Boss Bar", desc = "Event and objective banners at the top of the screen.")
		@ConfigEditorBoolean
		public boolean bossBar = true;
	}

	public static class Online {
		@Expose
		@ConfigOption(name = "Online Translation", desc = "Translate unknown strings through an online service and cache the result locally. With this off, only dictionaries and the cache are used.")
		@ConfigEditorBoolean
		public boolean translateOnline = true;

		@Expose
		@ConfigOption(name = "Provider", desc = "Google — free, works out of the box. LibreTranslate — your own instance (URL below).")
		@ConfigEditorDropdown(values = {"Google (free)", "LibreTranslate"})
		public int providerIndex = 0;

		@Expose
		@ConfigOption(name = "Daily Request Limit", desc = "Maximum online translation requests per day. Cached translations don't count.")
		@ConfigEditorSlider(minValue = 0, maxValue = 100000, minStep = 500)
		public int dailyBudget = 2000;

		@Expose
		@ConfigOption(name = "LibreTranslate URL", desc = "Instance address, e.g. https://libretranslate.example.com")
		@ConfigEditorText
		public String libreUrl = "";

		@Expose
		@ConfigOption(name = "LibreTranslate API Key", desc = "Leave empty if the instance doesn't require one.")
		@ConfigEditorText
		public String libreApiKey = "";

		@Expose
		@ConfigOption(name = "Collect Untranslated Strings", desc = "Append unknown templates to untranslated-<lang>.txt — used to grow the bundled dictionaries.")
		@ConfigEditorBoolean
		public boolean dumpUntranslated = true;
	}

	static final int OPEN_EDITOR_RUNNABLE = 0;

	@Override
	public void executeRunnable(int runnableId) {
		if (runnableId == OPEN_EDITOR_RUNNABLE) {
			Minecraft.getInstance().setScreenAndShow(ConfigScreens.createEditor(null));
		}
	}

	public String languageCode() {
		int idx = general.languageIndex;
		return LANGUAGE_CODES[idx >= 0 && idx < LANGUAGE_CODES.length ? idx : 0];
	}

	public String onlineProviderCode() {
		return online.providerIndex == 1 ? "libretranslate" : "google";
	}

	public static ModConfig get() {
		return ConfigHolder.get();
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("skyblock-translator");
	}

	public void save() {
		ConfigHolder.save();
	}
}
