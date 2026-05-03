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
    private final java.util.concurrent.atomic.AtomicBoolean renderPending;

    private volatile int renderW;
    private volatile int renderH;
    private volatile int gridW;
    private volatile int gridH;
    private volatile android.view.View surfaceView;
    private volatile android.opengl.GLSurfaceView glView;
    private volatile ShellInputView inputView;
    private volatile boolean surfaceReady;
    private volatile boolean running;
    private volatile android.widget.FrameLayout overlayLayer;
    private volatile android.view.View scrollTrackView;
    private volatile android.view.View scrollThumbView;
    private volatile android.widget.TextView liveChipView;

    private int texture;
    private Thread wakeThread;
    private long lastFrameLogMs;
    private long lastWakeLogMs;
    private final java.util.concurrent.atomic.AtomicLong lastInputMs;
    private android.widget.OverScroller scrollFlingScroller;
    private boolean scrollFlingScheduled;
    private int scrollFlingLastY;
    private float scrollRemainderRows;
    private int cellHeightPx;
    private int lastOverlayCount;
    private int lastOverlayOffset;
    private long lastOverlayRefreshMs;

    public TermInstance(Config cfg, ShellLaunch shellLaunch) {
        if (cfg == null) throw new IllegalArgumentException("config required");
        if (shellLaunch == null) throw new IllegalArgumentException("shellLaunch required");
        this.gpu = new Gpu();
        this.gpuState = new Gpu.State();
        this.term = new Terminal();
        this.cfg = cfg;
        this.shellLaunch = shellLaunch;
        this.renderQueued = new java.util.concurrent.atomic.AtomicBoolean(false);
        this.renderPending = new java.util.concurrent.atomic.AtomicBoolean(false);
        this.renderW = 1;
        this.renderH = 1;
        this.gridW = 1;
        this.gridH = 1;
        this.surfaceReady = false;
        this.running = false;
        this.overlayLayer = null;
        this.scrollTrackView = null;
        this.scrollThumbView = null;
        this.liveChipView = null;
        this.texture = 0;
        this.wakeThread = null;
        this.lastFrameLogMs = 0L;
        this.lastWakeLogMs = 0L;
        this.lastInputMs = new java.util.concurrent.atomic.AtomicLong(0L);
        this.scrollFlingScroller = null;
        this.scrollFlingScheduled = false;
        this.scrollFlingLastY = 0;
        this.scrollRemainderRows = 0f;
        this.cellHeightPx = 24;
        this.lastOverlayCount = -1;
        this.lastOverlayOffset = -1;
        this.lastOverlayRefreshMs = 0L;
    }

    public android.view.View view(android.app.Activity activity) {
        final int fontPx = Math.max(8, Math.round(cfg.term.fontSizeSp * activity.getResources().getDisplayMetrics().density));
        final int cellW = Math.max(4, fontPx / 2);
        final int cellH = fontPx;
        this.cellHeightPx = cellH;
        this.scrollFlingScroller = new android.widget.OverScroller(activity);
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
                applyViewportSize(width, height);
                gpu.ensureTextureSize(gpuState, renderW, renderH);
                gpu.markFrameReady(gpuState, false);
                surfaceReady = true;
                android.util.Log.i(TAG, "termInst.surfaceChanged render=" + renderW + "x" + renderH + " grid=" + gridW + "x" + gridH + " tex=" + texture);
                requestRender(ref[0]);
            }

            @Override
            public void onDrawFrame() {
                renderQueued.set(false);
                final boolean needsFollowup = renderPending.getAndSet(false);
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
                final long inputAt = lastInputMs.get();
                if (now - lastFrameLogMs > 1000) {
                    lastFrameLogMs = now;
                    final long inputToFrameMs = (inputAt > 0L && inputAt <= now) ? (now - inputAt) : -1L;
                    android.util.Log.i(TAG, "termInst.frame ok rw=" + renderW + " rh=" + renderH + " tex=" + texture + " inputToFrameMs=" + inputToFrameMs + " queued=" + renderQueued.get());
                }
                if (now - lastOverlayRefreshMs > 250) {
                    lastOverlayRefreshMs = now;
                    refreshScrollOverlay();
                }
                if (needsFollowup) {
                    requestRender(glView);
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
                    lastInputMs.set(android.os.SystemClock.uptimeMillis());
                    final int rc = term.publishInputBytes(bytes);
                    if (rc >= 0) requestRender(ref[0]);
                }
            }

            @Override
            public void sendDirectCodepoint(int codepoint) {
                sendDirectText(new String(Character.toChars(codepoint)));
            }
        });

        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(activity,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(android.view.MotionEvent e) {
                        stopScrollFling();
                        scrollRemainderRows = 0f;
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(android.view.MotionEvent e) {
                        focusInput();
                        final android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.restartInput(shellInputView);
                            imm.showSoftInput(shellInputView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        }
                        return true;
                    }

                    @Override
                    public boolean onScroll(android.view.MotionEvent e1, android.view.MotionEvent e2, float distanceX, float distanceY) {
                        applyScrollDelta(distanceY);
                        return true;
                    }

                    @Override
                    public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                        if (!running) return false;
                        if (cellHeightPx <= 0) return false;
                        final android.widget.OverScroller scroller = scrollFlingScroller;
                        if (scroller == null) return false;
                        stopScrollFling();
                        scrollFlingLastY = 0;
                        scroller.fling(0, 0, 0, Math.round(velocityY), 0, 0, Integer.MIN_VALUE / 4, Integer.MAX_VALUE / 4);
                        scheduleScrollFlingFrame();
                        return true;
                    }
                });
        gl.setOnTouchListener((v, e) -> gestureDetector.onTouchEvent(e));

        final android.widget.FrameLayout container = new android.widget.FrameLayout(activity);
        container.addView(gl, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        final android.widget.FrameLayout overlay = new android.widget.FrameLayout(activity);
        container.addView(overlay, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        final float density = activity.getResources().getDisplayMetrics().density;
        final android.view.View track = new android.view.View(activity);
        track.setBackgroundColor(0x336B7280);
        final android.widget.FrameLayout.LayoutParams trackLp = new android.widget.FrameLayout.LayoutParams(
                Math.round(4 * density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        trackLp.gravity = android.view.Gravity.END;
        trackLp.topMargin = Math.round(20 * density);
        trackLp.bottomMargin = Math.round(20 * density);
        trackLp.rightMargin = Math.round(4 * density);
        overlay.addView(track, trackLp);

        final android.view.View thumb = new android.view.View(activity);
        thumb.setBackgroundColor(0xCCCBD5E1);
        final android.widget.FrameLayout.LayoutParams thumbLp = new android.widget.FrameLayout.LayoutParams(
                Math.round(4 * density),
                Math.round(48 * density)
        );
        thumbLp.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        thumbLp.topMargin = Math.round(20 * density);
        thumbLp.rightMargin = Math.round(4 * density);
        overlay.addView(thumb, thumbLp);

        final android.widget.TextView liveChip = new android.widget.TextView(activity);
        liveChip.setText("LIVE");
        liveChip.setTextColor(0xFFE5E7EB);
        liveChip.setTextSize(12f);
        liveChip.setPadding(Math.round(10 * density), Math.round(6 * density), Math.round(10 * density), Math.round(6 * density));
        liveChip.setBackgroundColor(0xCC111827);
        liveChip.setOnClickListener(v -> {
            if (!running) return;
            final int rc = term.followLiveBottom();
            if (rc >= 0) {
                requestRender(glView);
                refreshScrollOverlay();
            }
        });
        final android.widget.FrameLayout.LayoutParams liveLp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        liveLp.gravity = android.view.Gravity.END | android.view.Gravity.BOTTOM;
        liveLp.rightMargin = Math.round(14 * density);
        liveLp.bottomMargin = Math.round(14 * density);
        overlay.addView(liveChip, liveLp);

        this.overlayLayer = overlay;
        this.scrollTrackView = track;
        this.scrollThumbView = thumb;
        this.liveChipView = liveChip;
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
            applyViewportSize(w, h);
            gpu.resizeTexture(gpuState, v, renderW, renderH);
            requestRender(v);
        });

        gl.setFocusable(true);
        gl.setFocusableInTouchMode(true);
        focusInput();
        ref[0] = gl;
        this.surfaceView = container;
        this.glView = (gl instanceof android.opengl.GLSurfaceView) ? (android.opengl.GLSurfaceView) gl : null;
        refreshScrollOverlay();
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
            if (Thread.currentThread() != t) {
                try {
                    t.join(200);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        term.stop();
        glView = null;
        inputView = null;
        overlayLayer = null;
        scrollTrackView = null;
        scrollThumbView = null;
        liveChipView = null;
        stopScrollFling();
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
                    final long now = android.os.SystemClock.uptimeMillis();
                    if (now - lastWakeLogMs > 1000) {
                        lastWakeLogMs = now;
                        android.util.Log.i(TAG, "termInst.wake rc=" + rc + " queued=" + renderQueued.get());
                    }
                    requestRender(glView);
                }
            }
        }, "howl-term-wake");
        wakeThread.start();
    }

    private void requestRender(android.view.View view) {
        if (view == null) return;
        if (!renderQueued.compareAndSet(false, true)) {
            renderPending.set(true);
            return;
        }
        view.post(() -> gpu.requestRender(view));
    }

    private void applyViewportSize(int width, int height) {
        final int nextRenderW = Math.max(1, width);
        final int nextRenderH = Math.max(1, height);
        final boolean widthChanged = gridW != nextRenderW;

        renderW = nextRenderW;
        renderH = nextRenderH;

        if (widthChanged || gridW <= 1 || gridH <= 1) {
            gridW = nextRenderW;
            gridH = nextRenderH;
            return;
        }

        // IME visibility commonly shrinks height without changing width. Keep the
        // terminal grid stable in that case so the keyboard does not trigger a PTY resize.
        if (nextRenderH > gridH) {
            gridH = nextRenderH;
        }
    }

    private void applyScrollDelta(float deltaY) {
        if (!running) return;
        final int h = Math.max(1, renderH);
        final int rowPx = Math.max(1, cellHeightPx);
        final int visibleRows = Math.max(1, h / rowPx);
        final float exactRowPx = Math.max(1f, (float) h / (float) visibleRows);
        scrollRemainderRows += (deltaY / exactRowPx);
        final int wholeRows = (int) scrollRemainderRows;
        if (wholeRows == 0) return;
        scrollRemainderRows -= wholeRows;

        final int historyCount = term.currentScrollbackCount();
        final int currentOffset = term.currentScrollbackOffset();
        final int nextOffset = Math.max(0, Math.min(currentOffset + wholeRows, historyCount));
        if (nextOffset == currentOffset) return;
        if (nextOffset == 0) {
            term.followLiveBottom();
        } else {
            term.setScrollbackOffset(nextOffset);
        }
        requestRender(glView);
        refreshScrollOverlay();
    }

    private void refreshScrollOverlay() {
        final android.widget.FrameLayout overlay = overlayLayer;
        final android.view.View track = scrollTrackView;
        final android.view.View thumb = scrollThumbView;
        final android.widget.TextView liveChip = liveChipView;
        if (overlay == null || track == null || thumb == null || liveChip == null) return;
        overlay.post(() -> {
            final int historyCount = term.currentScrollbackCount();
            final int offset = term.currentScrollbackOffset();
            if (historyCount == lastOverlayCount && offset == lastOverlayOffset && thumb.getHeight() > 0) return;
            lastOverlayCount = historyCount;
            lastOverlayOffset = offset;

            final boolean hasHistory = historyCount > 0;
            final boolean scrolled = offset > 0;
            track.setVisibility(hasHistory ? android.view.View.VISIBLE : android.view.View.GONE);
            thumb.setVisibility(hasHistory ? android.view.View.VISIBLE : android.view.View.GONE);
            liveChip.setVisibility(scrolled ? android.view.View.VISIBLE : android.view.View.GONE);

            if (!hasHistory) return;
            final android.widget.FrameLayout.LayoutParams trackLp = (android.widget.FrameLayout.LayoutParams) track.getLayoutParams();
            final int trackHeight = Math.max(1, overlay.getHeight() - trackLp.topMargin - trackLp.bottomMargin);
            final int visibleRows = Math.max(1, renderH / Math.max(1, cellHeightPx));
            final int totalRows = historyCount + visibleRows;
            int thumbHeight = (int) Math.round((double) trackHeight * ((double) visibleRows / (double) Math.max(visibleRows, totalRows)));
            thumbHeight = Math.max(Math.round(24 * overlay.getResources().getDisplayMetrics().density), Math.min(trackHeight, thumbHeight));
            final int travel = Math.max(0, trackHeight - thumbHeight);
            final double ratio = historyCount > 0 ? (double) offset / (double) historyCount : 0.0;
            final int topOffset = (int) Math.round(ratio * travel);

            final android.widget.FrameLayout.LayoutParams thumbLp = (android.widget.FrameLayout.LayoutParams) thumb.getLayoutParams();
            thumbLp.height = thumbHeight;
            thumbLp.topMargin = trackLp.topMargin + topOffset;
            thumb.setLayoutParams(thumbLp);
        });
    }

    private void scheduleScrollFlingFrame() {
        if (scrollFlingScheduled) return;
        scrollFlingScheduled = true;
        android.view.Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
            scrollFlingScheduled = false;
            final android.widget.OverScroller scroller = scrollFlingScroller;
            if (scroller == null) return;
            if (!scroller.computeScrollOffset()) return;
            final int currY = scroller.getCurrY();
            final float deltaY = currY - scrollFlingLastY;
            scrollFlingLastY = currY;
            applyScrollDelta(deltaY);
            if (!scroller.isFinished()) {
                scheduleScrollFlingFrame();
            }
        });
    }

    private void stopScrollFling() {
        final android.widget.OverScroller scroller = scrollFlingScroller;
        if (scroller != null && !scroller.isFinished()) {
            scroller.forceFinished(true);
        }
        scrollFlingScheduled = false;
        scrollFlingLastY = 0;
        scrollRemainderRows = 0f;
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
