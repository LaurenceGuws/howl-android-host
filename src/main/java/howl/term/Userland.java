package howl.term;

import howl.term.userland.LaunchResolver;
import howl.term.userland.RepairWorkflow;
import howl.term.userland.Runtime;

/** Thin userland owner object. */
public final class Userland {
    public static final class Launch {
        public final ShellLaunch shellLaunch;
        public final boolean userlandReady;

        private Launch(ShellLaunch shellLaunch, boolean userlandReady) {
            this.shellLaunch = shellLaunch;
            this.userlandReady = userlandReady;
        }
    }

    public interface RepairListener {
        void onStarted();
        void onFinished(boolean ok);
    }

    private final Runtime runtime;
    private final LaunchResolver launchResolver;
    private final RepairWorkflow repairWorkflow;

    public Userland(android.content.Context context) {
        this.runtime = new Runtime(context);
        this.launchResolver = new LaunchResolver(runtime);
        this.repairWorkflow = new RepairWorkflow(runtime);
    }

    public void start() {
        runtime.start();
    }

    public Launch resolveLaunch(Config config, long timeoutMs) {
        final LaunchResolver.ResolvedLaunch resolved = launchResolver.resolve(config, timeoutMs);
        return new Launch(resolved.shellLaunch, resolved.userlandReady);
    }

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
