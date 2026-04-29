package howl.term.widget.term_surface;

import howl.term.service.GpuSvc;
import howl.term.service.TerminalSvc;
import howl.term.service.UserlandSvc;

/** Starts GPU runtime */
public final class TerminalSurface {
    private static final String TAG = "howl.term.runtime";
    private final GpuSvc gpuSvc;
    private final TerminalSvc termSvc;
    private volatile int pendingWidth;
    private volatile int pendingHeight;
    private volatile boolean pendingResize;
    private int texture;
    private boolean termStarted;
    private boolean stopRequested;

    public TerminalSurface(UserlandSvc userland) {
        this.gpuSvc = new GpuSvc();
        if (userland == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
        this.termSvc = new TerminalSvc();
        this.pendingWidth = 0;
        this.pendingHeight = 0;
        this.pendingResize = false;
        this.texture = 0;
        this.termStarted = false;
        this.stopRequested = false;
    }

    public android.view.View view(android.app.Activity activity) {
        final android.view.View view = gpuSvc.surface(activity, new GpuSvc.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                texture = gpuSvc.texture();
                termStarted = termSvc.start();
                if (!termStarted) {
                    android.util.Log.e(TAG, "runtime start failed");
                }
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                scheduleResize(width, height);
            }

            @Override
            public void onDrawFrame() {
                if (stopRequested && termStarted) {
                    stopSvc();
                    return;
                }
                if (!termStarted) return;
                if (pendingResize) pendingResize = false;
                final int rc = termSvc.renderFrame(
                        Math.max(1, pendingWidth),
                        Math.max(1, pendingHeight),
                        texture
                );
                if (rc < 0) {
                    android.util.Log.e(TAG, "runtime.render rc=" + rc);
                }
            }

            @Override
            public void onSurfaceDestroyed() {
                stopRequested = true;
                stopSvc();
            }
        });
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            final int width = right - left;
            final int height = bottom - top;
            if (width != oldRight - oldLeft || height != oldBottom - oldTop) {
                scheduleResize(width, height);
            }
        });
        return view;
    }

    private void scheduleResize(int width, int height) {
        pendingWidth = width;
        pendingHeight = height;
        pendingResize = true;
    }

    private synchronized void stopSvc() {
        if (!termStarted) {
            return;
        }
        termSvc.stop();
        termStarted = false;
        stopRequested = false;
        texture = 0;
    }
}
