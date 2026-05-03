package howl.term;

/**
 * Responsibility: own the persistent config surface for the Android host.
 * Ownership: stored launch settings and config normalization.
 * Reason: keep config persistence behind one boring host owner.
 */
public final class Config {
    /** Terminal launch and rendering config payload. */
    public static final class Term {
        public final String shell;
        public final String startPath;
        public final String command;
        public final int fontSizeSp;

        /** Construct one terminal config payload. */
        public Term(String shell, String startPath, String command, int fontSizeSp) {
            this.shell = shell;
            this.startPath = startPath;
            this.command = command;
            this.fontSizeSp = fontSizeSp;
        }
    }

    public final Term term;

    /** Construct one top-level config payload. */
    public Config(Term term) {
        this.term = term;
    }

    /** Load the persisted Android-host config. */
    public static Config load(android.content.Context context) {
        final android.content.SharedPreferences prefs =
                context.getSharedPreferences("howl.term.config", android.content.Context.MODE_PRIVATE);
        final String shell = normalize(prefs.getString("term.shell", null));
        final String startPath = normalize(prefs.getString("term.start_path", null));
        final String command = normalize(prefs.getString("term.command", null));
        final int fontSizeSp = Math.max(8, prefs.getInt("term.font_size_sp", 16));
        return new Config(new Term(shell, startPath, command, fontSizeSp));
    }

    static String normalize(String value) {
        if (value == null) return null;
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
