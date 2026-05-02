package howl.term.service;

/** Persistent host config (no runtime instance state). */
public final class Config {
    public static final class Term {
        public final String shell;
        public final String startPath;
        public final String command;
        public final int fontSizeSp;

        public Term(String shell, String startPath, String command, int fontSizeSp) {
            this.shell = shell;
            this.startPath = startPath;
            this.command = command;
            this.fontSizeSp = fontSizeSp;
        }
    }

    public final Term term;

    public Config(Term term) {
        this.term = term;
    }

    public static Config load(android.content.Context context) {
        final android.content.SharedPreferences prefs =
                context.getSharedPreferences("howl.term.config", android.content.Context.MODE_PRIVATE);
        final String shell = normalize(prefs.getString("term.shell", null));
        final String startPath = normalize(prefs.getString("term.start_path", null));
        final String command = normalize(prefs.getString("term.command", null));
        final int fontSizeSp = Math.max(8, prefs.getInt("term.font_size_sp", 16));
        return new Config(new Term(shell, startPath, command, fontSizeSp));
    }

    private static String normalize(String value) {
        if (value == null) return null;
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
