package howl.term.userland;

import howl.term.Config;
import howl.term.ShellLaunch;
import howl.term.userland.Runtime;

/** Resolves one terminal launch against current userland readiness. */
public final class LaunchResolver {
    private static final String TAG = "howl.term.runtime";

    public static final class ResolvedLaunch {
        public final ShellLaunch shellLaunch;
        public final boolean userlandReady;

        public ResolvedLaunch(ShellLaunch shellLaunch, boolean userlandReady) {
            this.shellLaunch = shellLaunch;
            this.userlandReady = userlandReady;
        }
    }

    private final Runtime userland;

    public LaunchResolver(Runtime userland) {
        if (userland == null) throw new IllegalArgumentException("userland required");
        this.userland = userland;
    }

    public ResolvedLaunch resolve(Config cfg, long timeoutMs) {
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
        return new ResolvedLaunch(new ShellLaunch(shell, command), ready);
    }
}
