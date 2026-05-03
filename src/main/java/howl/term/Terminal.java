package howl.term;

import howl.term.terminal.NativeBinding;
import howl.term.terminal.RenderTelemetry;

/**
 * Responsibility: own the Java-side terminal runtime surface.
 * Ownership: JNI lifecycle, terminal state, and render telemetry handoff.
 * Reason: keep native terminal details behind one boring host owner.
 */
public final class Terminal {
    private static final String TAG = "howl.term.runtime";

    /** Bind the terminal native methods onto one target class. */
    public static int bindNativeMethods(Class<?> cls) {
        return BindNativeMethods(cls);
    }

    /** Host-visible terminal lifecycle state. */
    public enum State {
        STOPPED,
        STARTING,
        READY,
        FAILED
    }

    private long handle;
    private State state;
    private String shell;
    private String command;
    private int cellWidthPx;
    private int cellHeightPx;
    private final RenderTelemetry telemetry;

    /** Construct one stopped Java-side terminal owner. */
    public Terminal() {
        this.handle = 0L;
        this.state = State.STOPPED;
        this.shell = null;
        this.command = null;
        this.cellWidthPx = 12;
        this.cellHeightPx = 24;
        this.telemetry = new RenderTelemetry();
    }

    /** Configure terminal cell geometry in pixels for the next start. */
    public void setCellSizePx(int width, int height) {
        if (width >= 1) this.cellWidthPx = width;
        if (height >= 1) this.cellHeightPx = height;
    }

    /** Configure shell launch inputs for the next start. */
    public void configure(String shellPath, String commandText) {
        this.shell = shellPath;
        this.command = commandText;
        android.util.Log.i(TAG, "terminal.configure shell=" + shellPath + " cmd=" + (commandText != null ? "set" : "null"));
    }

    /** Start the configured terminal session if native runtime support is ready. */
    public boolean start() {
        state = State.STARTING;
        android.util.Log.i(TAG, "terminal.start begin ready=" + NativeBinding.ready() + " shell=" + shell + " cw=" + cellWidthPx + " ch=" + cellHeightPx);
        if (!NativeBinding.ready()) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: runtime not ready");
            return false;
        }
        if (shell == null || shell.isEmpty()) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: shell missing");
            return false;
        }
        handle = NativeBinding.create(shell, command, 60, 40, cellWidthPx, cellHeightPx);
        if (handle == 0L) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.start fail: native Create returned 0");
            return false;
        }
        state = State.READY;
        telemetry.reset();
        android.util.Log.i(TAG, "terminal.start ok handle=" + handle);
        return true;
    }

    /** Set or clear the primary font path for the running terminal session. */
    public int setPrimaryFontPath(String path) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.setPrimaryFontPath(handle, path);
        android.util.Log.i(TAG, "terminal.setPrimaryFontPath rc=" + rc + " path=" + path);
        return rc;
    }

    /** Replace the configured fallback font paths for the running terminal session. */
    public int setFallbackFontPaths(String[] paths) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.setFallbackFontPaths(handle, paths);
        android.util.Log.i(TAG, "terminal.setFallbackFontPaths rc=" + rc + " count=" + (paths != null ? paths.length : 0));
        return rc;
    }

    /** Stop the running terminal session and clear the native handle. */
    public void stop() {
        if (handle != 0L) NativeBinding.destroy(handle);
        handle = 0L;
        state = State.STOPPED;
    }

    /** Publish raw host input bytes into the running terminal session. */
    public int publishInputBytes(byte[] bytes) {
        if (handle == 0L || bytes == null || bytes.length == 0) return -1;
        final int rc = NativeBinding.publishInputBytes(handle, bytes);
        if (rc < 0) android.util.Log.e(TAG, "terminal.publishInputBytes rc=" + rc + " n=" + bytes.length);
        return rc;
    }

    /** Render one frame with independent render and grid geometry. */
    public int renderFrameSized(int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.renderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture);
        telemetry.onRenderedFrame(handle);
        if (rc < 0) {
            state = State.FAILED;
            android.util.Log.e(TAG, "terminal.renderFrameSized rc=" + rc + " rw=" + renderWidth + " rh=" + renderHeight + " gw=" + gridWidth + " gh=" + gridHeight + " tex=" + texture);
        }
        return rc;
    }

    /** Acknowledge presentation on the running terminal session. */
    public int presentAck() {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.presentAck(handle);
        if (rc < 0) android.util.Log.e(TAG, "terminal.presentAck rc=" + rc);
        return rc;
    }

    /** Wait until render work is armed or the timeout expires. */
    public int waitRenderWake(int timeoutMs) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.waitRenderWake(handle, timeoutMs);
        if (rc < 0) android.util.Log.e(TAG, "terminal.waitRenderWake rc=" + rc + " timeoutMs=" + timeoutMs);
        return rc;
    }

    /** Report the current terminal lifecycle state. */
    public State state() {
        return state;
    }

    /** Report whether transport output has been observed. */
    public boolean hasOutputProof() {
        if (handle == 0L) return false;
        return NativeBinding.hasOutputProof(handle) != 0;
    }

    /** Report the total current scrollback history row count. */
    public int currentScrollbackCount() {
        if (handle == 0L) return 0;
        return NativeBinding.currentScrollbackCount(handle);
    }

    /** Report the current scrollback offset from the live bottom. */
    public int currentScrollbackOffset() {
        if (handle == 0L) return 0;
        return NativeBinding.currentScrollbackOffset(handle);
    }

    /** Set the active scrollback offset. */
    public int setScrollbackOffset(int offsetRows) {
        if (handle == 0L) return -1;
        return NativeBinding.setScrollbackOffset(handle, Math.max(0, offsetRows));
    }

    /** Return the viewport to the live bottom. */
    public int followLiveBottom() {
        if (handle == 0L) return -1;
        return NativeBinding.followLiveBottom(handle);
    }

    /** Report whether the native terminal runtime is still alive. */
    public boolean isAlive() {
        if (handle == 0L) return false;
        return NativeBinding.isSessionAlive(handle) != 0;
    }

    private static native int BindNativeMethods(Class<?> cls);
}
