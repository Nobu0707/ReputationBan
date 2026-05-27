package dev.modplugin.reputationban.util;

import java.util.regex.Pattern;

public final class PathRedactor {
    public static final String PLUGIN_DATA_FOLDER = "<plugin-data-folder>";

    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("(?<![\\w.:-])/(?!/)[^\\s\\r\\n]+");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("(?i)\\b[A-Z]:\\\\[^\\s\\r\\n]+");

    private PathRedactor() {
    }

    public static String pluginDataFolderForSharing() {
        return PLUGIN_DATA_FOLDER;
    }

    public static String redactAbsolutePaths(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String redacted = WINDOWS_ABSOLUTE_PATH.matcher(text).replaceAll(PLUGIN_DATA_FOLDER);
        return UNIX_ABSOLUTE_PATH.matcher(redacted).replaceAll(PLUGIN_DATA_FOLDER);
    }
}
