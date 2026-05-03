package howl.term;

/** App-resolved shell launch contract for one terminal session. */
public final class ShellLaunch {
    public final String shell;
    public final String command;

    public ShellLaunch(String shell, String command) {
        this.shell = shell;
        this.command = command;
    }
}
