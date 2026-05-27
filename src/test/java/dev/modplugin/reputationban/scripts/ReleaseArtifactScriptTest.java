package dev.modplugin.reputationban.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ReleaseArtifactScriptTest {
    @Test
    void createReleaseArtifactScriptIsValidBash() throws IOException, InterruptedException {
        assertBashSyntax("scripts/create-release-artifact.sh");
        assertBashSyntax("scripts/verify-release-artifact.sh");
        assertBashSyntax("scripts/record-paper-runtime-smoke-result.sh");
    }

    private static void assertBashSyntax(String script) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("bash", "-n", script)
                .redirectErrorStream(true)
                .start();

        assertEquals(0, process.waitFor(), script);
    }
}
