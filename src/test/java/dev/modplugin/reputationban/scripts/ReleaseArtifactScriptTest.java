package dev.modplugin.reputationban.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ReleaseArtifactScriptTest {
    @Test
    void createReleaseArtifactScriptIsValidBash() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("bash", "-n", "scripts/create-release-artifact.sh")
                .redirectErrorStream(true)
                .start();

        assertEquals(0, process.waitFor());
    }
}
