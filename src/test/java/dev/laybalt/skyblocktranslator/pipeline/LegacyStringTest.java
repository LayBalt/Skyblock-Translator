package dev.laybalt.skyblocktranslator.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure string-side tests of LegacyText (component conversion is covered in-game). */
class LegacyStringTest {
	@Test
	void stripRemovesAllCodes() {
		assertEquals("Damage: +100", LegacyText.stripCodes("§7Damage: §c+100"));
		assertEquals("plain", LegacyText.stripCodes("plain"));
		assertEquals("BoldRed", LegacyText.stripCodes("§l§4BoldRed"));
	}

	@Test
	void leadingCodesReturnsOnlyThePrefixRun() {
		assertEquals("§7§o", LegacyText.leadingCodes("§7§oGray italic §ctext"));
		assertEquals("", LegacyText.leadingCodes("no codes §7here"));
		assertEquals("§a", LegacyText.leadingCodes("§aGreen"));
	}
}
