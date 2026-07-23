package dev.laybalt.skyblocktranslator.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * The in-game settings UI (YACL): general toggles, language, online translation
 * backend, and a live string editor over the most recently seen templates —
 * edits are saved as user overrides that beat the dictionary and the MT cache.
 */
public final class ConfigScreens {
	private static final List<String> LANGUAGES = List.of(
			"ru_ru", "uk_ua", "de_de", "fr_fr", "es_es", "pt_br", "pl_pl", "tr_tr", "zh_cn", "ja_jp", "ko_kr");
	private static final List<String> PROVIDERS = List.of("google", "libretranslate");

	private ConfigScreens() {
	}

	public static Screen create(Screen parent) {
		ModConfig config = ModConfig.get();
		Map<String, String> editedOverrides = new LinkedHashMap<>();

		var general = ConfigCategory.createBuilder()
				.name(Component.translatable("sbt.config.category.general"))
				.option(bool("sbt.config.enabled", true, () -> config.enabled, v -> config.enabled = v))
				.option(Option.<String>createBuilder()
						.name(Component.translatable("sbt.config.language"))
						.description(OptionDescription.of(Component.translatable("sbt.config.language.desc")))
						.binding("ru_ru", () -> config.language, v -> config.language = v)
						.controller(opt -> DropdownStringControllerBuilder.create(opt).values(LANGUAGES))
						.build())
				.option(bool("sbt.config.onlyOnHypixel", true, () -> config.onlyOnHypixel, v -> config.onlyOnHypixel = v))
				.group(OptionGroup.createBuilder()
						.name(Component.translatable("sbt.config.group.categories"))
						.option(bool("sbt.config.translateItems", true, () -> config.translateItems, v -> config.translateItems = v))
						.option(bool("sbt.config.translateMenus", true, () -> config.translateMenus, v -> config.translateMenus = v))
						.option(bool("sbt.config.translateDialogs", true, () -> config.translateDialogs, v -> config.translateDialogs = v))
						.option(bool("sbt.config.translatePlayerChat", false, () -> config.translatePlayerChat, v -> config.translatePlayerChat = v))
						.option(bool("sbt.config.translateScoreboard", true, () -> config.translateScoreboard, v -> config.translateScoreboard = v))
						.option(bool("sbt.config.translateTabList", true, () -> config.translateTabList, v -> config.translateTabList = v))
						.option(bool("sbt.config.translateBossBar", true, () -> config.translateBossBar, v -> config.translateBossBar = v))
						.build())
				.build();

		var online = ConfigCategory.createBuilder()
				.name(Component.translatable("sbt.config.category.online"))
				.option(bool("sbt.config.translateOnline", true, () -> config.translateOnline, v -> config.translateOnline = v))
				.option(Option.<String>createBuilder()
						.name(Component.translatable("sbt.config.onlineProvider"))
						.description(OptionDescription.of(Component.translatable("sbt.config.onlineProvider.desc")))
						.binding("google", () -> config.onlineProvider, v -> config.onlineProvider = v)
						.controller(opt -> DropdownStringControllerBuilder.create(opt).values(PROVIDERS))
						.build())
				.option(Option.<Integer>createBuilder()
						.name(Component.translatable("sbt.config.dailyOnlineBudget"))
						.description(OptionDescription.of(Component.translatable("sbt.config.dailyOnlineBudget.desc")))
						.binding(2000, () -> config.dailyOnlineBudget, v -> config.dailyOnlineBudget = v)
						.controller(opt -> IntegerFieldControllerBuilder.create(opt).range(0, 100_000))
						.build())
				.option(Option.<String>createBuilder()
						.name(Component.translatable("sbt.config.libreUrl"))
						.description(OptionDescription.of(Component.translatable("sbt.config.libreUrl.desc")))
						.binding("", () -> config.libreTranslateUrl, v -> config.libreTranslateUrl = v)
						.controller(StringControllerBuilder::create)
						.build())
				.option(Option.<String>createBuilder()
						.name(Component.translatable("sbt.config.libreApiKey"))
						.description(OptionDescription.of(Component.translatable("sbt.config.libreApiKey.desc")))
						.binding("", () -> config.libreTranslateApiKey, v -> config.libreTranslateApiKey = v)
						.controller(StringControllerBuilder::create)
						.build())
				.build();

		var editorBuilder = ConfigCategory.createBuilder()
				.name(Component.translatable("sbt.config.category.editor"));
		List<Map.Entry<String, String>> recent = TranslationEngine.get().recentSeenSnapshot();
		int shown = 0;
		for (Map.Entry<String, String> entry : recent) {
			if (shown++ >= 50) {
				break;
			}
			String template = entry.getKey();
			String current = entry.getValue();
			editorBuilder.option(Option.<String>createBuilder()
					.name(Component.literal(shorten(template)))
					.description(OptionDescription.of(
							Component.translatable("sbt.config.editor.entry.desc", template)))
					.binding(current, () -> editedOverrides.getOrDefault(template, current),
							v -> editedOverrides.put(template, v))
					.controller(StringControllerBuilder::create)
					.build());
		}
		if (shown == 0) {
			editorBuilder.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("sbt.config.editor.empty"))
					.description(OptionDescription.of(Component.translatable("sbt.config.editor.empty.desc")))
					.binding(false, () -> false, v -> {
					})
					.controller(BooleanControllerBuilder::create)
					.build());
		}

		return YetAnotherConfigLib.createBuilder()
				.title(Component.translatable("sbt.config.title"))
				.category(general)
				.category(online)
				.category(editorBuilder.build())
				.save(() -> {
					config.save();
					applyOverrides(editedOverrides);
					TranslationEngine.reload();
				})
				.build()
				.generateScreen(parent);
	}

	private static void applyOverrides(Map<String, String> edited) {
		if (edited.isEmpty()) {
			return;
		}
		var overrides = TranslationEngine.get().overrides();
		edited.forEach(overrides::put);
		overrides.save();
	}

	private static Option<Boolean> bool(String key, boolean defaultValue,
			java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
		return Option.<Boolean>createBuilder()
				.name(Component.translatable(key))
				.description(OptionDescription.of(Component.translatable(key + ".desc")))
				.binding(defaultValue, getter::get, setter::accept)
				.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
				.build();
	}

	private static String shorten(String template) {
		return template.length() <= 40 ? template : template.substring(0, 37) + "...";
	}
}
