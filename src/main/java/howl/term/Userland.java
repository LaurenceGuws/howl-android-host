package howl.term;

import howl.term.userland.LaunchResolver;
import howl.term.userland.RepairWorkflow;
import howl.term.userland.Runtime;

/**
 * Responsibility: own the public userland surface for the Android host.
 * Ownership: runtime startup, launch resolution, and repair orchestration.
 * Reason: keep userland leaves behind one boring host owner.
 */
public final class Userland {
    /** Resolved launch payload for one terminal start attempt. */
    public static final class Launch {
        public final ShellLaunch shellLaunch;
        public final boolean userlandReady;

        private Launch(ShellLaunch shellLaunch, boolean userlandReady) {
            this.shellLaunch = shellLaunch;
            this.userlandReady = userlandReady;
        }
    }

    /** Host callback contract for repair progress. */
    public interface RepairListener {
        void onStarted();
        void onFinished(boolean ok);
    }

    private final Runtime runtime;
    private final LaunchResolver launchResolver;
    private final RepairWorkflow repairWorkflow;

    /** Construct one userland owner around the Android runtime context. */
    public Userland(android.content.Context context) {
        this.runtime = new Runtime(context);
        this.launchResolver = new LaunchResolver(runtime);
        this.repairWorkflow = new RepairWorkflow(runtime);
    }

    /** Start the userland runtime lane. */
    public void start() {
        runtime.start();
    }

    /** Resolve one terminal launch against current userland readiness. */
    public Launch resolveLaunch(Config config, long timeoutMs) {
        final LaunchResolver.ResolvedLaunch resolved = launchResolver.resolve(config, timeoutMs);
        return new Launch(resolved.shellLaunch, resolved.userlandReady);
    }

    /** Start the repair lane and forward progress callbacks to the host. */
    public void startRepair(RepairListener listener) {
        repairWorkflow.start(listener == null ? null : new RepairWorkflow.Listener() {
            @Override
            public void onStarted() {
                listener.onStarted();
            }

            @Override
            public void onFinished(boolean ok) {
                listener.onFinished(ok);
            }
        });
    }
}
