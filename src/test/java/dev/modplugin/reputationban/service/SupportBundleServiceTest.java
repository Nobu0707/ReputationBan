package dev.modplugin.reputationban.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
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
                "meta",
                "doctor",
                "players=0\nreports=0\nscore_history=0\nbans=0\naudit_events=0\n",
                "notify:\n  discord-webhook:\n    url: \"<redacted>\"\n",
                SupportBundleService.SHARING_README,
                "name: ReputationBan\n",
                "## 0.12.0\n"
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
            assertFalse(names.contains("latest.log"));
            assertFalse(names.stream().anyMatch(name -> name.startsWith("logs/")));
        }
    }
}
