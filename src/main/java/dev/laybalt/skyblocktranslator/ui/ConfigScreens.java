package dev.laybalt.skyblocktranslator.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * The in-game translation editor (YACL): the most recently seen strings,
 * editable in place. Saved edits become user overrides that beat both the
 * bundled dictionary and the MT cache.
 *
 * <p>The main settings live in the MoulConfig screen ({@code ConfigHolder.openGui()});
 * this screen is reached through its "Translation Editor" button.
 */
public final class ConfigScreens {
	private ConfigScreens() {
	}

	public static Screen createEditor(Screen parent) {
		Map<String, String> editedOverrides = new LinkedHashMap<>();

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
				.category(editorBuilder.build())
				.save(() -> applyOverrides(editedOverrides))
				.build()
				.generateScreen(parent);
	}

	private static void applyOverrides(Map<String, String> edited) {
		if (edited.isEmpty()) {
			return;
		}
		var engine = TranslationEngine.get();
		edited.forEach(engine.overrides()::put);
		engine.overrides().save();
		engine.flushMemo();
	}

	private static String shorten(String template) {
		return template.length() <= 40 ? template : template.substring(0, 37) + "...";
	}
}
