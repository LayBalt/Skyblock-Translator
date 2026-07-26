package dev.laybalt.skyblocktranslator.config;

import java.io.File;

import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;

import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

/**
 * Owns the {@link ManagedConfig} wrapper around {@link ModConfig}: MoulConfig
 * handles persistence and the editor screen; after every save the translation
 * engine is rebuilt so language/provider changes apply immediately.
 */
public final class ConfigHolder {
	private static ManagedConfig<ModConfig> managed;

	private ConfigHolder() {
	}

	public static synchronized ManagedConfig<ModConfig> managed() {
		if (managed == null) {
			File file = ModConfig.directory().resolve("config.json").toFile();
			file.getParentFile().mkdirs();
			ManagedConfigBuilder<ModConfig> builder = new ManagedConfigBuilder<>(file, ModConfig.class);
			builder.setAfterSave(unused -> TranslationEngine.reload());
			managed = new ManagedConfig<>(builder);
		}
		return managed;
	}

	public static ModConfig get() {
		return managed().getInstance();
	}

	public static void save() {
		managed().saveToFile();
	}

	/** Opens the MoulConfig settings screen. */
	public static void openGui() {
		managed().openConfigGui();
	}
}
