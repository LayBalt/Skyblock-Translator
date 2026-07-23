package dev.laybalt.skyblocktranslator.detect;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import dev.laybalt.skyblocktranslator.config.ModConfig;

/**
 * Decides whether translation should be active right now.
 *
 * <p>Phase 1 gates on the server address only; SkyBlock-specific detection via
 * the scoreboard title arrives in Phase 2. {@code onlyOnHypixel=false} in the
 * config force-enables translation anywhere (useful for singleplayer testing).
 */
public final class HypixelDetector {
	private HypixelDetector() {
	}

	public static boolean isTranslationActive() {
		ModConfig config = ModConfig.get();
		if (!config.general.enabled) {
			return false;
		}
		if (!config.general.onlyOnHypixel) {
			return true;
		}
		ServerData server = Minecraft.getInstance().getCurrentServer();
		if (server == null || server.ip == null) {
			return false;
		}
		String ip = server.ip.toLowerCase(Locale.ROOT);
		return ip.endsWith("hypixel.net") || ip.endsWith("hypixel.io");
	}
}
