package howl.term.userland;

import howl.term.userland.Runtime;

/**
 * Responsibility: own repair workflow orchestration for the Android userland unit.
 * Ownership: worker-thread repair execution and main-thread callback delivery.
 * Reason: keep repair flow policy behind one boring userland unit.
 */
public final class RepairWorkflow {
    public interface Listener {
        void onStarted();
        void onFinished(boolean ok);
    }

    private final Runtime userland;
    private final android.os.Handler mainHandler;
    private Thread worker;

    public RepairWorkflow(Runtime userland) {
        if (userland == null) throw new IllegalArgumentException("userland required");
        this.userland = userland;
        this.mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    public synchronized boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    public synchronized void start(Listener listener) {
        if (isRunning()) return;
        if (listener != null) {
            mainHandler.post(listener::onStarted);
        }
        worker = new Thread(() -> {
            final boolean ok = userland.repairAndRecheck();
            if (listener != null) {
                mainHandler.post(() -> listener.onFinished(ok));
            }
        }, "howl-userland-workflow");
        worker.start();
    }
}
