package howl.term.widget;

import howl.term.service.Config;
import howl.term.service.Gpu;
import howl.term.service.Terminal;
import howl.term.service.Userland;

/** Terminal widget runtime object. */
public final class TermInstance {
    private static final String TAG = "howl.term.runtime";

    private final Gpu gpu;
    private final Gpu.State gpuState;
    private final Terminal term;
    private final Userland userland;
    private final Config cfg;
    private final java.util.concurrent.atomic.AtomicBoolean renderQueued;

    private volatile int renderW;
    private volatile int renderH;
    private volatile int gridW;
    private volatile int gridH;
    private volatile android.view.View surfaceView;
    private volatile boolean surfaceReady;
    private volatile boolean running;

    private int texture;
    private Thread wakeThread;
    private long lastFrameLogMs;

    public TermInstance(Userland userland, Config cfg) {
        if (userland == null) throw new IllegalArgumentException("userland required");
        if (cfg == null) throw new IllegalArgumentException("config required");
        this.gpu = new Gpu();
        this.gpuState = new Gpu.State();
        this.term = new Terminal();
        this.userland = userland;
        this.cfg = cfg;
        this.renderQueued = new java.util.concurrent.atomic.AtomicBoolean(false);
        this.renderW = 1;
        this.renderH = 1;
        this.gridW = 1;
        this.gridH = 1;
        this.surfaceReady = false;
        this.running = false;
        this.texture = 0;
        this.wakeThread = null;
        this.lastFrameLogMs = 0L;
    }

