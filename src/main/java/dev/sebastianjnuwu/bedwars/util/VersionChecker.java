package dev.sebastianjnuwu.bedwars.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.logging.Logger;

import dev.sebastianjnuwu.bedwars.lang.LangManager;

public class VersionChecker {

    private static final String POM_URL = "https://raw.githubusercontent.com/sebastianjnuwu/BedWars/main/pom.xml";

    private final String currentVersion;
    private final Logger logger;
    private final LangManager lang;

    public VersionChecker(final String currentVersion, final Logger logger, final LangManager lang) {
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.lang = lang;
    }

    public void check() {
        this.logger.info(this.lang.raw("version_check.checking"));

        try {
            final HttpURLConnection conn = (HttpURLConnection) URI.create(POM_URL).toURL().openConnection();
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

                final int cmp = compareVersions(normalize(this.currentVersion), normalize(latestVersion));
                if (cmp == 0) {
                    this.logger.info(this.lang.raw("version_check.up_to_date", this.currentVersion));
                } else if (cmp < 0) {
                    this.logger.warning(this.lang.raw("version_check.new_version", latestVersion, this.currentVersion));
                    this.logger.warning(this.lang.raw("version_check.download", "https://github.com/sebastianjnuwu/BedWars"));
                } else {
                    this.logger.info(this.lang.raw("version_check.ahead", this.currentVersion, latestVersion));
                }
            }
        } catch (final Exception e) {
            this.logger.warning(this.lang.raw("version_check.error", e.getMessage()));
        }
    }

    private static String normalize(final String v) {
        String s = v.startsWith("v") ? v.substring(1) : v;
        s = s.replace("-", ".");
        return s;
    }

    static int compareVersions(final String a, final String b) {
        final String[] partsA = a.split("\\.");
        final String[] partsB = b.split("\\.");
        final int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            final int na = i < partsA.length ? parseInt(partsA[i]) : 0;
            final int nb = i < partsB.length ? parseInt(partsB[i]) : 0;
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }
        return 0;
    }

    private static int parseInt(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private String parseVersion(final String xml) {
        final String tag = "<version>";
        final int start = xml.indexOf(tag);
        if (start == -1) {
            return null;
        }
        final int valueStart = start + tag.length();
        final int valueEnd = xml.indexOf("</version>", valueStart);
        if (valueEnd == -1) {
            return null;
        }
        return xml.substring(valueStart, valueEnd);
    }
}
