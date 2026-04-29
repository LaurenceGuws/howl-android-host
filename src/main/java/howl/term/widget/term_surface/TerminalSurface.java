package howl.term.widget.term_surface;

import howl.term.service.GpuRuntime;
import howl.term.service.TerminalRuntime;
import howl.term.service.UserlandRuntime;

/** Starts GPU runtime */
public final class TerminalSurface {
    private static final String TAG = "howl.term.runtime";
    private final GpuRuntime gpuRt;
    private final TerminalRuntime termRt;
    private volatile int pendingWidth;
    private volatile int pendingHeight;
    private volatile boolean pendingResize;
    private int texture;
    private boolean termStarted;
    private boolean stopRequested;

    public TerminalSurface(UserlandRuntime userland) {
        this.gpuRt = new GpuRuntime();
        if (userland == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
        this.termRt = new TerminalRuntime();
        this.pendingWidth = 0;
        this.pendingHeight = 0;
        this.pendingResize = false;
        this.texture = 0;
        this.termStarted = false;
        this.stopRequested = false;
    }

    public android.view.View view(android.app.Activity activity) {
        final android.view.View view = gpuRt.surface(activity, new GpuRuntime.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                texture = gpuRt.texture();
                termStarted = termRt.start();
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
                    stopRuntime();
                    return;
                }
                if (!termStarted) return;
                if (pendingResize) pendingResize = false;
                final int rc = termRt.renderFrame(
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
                stopRuntime();
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

    private synchronized void stopRuntime() {
        if (!termStarted) {
            return;
        }
        termRt.stop();
        termStarted = false;
        stopRequested = false;
        texture = 0;
    }
}
