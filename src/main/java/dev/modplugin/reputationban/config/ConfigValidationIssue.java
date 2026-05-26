package dev.modplugin.reputationban.config;

public record ConfigValidationIssue(Severity severity, String path, String message) {
    public enum Severity {
        WARNING,
        ERROR
    }
}
