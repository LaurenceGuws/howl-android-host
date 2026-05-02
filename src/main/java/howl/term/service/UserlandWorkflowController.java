package howl.term.service;

/** Userland maintenance workflow lane: install/repair only. */
public final class UserlandWorkflowController {
    public interface Listener {
        void onStarted();
        void onFinished(boolean ok);
    }

    private final Userland userland;
    private final android.os.Handler mainHandler;
    private Thread worker;

    public UserlandWorkflowController(Userland userland) {
        if (userland == null) throw new IllegalArgumentException("userland required");
        this.userland = userland;
        this.mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    public synchronized boolean isRunning() {
        return worker != null && worker.isAlive();
    }

    public synchronized void startRepair(Listener listener) {
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
