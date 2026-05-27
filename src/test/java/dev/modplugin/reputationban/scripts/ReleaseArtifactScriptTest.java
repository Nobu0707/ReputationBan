package dev.modplugin.reputationban.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseArtifactScriptTest {
    @Test
    void createReleaseArtifactScriptIsValidBash() throws IOException, InterruptedException {
        assertBashSyntax("scripts/create-release-artifact.sh");
        assertBashSyntax("scripts/verify-release-artifact.sh");
        assertBashSyntax("scripts/record-paper-runtime-smoke-result.sh");
        assertBashSyntax("scripts/check-optional-dependency-safety.sh");
        assertBashSyntax("scripts/record-integration-runtime-smoke-result.sh");
        assertBashSyntax("scripts/check-integration-runtime-readiness.sh");
        assertBashSyntax("scripts/run-integration-runtime-smoke-helper.sh");
    }

    @Test
    void optionalDependencySafetyChecksPlaceholderApiIsolation() throws IOException {
        String script = Files.readString(Path.of("scripts/check-optional-dependency-safety.sh"));

        assertTrue(script.contains("PlaceholderAPI direct import is isolated"));
        assertTrue(script.contains("ReputationBanPlaceholderExpansion.java"));
        assertTrue(script.contains("PlaceholderAPI compileOnly dependency missing"));
        assertTrue(script.contains("reflection-load ReputationBanPlaceholderExpansion"));
    }

    @Test
    void integrationRuntimeReadinessHoldsWhenSummaryIsMissing() throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("reputationban-readiness-missing");

        ProcessResult normal = runScript(workDir, "scripts/check-integration-runtime-readiness.sh");
        ProcessResult strict = runScript(workDir, "scripts/check-integration-runtime-readiness.sh", "--strict");

        assertEquals(0, normal.exitCode());
        assertTrue(normal.output().contains("integration runtime smoke: NOT_RUN"));
        assertTrue(normal.output().contains("HOLD_FOR_INTEGRATION_RUNTIME_SMOKE"));
        assertEquals(1, strict.exitCode());
    }

    @Test
    void integrationRuntimeReadinessAcceptsPassSummaryInStrictMode() throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("reputationban-readiness-pass");
        Path summary = workDir.resolve("build/manual-smoke/integration-runtime-20260101-000000/summary.txt");
        Files.createDirectories(summary.getParent());
        Files.writeString(summary, "result=PASS\nscenario=All integrations\n");

        ProcessResult strict = runScript(workDir, "scripts/check-integration-runtime-readiness.sh", "--strict");

        assertEquals(0, strict.exitCode());
        assertTrue(strict.output().contains("integration runtime smoke: PASS"));
        assertTrue(strict.output().contains("READY_FOR_INTEGRATION_RUNTIME_RELEASE_REVIEW"));
    }

    private static void assertBashSyntax(String script) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("bash", "-n", script)
                .redirectErrorStream(true)
                .start();

        assertEquals(0, process.waitFor(), script);
    }

    private static ProcessResult runScript(Path workDir, String script, String... args) throws IOException, InterruptedException {
        Path scriptPath = Path.of(script).toAbsolutePath();
        List<String> command = new java.util.ArrayList<>();
        command.add("bash");
        command.add(scriptPath.toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        return new ProcessResult(process.waitFor(), output);
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
