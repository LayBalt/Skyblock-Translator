package dev.laybalt.skyblocktranslator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

public class SkyblockTranslatorClient implements ClientModInitializer {
	public static final String MOD_ID = "skyblock-translator";
	public static final Logger LOGGER = LoggerFactory.getLogger("SkyBlock Translator");

	@Override
	public void onInitializeClient() {
		ModConfig config = ModConfig.get();
		TranslationEngine.init();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TranslationEngine.get().cache().save());
		LOGGER.info("SkyBlock Translator initialized (language={}, enabled={})", config.language, config.enabled);
	}
}
