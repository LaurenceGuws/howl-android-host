package howl.term.widget;

import howl.term.service.GpuSvc;
import howl.term.service.TerminalSvc;
import howl.term.service.UserlandSvc;

/** Starts GPU runtime */
public final class TerminalSurface {
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
    private final java.util.concurrent.atomic.AtomicBoolean renderQueued;

    public TerminalSurface(UserlandSvc userland) {
        this.gpuSvc = new GpuSvc();
        if (userland == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
        this.termSvc = new TerminalSvc();
        if (!this.termSvc.configurePty(userland.getShell(), null)) {
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
        this.renderQueued = new java.util.concurrent.atomic.AtomicBoolean(false);
    }

    public android.view.View view(android.app.Activity activity) {
        final android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        final int cellWidthPx = Math.max(12, Math.round(8.0f * dm.density));
        final int cellHeightPx = Math.max(24, Math.round(16.0f * dm.density));
        termSvc.configureCellSizePx(cellWidthPx, cellHeightPx);
        final android.view.View[] viewRef = new android.view.View[1];
        final android.view.View view = gpuSvc.surface(activity, new GpuSvc.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                surfaceReady = false;
                texture = gpuSvc.texture();
                termStarted = termSvc.start();
                if (!termStarted) {
                } else {
                    startWakeThread();
                }
                if (viewRef[0] != null) viewRef[0].requestFocus();
                requestRenderCoalesced(viewRef[0]);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                gpuSvc.ensureTextureSize(width, height);
                scheduleRenderResize(width, height);
                surfaceReady = true;
                requestRenderCoalesced(viewRef[0]);
            }

            @Override
            public void onDrawFrame() {
                renderQueued.set(false);
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

                final int rc = termSvc.renderFrameSized(
                        renderWidth,
                        renderHeight,
                        gridWidth,
                        gridHeight,
                        texture
                );
                if (rc < 0) {
                    return;
                }
                final int ackRc = termSvc.presentAck();
                if (ackRc < 0) {
                    return;
                }
            }

            @Override
            public void onSurfaceDestroyed() {
                stopRequested = true;
                stopSvc();
            }

            @Override
            public void onInputBytes(byte[] bytes) {
                if (bytes == null || bytes.length == 0 || !termStarted) return;
                final int rc = termSvc.publishInputBytes(bytes);
                if (rc < 0) {
                    return;
                }
                requestRenderCoalesced(viewRef[0]);
            }
        });
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setClickable(true);
        view.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            byte[] bytes = null;
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) bytes = new byte[] { '\r' };
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL) bytes = new byte[] { 0x7f };
            if (keyCode == android.view.KeyEvent.KEYCODE_TAB) bytes = new byte[] { '\t' };
            if (bytes == null) {
                final int codepoint = event.getUnicodeChar();
                if (codepoint > 0 && !Character.isISOControl(codepoint)) {
                    final String s = new String(Character.toChars(codepoint));
                    bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            if (bytes == null || bytes.length == 0) return false;
            final int rc = termSvc.publishInputBytes(bytes);
            if (rc < 0) {
                return false;
            }
            requestRenderCoalesced(v);
            return true;
        });
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            final int width = right - left;
            final int height = bottom - top;
            if (width != oldRight - oldLeft || height != oldBottom - oldTop) {
                scheduleRenderResize(width, height);
                scheduleGridResize(width, height);
                gpuSvc.resizeTexture(v, width, height);
                requestRenderCoalesced(v);
            }
        });
        viewRef[0] = view;
        surfaceView = view;
        view.requestFocus();
        return view;
    }

    public void onPause() {
        final android.view.View v = surfaceView;
        if (v instanceof android.opengl.GLSurfaceView glView) {
            glView.onPause();
        }
    }

    public void onResume() {
        final android.view.View v = surfaceView;
        if (v instanceof android.opengl.GLSurfaceView glView) {
            glView.onResume();
            requestRenderCoalesced(glView);
        }
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
        final Thread t = wakeThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(200);
            } catch (InterruptedException ignored) {}
        }
        wakeThread = null;
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
                final int rc = termSvc.waitRenderWake(16);
                if (!wakeThreadRunning || !termStarted || stopRequested) break;
                if (rc < 0) break;
                if (rc > 0) {
                    requestRenderCoalesced(surfaceView);
                }
            }
        }, "howl-term-wake");
        wakeThread.start();
    }

    private void requestRenderCoalesced(android.view.View v) {
        if (v == null) return;
        if (!renderQueued.compareAndSet(false, true)) return;
        v.post(() -> gpuSvc.requestRender(v));
    }
}
