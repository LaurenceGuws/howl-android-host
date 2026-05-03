package howl.term;

/**
 * Responsibility: own the resolved shell-launch payload for one terminal session.
 * Ownership: shell path and login command contract only.
 * Reason: keep launch payloads boring and explicit at the host boundary.
 */
public final class ShellLaunch {
    public final String shell;
    public final String command;

    public ShellLaunch(String shell, String command) {
        this.shell = shell;
        this.command = command;
    }
}
