package dev.modplugin.reputationban.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigRedactor {
    private static final Pattern YAML_KEY_VALUE = Pattern.compile("^(\\s*)([^:#][^:]*?)(\\s*:\\s*)(.*)$");

    private ConfigRedactor() {
    }

    public static String redactYaml(String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return "";
        }
        String[] lines = yaml.split("\\R", -1);
        StringBuilder redacted = new StringBuilder(yaml.length());
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                redacted.append('\n');
            }
            redacted.append(redactLine(lines[index]));
        }
        return redacted.toString();
    }

    private static String redactLine(String line) {
        if (line.stripLeading().startsWith("#")) {
            return line;
        }
        Matcher matcher = YAML_KEY_VALUE.matcher(line);
        if (!matcher.matches()) {
            return Redactor.redactSecretLikeValue(line);
        }
        String key = matcher.group(2).trim();
        String value = matcher.group(4);
        if (Redactor.isSensitiveKey(key)) {
            if (value.isBlank()) {
                return line;
            }
            return matcher.group(1) + matcher.group(2) + matcher.group(3) + quoteLike(value, Redactor.REDACTED);
        }
        return matcher.group(1) + matcher.group(2) + matcher.group(3) + Redactor.redactSecretLikeValue(value);
    }

    private static String quoteLike(String original, String replacement) {
        String trimmed = original.trim();
        int commentIndex = original.indexOf(" #");
        String comment = commentIndex >= 0 ? original.substring(commentIndex) : "";
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return "\"" + replacement + "\"" + comment;
        }
        return "\"" + replacement + "\"" + comment;
    }
}
