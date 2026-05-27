package dev.modplugin.reputationban.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modplugin.reputationban.util.PathRedactor;
import dev.modplugin.reputationban.util.SupportBundleSafetyChecker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SupportBundleServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void bundleContainsOnlySafeDiagnosticEntries() throws Exception {
        Path zip = tempDir.resolve("support.zip");

        SupportBundleService.writeBundle(zip, new SupportBundleService.SupportBundlePayload(
                "pluginDataFolder=" + PathRedactor.pluginDataFolderForSharing() + "\n",
                "pluginDataFolder=" + PathRedactor.pluginDataFolderForSharing() + "\n"
                        + "discord URL configured=false\n",
                "players=0\nreports=0\nscore_history=0\nbans=0\naudit_events=0\n",
                "notify:\n  discord-webhook:\n    url: \"<redacted>\"\n",
                SupportBundleService.SHARING_README,
                "name: ReputationBan\n",
                "## 0.13.0\n"
        ));

        assertTrue(Files.exists(zip));
        try (ZipFile file = new ZipFile(zip.toFile())) {
            Set<String> names = file.stream()
                    .map(entry -> entry.getName())
                    .collect(java.util.stream.Collectors.toSet());
            assertTrue(names.contains("meta.txt"));
            assertTrue(names.contains("doctor.txt"));
            assertTrue(names.contains("counts.txt"));
            assertTrue(names.contains("config-redacted.yml"));
            assertTrue(names.contains("README-SHARING.txt"));
            assertTrue(names.contains("plugin.yml"));
            assertTrue(names.contains("changelog-excerpt.txt"));
            assertFalse(names.contains("reputationban.db"));
            assertFalse(names.contains("reputationban.db-wal"));
            assertFalse(names.contains("reputationban.db-shm"));
            assertFalse(names.contains("latest.log"));
            assertFalse(names.contains("debug.log"));
            assertFalse(names.stream().anyMatch(name -> name.startsWith("logs/")));

            String allText = readAllText(file);
            assertFalse(SupportBundleSafetyChecker.containsDiscordWebhook(allText));
            assertFalse(SupportBundleSafetyChecker.containsUrl(allText));
            assertFalse(SupportBundleSafetyChecker.containsAbsolutePath(readEntry(file, "meta.txt")));
            assertFalse(SupportBundleSafetyChecker.containsAbsolutePath(readEntry(file, "doctor.txt")));
        }
    }

    private static String readAllText(ZipFile file) throws IOException {
        StringBuilder text = new StringBuilder();
        Enumeration<? extends ZipEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            text.append(readEntry(file, entry.getName())).append('\n');
        }
        return text.toString();
    }

    private static String readEntry(ZipFile file, String name) throws IOException {
        ZipEntry entry = file.getEntry(name);
        try (java.io.InputStream input = file.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
