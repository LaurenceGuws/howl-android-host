package howl.term.widget;

import howl.term.input.ShellInputView;
import howl.term.service.Config;
import howl.term.service.Gpu;
import howl.term.service.ShellLaunch;
import howl.term.service.Terminal;

import java.nio.charset.StandardCharsets;

/** Terminal widget runtime object. */
public final class TermInstance {
    private static final String TAG = "howl.term.runtime";

    private final Gpu gpu;
    private final Gpu.State gpuState;
    private final Terminal term;
    private final Config cfg;
    private final ShellLaunch shellLaunch;
    private final java.util.concurrent.atomic.AtomicBoolean renderQueued;

    private volatile int renderW;
    private volatile int renderH;
    private volatile int gridW;
    private volatile int gridH;
    private volatile android.view.View surfaceView;
    private volatile android.opengl.GLSurfaceView glView;
    private volatile ShellInputView inputView;
    private volatile boolean surfaceReady;
    private volatile boolean running;

    private int texture;
    private Thread wakeThread;
    private long lastFrameLogMs;

    public TermInstance(Config cfg, ShellLaunch shellLaunch) {
        if (cfg == null) throw new IllegalArgumentException("config required");
        if (shellLaunch == null) throw new IllegalArgumentException("shellLaunch required");
        this.gpu = new Gpu();
        this.gpuState = new Gpu.State();
        this.term = new Terminal();
        this.cfg = cfg;
        this.shellLaunch = shellLaunch;
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
        final android.view.View gl = gpu.createSurface(activity, gpuState, new Gpu.Hooks() {
            @Override
            public void onSurfaceCreated() {
                texture = gpu.texture(gpuState);
                gpu.markFrameReady(gpuState, false);
                android.util.Log.i(TAG, "termInst.surfaceCreated tex=" + texture);
                term.configure(shellLaunch.shell, shellLaunch.command);
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
                    android.util.Log.e(TAG, "terminal start failed shell=" + shellLaunch.shell);
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
                if (!term.isAlive()) {
                    android.util.Log.e(TAG, "term session died before draw");
                    stop();
                    return;
                }
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
        });

        final ShellInputView shellInputView = new ShellInputView(activity, new ShellInputView.Host() {
            @Override
            public void sendDirectText(String text) {
                if (text == null || text.isEmpty()) return;
                final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                if (running) {
                    final int rc = term.publishInputBytes(bytes);
                    if (rc >= 0) requestRender(ref[0]);
                }
            }

            @Override
            public void sendDirectCodepoint(int codepoint) {
                sendDirectText(new String(Character.toChars(codepoint)));
            }
        });

        gl.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                focusInput();
                final android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.restartInput(shellInputView);
                    imm.showSoftInput(shellInputView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
            return false;
        });

        final android.widget.FrameLayout container = new android.widget.FrameLayout(activity);
        container.addView(gl, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        final android.widget.FrameLayout.LayoutParams inputLp = new android.widget.FrameLayout.LayoutParams(1, 1);
        inputLp.leftMargin = 0;
        inputLp.topMargin = 0;
        shellInputView.setAlpha(0f);
        container.addView(shellInputView, inputLp);
        this.inputView = shellInputView;

        gl.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
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

        gl.setFocusable(true);
        gl.setFocusableInTouchMode(true);
        focusInput();
        ref[0] = gl;
        this.surfaceView = container;
        this.glView = (gl instanceof android.opengl.GLSurfaceView) ? (android.opengl.GLSurfaceView) gl : null;
        return container;
    }

    public void onPause() {
        final android.view.View view = surfaceView;
        final android.opengl.GLSurfaceView gl = glView;
        if (gl != null) gl.onPause();
    }

    public void onResume() {
        final android.opengl.GLSurfaceView gl = glView;
        if (gl != null) {
            gl.onResume();
            requestRender(gl);
        }
    }

    public android.view.View imeAnchor() {
        return inputView != null ? inputView : surfaceView;
    }

    public ShellInputView shellInputView() {
        return inputView;
    }

    public void focusInput() {
        final ShellInputView input = inputView;
        if (input == null) return;
        input.requestFocusFromTouch();
        if (!input.hasFocus()) input.requestFocus();
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
        glView = null;
        inputView = null;
    }

    private synchronized void startWakeThread() {
        if (wakeThread != null) return;
        wakeThread = new Thread(() -> {
            while (running) {
                if (!term.isAlive()) {
                    android.util.Log.e(TAG, "term session died in wake loop");
                    stop();
                    break;
                }
                final int rc = term.waitRenderWake(16);
                if (!running) break;
                if (rc < 0) break;
                if (rc > 0) {
                    requestRender(glView);
                }
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
