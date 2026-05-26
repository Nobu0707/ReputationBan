package dev.modplugin.reputationban.util;

public final class MaintenancePolicy {
    private MaintenancePolicy() {
    }

    public static MaintenanceAction parse(String[] args) {
        if (args == null || args.length < 2) {
            return MaintenanceAction.HELP;
        }
        if ("preview".equalsIgnoreCase(args[1])) {
            return MaintenanceAction.PREVIEW;
        }
        if (!"run".equalsIgnoreCase(args[1])) {
            return MaintenanceAction.HELP;
        }
        if (args.length >= 3 && "confirm".equalsIgnoreCase(args[2])) {
            return MaintenanceAction.RUN_CONFIRMED;
        }
        return MaintenanceAction.RUN_REQUIRES_CONFIRMATION;
    }

    public enum MaintenanceAction {
        HELP,
        PREVIEW,
        RUN_REQUIRES_CONFIRMATION,
        RUN_CONFIRMED
    }
}
