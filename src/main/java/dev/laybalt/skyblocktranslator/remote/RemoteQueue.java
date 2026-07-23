package dev.laybalt.skyblocktranslator.remote;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.laybalt.skyblocktranslator.SkyblockTranslatorClient;

/**
 * Single background worker feeding templates to a {@link RemoteTranslator}.
 *
 * <p>Politeness rules: requests are deduplicated, spaced by a minimum interval,
 * capped by a persistent per-day budget, and errors trigger a cool-down instead
 * of hammering the endpoint. Results are handed back to the engine on the worker
 * thread; the engine takes care of thread safety.
 */
public final class RemoteQueue {
	private static final long MIN_INTERVAL_MS = 350;
	private static final long ERROR_BACKOFF_MS = 60_000;

	private final RemoteTranslator translator;
	private final String targetLang;
	private final int dailyBudget;
	private final Path statsFile;
	private final BiConsumer<String, String> onResult;

	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
	private final Set<String> pending = ConcurrentHashMap.newKeySet();
	private final Thread worker;
	private volatile boolean running = true;

	private DayStats stats;
	private boolean budgetWarned;

	private static final Gson GSON = new GsonBuilder().create();

	private static final class DayStats {
		String date = LocalDate.now().toString();
		int requests;
	}

	public RemoteQueue(RemoteTranslator translator, String targetLang, int dailyBudget,
			Path configDir, BiConsumer<String, String> onResult) {
		this.translator = translator;
		this.targetLang = targetLang;
		this.dailyBudget = dailyBudget;
		this.statsFile = configDir.resolve("remote-stats.json");
		this.onResult = onResult;
		this.stats = loadStats();
		this.worker = new Thread(this::run, "SkyblockTranslator-Remote");
		this.worker.setDaemon(true);
		this.worker.start();
	}

	/** Queues a template unless it is already queued or in flight. */
	public void submit(String templateKey) {
		if (running && pending.add(templateKey)) {
			queue.offer(templateKey);
		}
	}

	public void shutdown() {
		running = false;
		worker.interrupt();
		saveStats();
	}

	private void run() {
		while (running) {
			String key;
			try {
				key = queue.take();
			} catch (InterruptedException e) {
				return;
			}
			if (!budgetAvailable()) {
				pending.remove(key);
				continue;
			}
			try {
				String translated = translator.translate(key, targetLang);
				stats.requests++;
				if (stats.requests % 25 == 0) {
					saveStats();
				}
				if (translated != null && !translated.isBlank()) {
					onResult.accept(key, translated);
				}
				pending.remove(key);
				Thread.sleep(MIN_INTERVAL_MS);
			} catch (InterruptedException e) {
				return;
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.warn("Online translation failed ({}), backing off: {}",
						translator.name(), e.toString());
				pending.remove(key);
				try {
					Thread.sleep(ERROR_BACKOFF_MS);
				} catch (InterruptedException ie) {
					return;
				}
			}
		}
	}

	private boolean budgetAvailable() {
		String today = LocalDate.now().toString();
		if (!today.equals(stats.date)) {
			stats = new DayStats();
			budgetWarned = false;
		}
		if (stats.requests >= dailyBudget) {
			if (!budgetWarned) {
				budgetWarned = true;
				SkyblockTranslatorClient.LOGGER.info(
						"Daily online translation budget ({}) reached; dictionary and cache keep working", dailyBudget);
			}
			return false;
		}
		return true;
	}

	private DayStats loadStats() {
		if (Files.exists(statsFile)) {
			try (Reader reader = Files.newBufferedReader(statsFile, StandardCharsets.UTF_8)) {
				DayStats loaded = GSON.fromJson(reader, DayStats.class);
				if (loaded != null && loaded.date != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				SkyblockTranslatorClient.LOGGER.warn("Could not read {}", statsFile, e);
			}
		}
		return new DayStats();
	}

	private void saveStats() {
		try {
			Files.createDirectories(statsFile.getParent());
			try (Writer writer = Files.newBufferedWriter(statsFile, StandardCharsets.UTF_8)) {
				GSON.toJson(stats, writer);
			}
		} catch (IOException e) {
			SkyblockTranslatorClient.LOGGER.warn("Could not save {}", statsFile, e);
		}
	}
}
