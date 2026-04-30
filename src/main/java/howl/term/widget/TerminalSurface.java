package howl.term.widget;

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
    private boolean outputProofLogged;

    public TerminalSurface(UserlandSvc userland) {
        this.gpuSvc = new GpuSvc();
        if (userland == null) {
            throw new IllegalArgumentException("userland runtime required");
        }
        this.termSvc = new TerminalSvc();
        if (!this.termSvc.configurePty(userland.getShell(), null)) {
            android.util.Log.e(TAG, "runtime pty configure failed");
        }
        this.pendingWidth = 0;
        this.pendingHeight = 0;
        this.pendingResize = false;
        this.texture = 0;
        this.termStarted = false;
        this.stopRequested = false;
        this.outputProofLogged = false;
    }

    public android.view.View view(android.app.Activity activity) {
        final android.view.View view = gpuSvc.surface(activity, new GpuSvc.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                texture = gpuSvc.texture();
                termStarted = termSvc.start();
                if (!termStarted) {
                    android.util.Log.e(TAG, "runtime start failed state=" + termSvc.state());
                }
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                gpuSvc.ensureTextureSize(width, height);
                scheduleResize(width, height);
            }

            @Override
            public void onDrawFrame() {
                if (stopRequested && termStarted) {
                    stopSvc();
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
        outputProofLogged = false;
    }
}
