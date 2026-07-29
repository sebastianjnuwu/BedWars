package dev.sebastianjnuwu.bedwars.util;

import dev.sebastianjnuwu.bedwars.lang.LangManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Logger;

public class VersionChecker {

    private static final String API_URL = "https://api.github.com/repos/%s/releases/latest";

    private final String github;
    private final String currentVersion;
    private final Logger logger;
    private final LangManager lang;

    public VersionChecker(final String github, final String currentVersion, final Logger logger, final LangManager lang) {
        this.github = github;
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.lang = lang;
    }

    public void checkAsync() {
        if (this.github == null || this.github.isEmpty()) return;

        this.logger.info(this.lang.raw("version_check.checking"));

        Thread.startVirtualThread(() -> {
            try {
                final URI uri = new URI(String.format(API_URL, this.github));
                final HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "BedWars");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                final int code = conn.getResponseCode();
                if (code != 200) {
                    this.logger.warning(this.lang.raw("version_check.error", "HTTP " + code));
                    return;
                }

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    final StringBuilder json = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }

                    final String latestVersion = parseTagName(json.toString());
                    if (latestVersion == null) {
                        this.logger.warning(this.lang.raw("version_check.error", "Resposta inválida da API"));
                        return;
                    }

                    if (this.currentVersion.equals(latestVersion)) {
                        this.logger.info(this.lang.raw("version_check.up_to_date", this.currentVersion));
                    } else {
                        final String downloadUrl = String.format("https://github.com/%s/releases/latest", this.github);
                        this.logger.warning(this.lang.raw("version_check.new_version", latestVersion, this.currentVersion));
                        this.logger.warning(this.lang.raw("version_check.download", downloadUrl));
                    }
                }
            } catch (final Exception e) {
                this.logger.warning(this.lang.raw("version_check.error", e.getMessage()));
            }
        });
    }

    private String parseTagName(final String json) {
        final String key = "\"tag_name\":\"";
        final int start = json.indexOf(key);
        if (start == -1) return null;
        final int end = json.indexOf("\"", start + key.length());
        if (end == -1) return null;
        return json.substring(start + key.length(), end);
    }
}
