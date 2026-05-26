package dev.modplugin.reputationban.util;

import dev.modplugin.reputationban.model.ReportStatus;
import dev.modplugin.reputationban.model.AuditEventType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class CommandSuggestionUtil {
    private static final List<String> REP_NUMBERS = List.of("1", "5", "10", "50", "100");
    private static final List<String> LIMITS = List.of("10", "20", "50");
    private static final List<String> REPORT_STATUSES = withAllStatus();

    private CommandSuggestionUtil() {
    }

    public static List<String> filterByPrefix(Collection<String> candidates, String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .toList();
    }

    public static List<String> repSubcommands(Predicate<String> hasPermission, String prefix) {
        List<String> candidates = new ArrayList<>();
        candidates.add("help");
        if (hasPermission.test("reputationban.score.others") || hasPermission.test("reputationban.admin.score")) {
            candidates.add("check");
            candidates.add("history");
        }
        if (hasPermission.test("reputationban.admin.score")) {
            candidates.add("add");
            candidates.add("remove");
            candidates.add("set");
        }
        if (hasPermission.test("reputationban.admin.ban")) {
            candidates.add("banhistory");
            candidates.add("baninfo");
            candidates.add("unban");
        }
        if (hasPermission.test("reputationban.admin.score") && hasPermission.test("reputationban.admin.ban")) {
            candidates.add("pardon");
        }
        if (hasPermission.test("reputationban.admin")) {
            candidates.add("reload");
        }
        if (hasPermission.test("reputationban.admin.audit")) {
            candidates.add("audit");
        }
        if (hasPermission.test("reputationban.admin.maintenance")) {
            candidates.add("maintenance");
        }
        return filterByPrefix(candidates, prefix);
    }

    public static boolean repSubcommandNeedsPlayer(String subcommand) {
        return switch (normalize(subcommand)) {
            case "check", "history", "add", "remove", "set", "banhistory", "baninfo", "unban", "pardon" -> true;
            default -> false;
        };
    }

    public static List<String> repSecondArgumentSuggestions(String subcommand, Collection<String> playerNames, String prefix) {
        return switch (normalize(subcommand)) {
            case "audit" -> {
                List<String> candidates = new ArrayList<>(List.of("recent", "type", "export"));
                candidates.addAll(playerNames);
                yield filterByPrefix(candidates, prefix);
            }
            case "maintenance" -> filterByPrefix(List.of("run"), prefix);
            default -> repSubcommandNeedsPlayer(subcommand) ? filterByPrefix(playerNames, prefix) : List.of();
        };
    }

    public static List<String> repAuditThirdArgumentSuggestions(String second, Collection<String> playerNames, String prefix) {
        return switch (normalize(second)) {
            case "type" -> filterByPrefix(AuditEventType.databaseValues(), prefix);
            case "export" -> {
                List<String> candidates = new ArrayList<>(List.of("recent"));
                candidates.addAll(playerNames);
                yield filterByPrefix(candidates, prefix);
            }
            default -> filterByPrefix(LIMITS, prefix);
        };
    }

    public static List<String> repAuditFourthArgumentSuggestions(String second, String prefix) {
        return switch (normalize(second)) {
            case "type", "export" -> filterByPrefix(LIMITS, prefix);
            default -> List.of();
        };
    }

    public static List<String> repThirdArgumentSuggestions(String subcommand, String prefix) {
        return switch (normalize(subcommand)) {
            case "add", "remove", "set" -> filterByPrefix(REP_NUMBERS, prefix);
            case "history", "banhistory" -> filterByPrefix(LIMITS, prefix);
            default -> List.of();
        };
    }

    public static List<String> reportStatuses(String prefix) {
        return filterByPrefix(REPORT_STATUSES, prefix);
    }

    public static List<String> reportsSubcommands(boolean hasPermission, String prefix) {
        if (!hasPermission) {
            return List.of();
        }
        return filterByPrefix(List.of("help", "list", "view", "approve", "reject"), prefix);
    }

    public static List<String> reportsSecondArgumentSuggestions(String subcommand, Collection<String> pendingIds, String prefix) {
        return switch (normalize(subcommand)) {
            case "list" -> reportStatuses(prefix);
            case "view", "approve", "reject" -> filterByPrefix(pendingIds, prefix);
            default -> List.of();
        };
    }

    public static List<String> reportsThirdArgumentSuggestions(String subcommand, String prefix) {
        if ("list".equals(normalize(subcommand))) {
            return filterByPrefix(LIMITS, prefix);
        }
        return List.of();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static List<String> withAllStatus() {
        List<String> values = new ArrayList<>(ReportStatus.databaseValues());
        values.add("all");
        return List.copyOf(values);
    }
}
