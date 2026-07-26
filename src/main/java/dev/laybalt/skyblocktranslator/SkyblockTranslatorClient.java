package dev.laybalt.skyblocktranslator;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.laybalt.skyblocktranslator.config.ConfigHolder;
import dev.laybalt.skyblocktranslator.config.ModConfig;
import dev.laybalt.skyblocktranslator.pipeline.TranslationEngine;

public class SkyblockTranslatorClient implements ClientModInitializer {
	public static final String MOD_ID = "skyblock-translator";
	public static final Logger LOGGER = LoggerFactory.getLogger("SkyBlock Translator");

	@Override
	public void onInitializeClient() {
		ModConfig config = ModConfig.get();
		TranslationEngine.init();

		KeyMapping openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.skyblock-translator.config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfig.consumeClick()) {
				ConfigHolder.openGui();
			}
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TranslationEngine.get().shutdown());
		LOGGER.info("SkyBlock Translator initialized (language={}, enabled={})",
				config.languageCode(), config.general.enabled);
	}
}
