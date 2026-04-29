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
    private byte[] frameMask;
    private boolean termStarted;
    private boolean stopRequested;
    private int lastCols;
    private int lastRows;

    public TerminalSurface(UserlandRuntime userland) {
        this.gpuRt = new GpuRuntime();
        this.termRt = new TerminalRuntime(userland);
        this.pendingWidth = 0;
        this.pendingHeight = 0;
        this.pendingResize = false;
        this.frameMask = new byte[0];
        this.termStarted = false;
        this.stopRequested = false;
        this.lastCols = 0;
        this.lastRows = 0;
    }

    public android.view.View view(android.app.Activity activity) {
        final android.view.View view = gpuRt.surface(activity, new GpuRuntime.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
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
                    termRt.stop();
                    termStarted = false;
                    stopRequested = false;
                    lastCols = 0;
                    lastRows = 0;
                    return;
                }
                if (!termStarted) return;
                if (pendingResize) {
                    applyResize(pendingWidth, pendingHeight);
                    pendingResize = false;
                }
                final int rc = termRt.tick();
                if (rc < 0) {
                    android.util.Log.e(TAG, "runtime.tick rc=" + rc);
                    return;
                }
                presentFrameMask();
            }

            @Override
            public void onSurfaceDestroyed() {
                stopRequested = true;
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

    private void applyResize(int width, int height) {
        final int cols = Math.max(1, width / 8);
        final int rows = Math.max(1, height / 16);
        if (termStarted && (cols != lastCols || rows != lastRows)) {
            final int rc = termRt.resize(cols, rows);
            if (rc < 0) {
                android.util.Log.e(TAG, "runtime.resize rc=" + rc);
            }
            lastCols = cols;
            lastRows = rows;
        }
    }

    private void presentFrameMask() {
        final int cols = termRt.frameCols();
        final int rows = termRt.frameRows();
        if (cols <= 0 || rows <= 0) {
            android.util.Log.e(TAG, "invalid frame dimensions cols=" + cols + " rows=" + rows);
            return;
        }
        final int count = cols * rows;
        if (frameMask.length != count) {
            frameMask = new byte[count];
        }
        final int used = termRt.frameMask(frameMask, count);
        if (used <= 0) {
            android.util.Log.e(TAG, "frame mask read failed used=" + used + " expected=" + count);
            return;
        }
        gpuRt.presentMask(cols, rows, frameMask, used);
    }
}
