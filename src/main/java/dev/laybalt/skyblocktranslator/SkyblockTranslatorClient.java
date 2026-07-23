package dev.laybalt.skyblocktranslator;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockTranslatorClient implements ClientModInitializer {
	public static final String MOD_ID = "skyblock-translator";
	public static final Logger LOGGER = LoggerFactory.getLogger("SkyBlock Translator");

	@Override
	public void onInitializeClient() {
		LOGGER.info("SkyBlock Translator initialized");
	}
}
