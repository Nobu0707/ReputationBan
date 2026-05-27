package dev.modplugin.reputationban.model;

public enum DiagnosticStatus {
    OK,
    WARN,
    ERROR;

    public static DiagnosticStatus aggregate(DiagnosticStatus... statuses) {
        DiagnosticStatus result = OK;
        for (DiagnosticStatus status : statuses) {
            if (status == ERROR) {
                return ERROR;
            }
            if (status == WARN) {
                result = WARN;
            }
        }
        return result;
    }
}
