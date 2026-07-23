package dev.laybalt.skyblocktranslator.remote;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * LibreTranslate backend for self-hosters or users with an instance/API key.
 * Configure the instance URL (and optional key) in the mod settings.
 */
public final class LibreTranslator implements RemoteTranslator {
	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private final String baseUrl;
	private final String apiKey;

	public LibreTranslator(String baseUrl, String apiKey) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.apiKey = apiKey;
	}

	@Override
	public String translate(String text, String targetLang) throws IOException, InterruptedException {
		JsonObject payload = new JsonObject();
		payload.addProperty("q", text);
		payload.addProperty("source", "en");
		payload.addProperty("target", targetLang);
		payload.addProperty("format", "text");
		if (!apiKey.isBlank()) {
			payload.addProperty("api_key", apiKey);
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/translate"))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(15))
				.POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("HTTP " + response.statusCode() + " from LibreTranslate");
		}
		return JsonParser.parseString(response.body()).getAsJsonObject().get("translatedText").getAsString();
	}

	@Override
	public String name() {
		return "libretranslate";
	}
}
