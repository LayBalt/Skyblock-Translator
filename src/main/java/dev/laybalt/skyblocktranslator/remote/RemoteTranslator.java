package dev.laybalt.skyblocktranslator.remote;

import java.io.IOException;

/**
 * A machine-translation backend. Implementations are blocking and are only ever
 * called from the {@link RemoteQueue} worker thread — never from the render thread.
 */
public interface RemoteTranslator {
	/**
	 * @param text       §-stripped template, may contain {0}-style placeholders
	 * @param targetLang two-letter target code ("ru")
	 */
	String translate(String text, String targetLang) throws IOException, InterruptedException;

	String name();
}
