package howl.term.service;

/** App-level userland orchestration: readiness + shell launch resolution. */
public final class UserlandManager {
    private static final String TAG = "howl.term.runtime";

    public static final class LaunchPlan {
        public final ShellLaunch shellLaunch;
        public final boolean userlandReady;

        public LaunchPlan(ShellLaunch shellLaunch, boolean userlandReady) {
            this.shellLaunch = shellLaunch;
            this.userlandReady = userlandReady;
        }
    }

    private final Userland userland;

    public UserlandManager(Userland userland) {
        if (userland == null) throw new IllegalArgumentException("userland required");
        this.userland = userland;
    }

    public void start() {
        userland.start();
    }

    public LaunchPlan resolveLaunch(Config cfg, long timeoutMs) {
        final boolean ready = userland.waitUntilReady(timeoutMs);
        String shell = cfg.term.shell != null ? cfg.term.shell : userland.getShell();
        String startPath = cfg.term.startPath != null ? cfg.term.startPath : userland.getHome();
        String command = null;
        final boolean explicitCommand = cfg.term.command != null && !cfg.term.command.trim().isEmpty();
        final boolean explicitStartPath = cfg.term.startPath != null
                && !cfg.term.startPath.trim().isEmpty()
                && !cfg.term.startPath.trim().equals(userland.getHome());
        if (explicitCommand || explicitStartPath) {
            command = userland.buildLoginCommand(shell, startPath, cfg.term.command);
        }

        final boolean launchable = ready && shell != null && new java.io.File(shell).isFile();
        if (!launchable) command = null;

        android.util.Log.i(
                TAG,
                "userland.launchPlan ready=" + ready +
                        " launchable=" + launchable +
                        " shell=" + shell +
                        " command=" + (command != null ? "set" : "null"));
        return new LaunchPlan(new ShellLaunch(shell, command), ready);
    }
}
