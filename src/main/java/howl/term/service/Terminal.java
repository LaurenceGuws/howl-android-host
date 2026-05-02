package howl.term.service;

/** JNI-backed terminal object. */
public final class Terminal {
    private static final String TAG = "howl.term.runtime";
    private static final boolean READY;

    public enum State {
        STOPPED,
        STARTING,
        READY,
        FAILED
    }

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            loaded = BindNativeMethods(Terminal.class) == 0;
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "native load failed", err);
        }
        READY = loaded;
    }

    private long handle;
    private State state;
    private String shell;
    private String command;
    private int cellWidthPx;
    private int cellHeightPx;
    private long lastMissingGlyphs;
    private long lastFallbackHits;
    private long lastFallbackMisses;
    private long lastShapedClusters;
    private int telemetryFrameCounter;

    public Terminal() {
        this.handle = 0L;
        this.state = State.STOPPED;
        this.shell = null;
        this.command = null;
        this.cellWidthPx = 12;
        this.cellHeightPx = 24;
        this.lastMissingGlyphs = 0L;
        this.lastFallbackHits = 0L;
        this.lastFallbackMisses = 0L;
        this.lastShapedClusters = 0L;
        this.telemetryFrameCounter = 0;
    }

    public void setCellSizePx(int width, int height) {
        if (width >= 1) this.cellWidthPx = width;
        if (height >= 1) this.cellHeightPx = height;
    }

    public void configure(String shellPath, String commandText) {
        this.shell = shellPath;
        this.command = commandText;
        android.util.Log.i(TAG, "terminal.configure shell=" + shellPath + " cmd=" + (commandText != null ? "set" : "null"));
    }

    public boolean start() {
        state = State.STARTING;
        android.util.Log.i(TAG, "terminal.start begin ready=" + READY + " shell=" + shell + " cw=" + cellWidthPx + " ch=" + cellHeightPx);
        if (!READY) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: runtime not ready");
            return false;
        }
        if (shell == null || shell.isEmpty()) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: shell missing");
            return false;
        }
        handle = Create(shell, command, 60, 40, cellWidthPx, cellHeightPx);
        if (handle == 0L) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: native Create returned 0");
            return false;
        }
        state = State.READY;
        android.util.Log.i(TAG, "terminal.start ok handle=" + handle);
        return true;
    }

    public int setPrimaryFontPath(String path) {
        if (handle == 0L) return -1;
        final int rc = SetPrimaryFontPath(handle, path);
        android.util.Log.i(TAG, "terminal.setPrimaryFontPath rc=" + rc + " path=" + path);
        return rc;
    }

    public int setFallbackFontPaths(String[] paths) {
        if (handle == 0L) return -1;
        final int rc = SetFallbackFontPaths(handle, paths);
        android.util.Log.i(TAG, "terminal.setFallbackFontPaths rc=" + rc + " count=" + (paths != null ? paths.length : 0));
        return rc;
    }

    public void stop() {
        if (handle != 0L) Destroy(handle);
        handle = 0L;
        state = State.STOPPED;
    }

    public int publishInputBytes(byte[] bytes) {
        if (handle == 0L || bytes == null || bytes.length == 0) return -1;
        final int rc = PublishInputBytes(handle, bytes);
        if (rc < 0) android.util.Log.e(TAG, "terminal.publishInputBytes rc=" + rc + " n=" + bytes.length);
        return rc;
    }

    public int renderFrameSized(int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture) {
        if (handle == 0L) return -1;
        final int rc = RenderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture);
        telemetryFrameCounter += 1;
        if (telemetryFrameCounter >= 30) {
            telemetryFrameCounter = 0;
            logRenderTelemetry();
        }
        if (rc < 0) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.renderFrameSized rc=" + rc + " rw=" + renderWidth + " rh=" + renderHeight + " gw=" + gridWidth + " gh=" + gridHeight + " tex=" + texture);
        }
        return rc;
    }

    public int presentAck() {
        if (handle == 0L) return -1;
        final int rc = PresentAck(handle);
        if (rc < 0) android.util.Log.e(TAG, "terminal.presentAck rc=" + rc);
        return rc;
    }

    public int waitRenderWake(int timeoutMs) {
        if (handle == 0L) return -1;
        final int rc = WaitRenderWake(handle, timeoutMs);
        if (rc < 0) android.util.Log.e(TAG, "terminal.waitRenderWake rc=" + rc + " timeoutMs=" + timeoutMs);
        return rc;
    }

    public State state() {
        return state;
    }

    public boolean hasOutputProof() {
        if (handle == 0L) return false;
        return HasOutputProof(handle) != 0;
    }

    public boolean isAlive() {
        if (handle == 0L) return false;
        return IsSessionAlive(handle) != 0;
    }

    private void logRenderTelemetry() {
        if (handle == 0L) return;
        final long missing = RenderMissingGlyphs(handle);
        final long hits = RenderFallbackHits(handle);
        final long misses = RenderFallbackMisses(handle);
        final long shaped = RenderShapedClusters(handle);
        final int stage = RenderResolveStage(handle);
        if (missing != lastMissingGlyphs || hits != lastFallbackHits || misses != lastFallbackMisses || shaped != lastShapedClusters) {
            android.util.Log.i(
                    TAG,
                    "render.telemetry missing=" + missing +
                            " fallback_hits=" + hits +
                            " fallback_misses=" + misses +
                            " shaped=" + shaped +
                            " stage=" + stage);
            lastMissingGlyphs = missing;
            lastFallbackHits = hits;
            lastFallbackMisses = misses;
            lastShapedClusters = shaped;
        }
    }

    private static native long Create(String shell, String command, int cols, int rows, int cellWidth, int cellHeight);
    private static native void Destroy(long handle);
    private static native int RenderFrameSized(long handle, int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture);
    private static native int PublishInputBytes(long handle, byte[] data);
    private static native int PresentAck(long handle);
    private static native int WaitRenderWake(long handle, int timeoutMs);
    private static native int SetPrimaryFontPath(long handle, String path);
    private static native int SetFallbackFontPaths(long handle, String[] paths);
    private static native long RenderMissingGlyphs(long handle);
    private static native long RenderFallbackHits(long handle);
    private static native long RenderFallbackMisses(long handle);
    private static native long RenderShapedClusters(long handle);
    private static native int RenderResolveStage(long handle);
    private static native int HasOutputProof(long handle);
    private static native int IsSessionAlive(long handle);
    private static native int BindNativeMethods(Class<?> cls);
}
