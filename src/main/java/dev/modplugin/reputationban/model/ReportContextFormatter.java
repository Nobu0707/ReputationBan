package dev.modplugin.reputationban.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReportContextFormatter {
    private ReportContextFormatter() {
    }

    public static List<String> formatEvidence(List<ReportContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of("この通報に保存された連携情報はありません。");
        }
        Map<String, List<ReportContext>> byProvider = new LinkedHashMap<>();
        for (ReportContext context : contexts) {
            byProvider.computeIfAbsent(context.provider(), ignored -> new ArrayList<>()).add(context);
        }

        List<String> lines = new ArrayList<>();
        appendProvider(lines, byProvider, "luckperms");
        appendProvider(lines, byProvider, "coreprotect");
        for (Map.Entry<String, List<ReportContext>> entry : byProvider.entrySet()) {
            if (!"luckperms".equals(entry.getKey()) && !"coreprotect".equals(entry.getKey())) {
                appendUnknown(lines, entry.getKey(), entry.getValue());
            }
        }
        return List.copyOf(lines);
    }

    private static void appendProvider(List<String> lines, Map<String, List<ReportContext>> byProvider, String provider) {
        List<ReportContext> contexts = byProvider.get(provider);
        if (contexts == null || contexts.isEmpty()) {
            return;
        }
        if (!lines.isEmpty()) {
            lines.add("");
        }
        if ("luckperms".equals(provider)) {
            appendLuckPerms(lines, contexts.getLast());
            return;
        }
        appendCoreProtect(lines, contexts);
    }

    private static void appendLuckPerms(List<String> lines, ReportContext context) {
        Map<String, String> metadata = parseFlatJson(context.metadata());
        Map<String, String> summary = parseKeyValueSummary(context.summary());
        lines.add("LuckPerms:");
        lines.add("  primaryGroup=" + value(metadata, summary, "primaryGroup"));
        lines.add("  reporterWeight=" + value(metadata, summary, "reporterWeight"));
        lines.add("  bypassGroup=" + value(metadata, summary, "bypassGroup"));
        lines.add("  applyWeightToDeduction=" + value(metadata, summary, "applyWeightToDeduction"));
    }

    private static void appendCoreProtect(List<String> lines, List<ReportContext> contexts) {
        lines.add("CoreProtect:");
        for (ReportContext context : contexts) {
            for (String line : splitSummary(context.summary())) {
                lines.add("  " + line);
            }
        }
    }

    private static void appendUnknown(List<String> lines, String provider, List<ReportContext> contexts) {
        if (!lines.isEmpty()) {
            lines.add("");
        }
        lines.add(providerLabel(provider) + ":");
        for (ReportContext context : contexts) {
            for (String line : splitSummary(context.summary())) {
                lines.add("  " + line);
            }
        }
    }

    private static String value(Map<String, String> metadata, Map<String, String> summary, String key) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            value = summary.get(key);
        }
        return value == null || value.isBlank() ? "-" : value;
    }

    private static List<String> splitSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return List.of("-");
        }
        return summary.lines().filter(line -> !line.isBlank()).toList();
    }

    private static Map<String, String> parseKeyValueSummary(String summary) {
        Map<String, String> values = new LinkedHashMap<>();
        if (summary == null || summary.isBlank()) {
            return values;
        }
        for (String token : summary.split("\\s+")) {
            int separator = token.indexOf('=');
            if (separator > 0 && separator < token.length() - 1) {
                values.put(token.substring(0, separator), token.substring(separator + 1));
            }
        }
        return values;
    }

    private static Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        int index = 0;
        while (index < json.length()) {
            int keyStart = json.indexOf('"', index);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = nextQuote(json, keyStart + 1);
            int colon = json.indexOf(':', keyEnd + 1);
            int valueStart = json.indexOf('"', colon + 1);
            if (keyEnd < 0 || colon < 0 || valueStart < 0) {
                break;
            }
            int valueEnd = nextQuote(json, valueStart + 1);
            if (valueEnd < 0) {
                break;
            }
            values.put(unescape(json.substring(keyStart + 1, keyEnd)), unescape(json.substring(valueStart + 1, valueEnd)));
            index = valueEnd + 1;
        }
        return values;
    }

    private static int nextQuote(String value, int from) {
        boolean escaped = false;
        for (int index = from; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return index;
            }
        }
        return -1;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String providerLabel(String provider) {
        if (provider == null || provider.isBlank()) {
            return "unknown";
        }
        if ("luckperms".equalsIgnoreCase(provider)) {
            return "LuckPerms";
        }
        if ("coreprotect".equalsIgnoreCase(provider)) {
            return "CoreProtect";
        }
        return provider.toLowerCase(Locale.ROOT);
    }
}
