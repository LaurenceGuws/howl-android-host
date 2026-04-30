package howl.term.widget;

import howl.term.service.GpuSvc;
import howl.term.service.TerminalSvc;
import howl.term.service.UserlandSvc;

/** Starts GPU runtime */
public final class TerminalSurface {
    private static final String TAG = "howl.term.runtime";
    private final GpuSvc gpuSvc;
    private final TerminalSvc termSvc;
    private volatile int pendingRenderWidth;
    private volatile int pendingRenderHeight;
    private volatile int pendingGridWidth;
    private volatile int pendingGridHeight;
    private volatile boolean pendingResize;
    private volatile android.view.View surfaceView;
    private int texture;
    private boolean termStarted;
    private boolean stopRequested;
    private volatile boolean surfaceReady;
    private volatile boolean wakeThreadRunning;
    private Thread wakeThread;

    public TerminalSurface(UserlandSvc userland) {
        this.gpuSvc = new GpuSvc();
        if (userland == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
        this.termSvc = new TerminalSvc();
        if (!this.termSvc.configurePty(userland.getShell(), null)) {
            android.util.Log.e(TAG, "runtime pty configure failed");
        }
        this.pendingRenderWidth = 0;
        this.pendingRenderHeight = 0;
        this.pendingGridWidth = 0;
        this.pendingGridHeight = 0;
        this.pendingResize = false;
        this.surfaceView = null;
        this.texture = 0;
        this.termStarted = false;
        this.stopRequested = false;
        this.surfaceReady = false;
        this.wakeThreadRunning = false;
        this.wakeThread = null;
    }

    public android.view.View view(android.app.Activity activity) {
        final android.view.View[] viewRef = new android.view.View[1];
        final android.view.View view = gpuSvc.surface(activity, new GpuSvc.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                surfaceReady = false;
                texture = gpuSvc.texture();
                termStarted = termSvc.start();
                if (!termStarted) {
                    android.util.Log.e(TAG, "runtime start failed state=" + termSvc.state());
                } else {
                    startWakeThread();
                }
                if (viewRef[0] != null) gpuSvc.requestRender(viewRef[0]);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                gpuSvc.ensureTextureSize(width, height);
                scheduleRenderResize(width, height);
                surfaceReady = true;
                if (viewRef[0] != null) gpuSvc.requestRender(viewRef[0]);
            }

            @Override
            public void onDrawFrame() {
                if (stopRequested && termStarted) {
                    stopSvc();
                    return;
                }
                if (!termStarted) {
                    return;
                }
                if (!surfaceReady) {
                    return;
                }
                final int renderWidth = Math.max(1, pendingRenderWidth);
                final int renderHeight = Math.max(1, pendingRenderHeight);
                final int gridWidth = Math.max(1, pendingGridWidth > 0 ? pendingGridWidth : pendingRenderWidth);
                final int gridHeight = Math.max(1, pendingGridHeight > 0 ? pendingGridHeight : pendingRenderHeight);
                final boolean hadResize = pendingResize;
                if (hadResize) pendingResize = false;

                final int dirty = termSvc.dirtyState();
                if (dirty == 0 && !hadResize) {
                    return;
                }

                final int rc = termSvc.renderFrameSized(
                        renderWidth,
                        renderHeight,
                        gridWidth,
                        gridHeight,
                        texture
                );
                if (rc < 0) {
                    android.util.Log.e(TAG, "runtime.render rc=" + rc + " state=" + termSvc.state());
                    return;
                }
                if (termSvc.acknowledgePresented() < 0) {
                    android.util.Log.e(TAG, "runtime.acknowledgePresented failed");
                    return;
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
                scheduleGridResize(width, height);
                gpuSvc.requestRender(v);
            }
        });
        viewRef[0] = view;
        surfaceView = view;
        return view;
    }

    private void scheduleRenderResize(int width, int height) {
        pendingRenderWidth = width;
        pendingRenderHeight = height;
        if (pendingGridWidth <= 0 || pendingGridHeight <= 0) {
            pendingGridWidth = width;
            pendingGridHeight = height;
        }
        pendingResize = true;
    }

    private void scheduleGridResize(int width, int height) {
        pendingGridWidth = width;
        pendingGridHeight = height;
        pendingResize = true;
    }

    private synchronized void stopSvc() {
        if (!termStarted) {
            return;
        }
        wakeThreadRunning = false;
        if (wakeThread != null) wakeThread.interrupt();
        termSvc.stop();
        termStarted = false;
        stopRequested = false;
        surfaceReady = false;
        texture = 0;
    }

    private synchronized void startWakeThread() {
        if (wakeThreadRunning) return;
        wakeThreadRunning = true;
        wakeThread = new Thread(() -> {
            while (wakeThreadRunning && termStarted && !stopRequested) {
                final int rc = termSvc.waitForWake(1000);
                if (!wakeThreadRunning || !termStarted || stopRequested) break;
                if (rc > 0) {
                    final android.view.View v = surfaceView;
                    if (v != null) v.post(() -> gpuSvc.requestRender(v));
                }
            }
        }, "howl-term-wake");
        wakeThread.start();
    }
}
