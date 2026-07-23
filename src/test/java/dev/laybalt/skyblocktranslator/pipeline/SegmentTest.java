package dev.laybalt.skyblocktranslator.pipeline;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Segment splitting for the §k-aware per-segment translation path. */
class SegmentTest {
	@Test
	void plainStringIsOneSegment() {
		List<LegacyText.Segment> segments = LegacyText.segments("Hello world");
		assertEquals(1, segments.size());
		assertEquals("", segments.get(0).codes());
		assertEquals("Hello world", segments.get(0).text());
		assertFalse(segments.get(0).obfuscated());
	}

	@Test
	void obfuscationIsTrackedAndClearedByColorOrReset() {
		// §d§l§ka§r §d§lLEGENDARY§r §d§l§ka — SkyBlock rarity line shape
		List<LegacyText.Segment> segments = LegacyText.segments("§d§l§ka§r §d§lLEGENDARY§r §d§l§ka");
		assertEquals(5, segments.size());

		assertEquals("§d§l§k", segments.get(0).codes());
		assertEquals("a", segments.get(0).text());
		assertTrue(segments.get(0).obfuscated());

		assertEquals("§r ", segments.get(1).codes() + segments.get(1).text());
		assertFalse(segments.get(1).obfuscated());

		assertEquals("§d§l", segments.get(2).codes());
		assertEquals("LEGENDARY", segments.get(2).text());
		assertFalse(segments.get(2).obfuscated());

		assertEquals("§r ", segments.get(3).codes() + segments.get(3).text());
		assertFalse(segments.get(3).obfuscated());

		assertEquals("§d§l§k", segments.get(4).codes());
		assertEquals("a", segments.get(4).text());
		assertTrue(segments.get(4).obfuscated());
	}

	@Test
	void reassemblingSegmentsReproducesTheOriginal() {
		String original = "§7Health: §a+130§8 (+40) §k!!§r done";
		StringBuilder rebuilt = new StringBuilder();
		for (LegacyText.Segment s : LegacyText.segments(original)) {
			rebuilt.append(s.codes()).append(s.text());
		}
		assertEquals(original, rebuilt.toString());
	}
}