    public android.view.View view(android.app.Activity activity) {
        final int fontPx = Math.max(8, Math.round(cfg.term.fontSizeSp * activity.getResources().getDisplayMetrics().density));
        final int cellW = Math.max(4, fontPx / 2);
        final int cellH = fontPx;
        term.setCellSizePx(cellW, cellH);

        final android.view.View[] ref = new android.view.View[1];
        final android.view.View view = gpu.createSurface(activity, gpuState, new Gpu.Hooks() {
            @Override
            public void onSurfaceCreated() {
                texture = gpu.texture(gpuState);
                gpu.markFrameReady(gpuState, false);
                android.util.Log.i(TAG, "termInst.surfaceCreated tex=" + texture);
                final boolean userlandReady = userland.waitUntilReady(4000);
                String shell = cfg.term.shell != null ? cfg.term.shell : userland.getShell();
                String command = cfg.term.command;
                if (command == null && cfg.term.startPath != null) {
                    command = "cd \"" + cfg.term.startPath + "\" && exec \"" + shell + "\" -i";
                }
                if (!userlandReady || shell == null || !new java.io.File(shell).isFile()) {
                    android.util.Log.e(TAG, "userland not ready; using fallback shell");
                    shell = "/system/bin/sh";
                    command = null;
                }
                term.configure(shell != null ? shell : "/system/bin/sh", command);
                running = term.start();
                if (running) {
                    final String fontPath = pickSystemMonoFont();
                    if (fontPath != null) {
                        final int fontRc = term.setPrimaryFontPath(fontPath);
                        if (fontRc < 0) {
                            android.util.Log.e(TAG, "setPrimaryFontPath failed rc=" + fontRc + " path=" + fontPath);
                        }
                    }
                    final String[] fallbacks = pickSystemFallbackFonts();
                    final int fallbackRc = term.setFallbackFontPaths(fallbacks);
                    if (fallbackRc < 0) {
                        android.util.Log.e(TAG, "setFallbackFontPaths failed rc=" + fallbackRc);
                    }
                    startWakeThread();
                } else {
                    android.util.Log.e(TAG, "terminal start failed shell=" + shell);
                }
                requestRender(ref[0]);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                renderW = Math.max(1, width);
                renderH = Math.max(1, height);
                gridW = renderW;
                gridH = renderH;
                gpu.ensureTextureSize(gpuState, renderW, renderH);
                gpu.markFrameReady(gpuState, false);
                surfaceReady = true;
                android.util.Log.i(TAG, "termInst.surfaceChanged render=" + renderW + "x" + renderH + " grid=" + gridW + "x" + gridH + " tex=" + texture);
                requestRender(ref[0]);
            }

            @Override
            public void onDrawFrame() {
                renderQueued.set(false);
                if (!running || !surfaceReady) return;
                final int rc = term.renderFrameSized(renderW, renderH, gridW, gridH, texture);
                if (rc < 0) {
                    gpu.markFrameReady(gpuState, false);
                    android.util.Log.e(TAG, "renderFrameSized failed rc=" + rc + " state=" + term.state());
                    return;
                }
                final int ack = term.presentAck();
                if (ack < 0) {
                    gpu.markFrameReady(gpuState, false);
                    android.util.Log.e(TAG, "presentAck failed rc=" + ack);
                    return;
                }
                gpu.markFrameReady(gpuState, true);
                final long now = android.os.SystemClock.uptimeMillis();
                if (now - lastFrameLogMs > 1000) {
                    lastFrameLogMs = now;
                    android.util.Log.i(TAG, "termInst.frame ok rw=" + renderW + " rh=" + renderH + " tex=" + texture);
                }
            }

            @Override
            public void onSurfaceDestroyed() {
                stop();
            }

            @Override
            public void onInputBytes(byte[] bytes) {
                if (!running || bytes == null || bytes.length == 0) return;
                if (term.publishInputBytes(bytes) >= 0) requestRender(ref[0]);
            }
        });

        view.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            final int w = r - l;
            final int h = b - t;
            if (w == (orr - ol) && h == (ob - ot)) return;
            renderW = Math.max(1, w);
            renderH = Math.max(1, h);
            gridW = renderW;
            gridH = renderH;
            gpu.resizeTexture(gpuState, v, renderW, renderH);
            requestRender(v);
        });

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        ref[0] = view;
        this.surfaceView = view;
        return view;
    }

    public void onPause() {
        final android.view.View view = surfaceView;
        if (view instanceof android.opengl.GLSurfaceView gl) gl.onPause();
    }

    public void onResume() {
        final android.view.View view = surfaceView;
        if (view instanceof android.opengl.GLSurfaceView gl) {
            gl.onResume();
            requestRender(gl);
        }
    }

    private synchronized void stop() {
        running = false;
        surfaceReady = false;
        final Thread t = wakeThread;
        wakeThread = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        term.stop();
    }

    private synchronized void startWakeThread() {
        if (wakeThread != null) return;
        wakeThread = new Thread(() -> {
            while (running) {
                final int rc = term.waitRenderWake(16);
                if (!running) break;
                if (rc < 0) break;
                if (rc > 0) requestRender(surfaceView);
            }
        }, "howl-term-wake");
        wakeThread.start();
    }

    private void requestRender(android.view.View view) {
        if (view == null) return;
        if (!renderQueued.compareAndSet(false, true)) return;
        view.post(() -> gpu.requestRender(view));
    }

    private static String pickSystemMonoFont() {
        final String[] candidates = new String[] {
                "/system/fonts/NotoSansMono-Regular.ttf",
                "/system/fonts/RobotoMono-Regular.ttf",
                "/system/fonts/DroidSansMono.ttf",
                "/system/fonts/JetBrainsMono-Regular.ttf"
        };
        for (String path : candidates) {
            if (new java.io.File(path).isFile()) return path;
        }
        return null;
    }

    private static String[] pickSystemFallbackFonts() {
        final String[] candidates = new String[] {
                "/system/fonts/NotoSansSymbols-Regular-Subsetted.ttf",
                "/system/fonts/NotoSansSymbols2-Regular-Subsetted.ttf",
                "/system/fonts/NotoColorEmoji.ttf",
                "/system/fonts/SamsungColorEmoji.ttf",
                "/system/fonts/NotoSans-Regular.ttf"
        };
        final java.util.ArrayList<String> paths = new java.util.ArrayList<>();
        for (String path : candidates) {
            if (new java.io.File(path).isFile()) paths.add(path);
        }
        return paths.toArray(new String[0]);
    }
}
