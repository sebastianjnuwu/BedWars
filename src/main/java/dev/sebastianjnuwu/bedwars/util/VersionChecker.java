package dev.sebastianjnuwu.bedwars.util;

import dev.sebastianjnuwu.bedwars.lang.LangManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Logger;

public class VersionChecker {

    private static final String JSON_URL = "https://raw.githubusercontent.com/sebastianjnuwu/BedWars/main/.github/version.json";

    private final String currentVersion;
    private final Logger logger;
    private final LangManager lang;

    public VersionChecker(final String currentVersion, final Logger logger, final LangManager lang) {
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.lang = lang;
    }

    public void checkAsync() {
        this.logger.info(this.lang.raw("version_check.checking"));

        Thread.startVirtualThread(() -> {
            try {
                final HttpURLConnection conn = (HttpURLConnection) URI.create(JSON_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "BedWars");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                final int code = conn.getResponseCode();
                if (code != 200) {
                    this.logger.warning(this.lang.raw("version_check.error", "HTTP " + code));
                    return;
                }

                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    final StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }

                    final String latestVersion = parseVersion(body.toString());
                    if (latestVersion == null) {
                        this.logger.warning(this.lang.raw("version_check.error", "Resposta inválida"));
                        return;
                    }

                    if (this.currentVersion.equals(latestVersion)) {
                        this.logger.info(this.lang.raw("version_check.up_to_date", this.currentVersion));
                    } else {
                        this.logger.warning(this.lang.raw("version_check.new_version", latestVersion, this.currentVersion));
                        this.logger.warning(this.lang.raw("version_check.download", "https://github.com/sebastianjnuwu/BedWars"));
                    }
                }
            } catch (final Exception e) {
                this.logger.warning(this.lang.raw("version_check.error", e.getMessage()));
            }
        });
    }

    private String parseVersion(final String json) {
        final String key = "\"version\"";
        final int start = json.indexOf(key);
        if (start == -1) return null;
        final int colon = json.indexOf(":", start + key.length());
        if (colon == -1) return null;
        final int valueStart = json.indexOf("\"", colon + 1);
        if (valueStart == -1) return null;
        final int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return null;
        return json.substring(valueStart + 1, valueEnd);
    }
}
