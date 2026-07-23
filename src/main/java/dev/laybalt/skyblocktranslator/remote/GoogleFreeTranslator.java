package dev.laybalt.skyblocktranslator.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Free machine translation through Google's public "gtx" endpoint.
 *
 * <p>No API key, but also no SLA — this is exactly the "free tier may hiccup"
 * backend. The queue keeps request rates polite; on HTTP errors the queue backs
 * off. The premium provider will replace this with an LLM service.
 */
public final class GoogleFreeTranslator implements RemoteTranslator {
	private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single";

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	@Override
	public String translate(String text, String targetLang) throws IOException, InterruptedException {
		String url = ENDPOINT + "?client=gtx&sl=en&tl=" + targetLang + "&dt=t&q="
				+ URLEncoder.encode(text, StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "Mozilla/5.0")
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("HTTP " + response.statusCode() + " from translate endpoint");
		}
		// Response shape: [[["перевод","original",...],["...","...",...]],...]
		JsonArray segments = JsonParser.parseString(response.body()).getAsJsonArray().get(0).getAsJsonArray();
		StringBuilder out = new StringBuilder();
		for (JsonElement segment : segments) {
			JsonElement translated = segment.getAsJsonArray().get(0);
			if (!translated.isJsonNull()) {
				out.append(translated.getAsString());
			}
		}
		return out.toString();
	}

	@Override
	public String name() {
		return "google";
	}
}
