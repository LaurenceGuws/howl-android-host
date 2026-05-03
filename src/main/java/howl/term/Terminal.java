package howl.term;

import howl.term.terminal.NativeBinding;
import howl.term.terminal.RenderTelemetry;

/** JNI-backed terminal object. */
public final class Terminal {
    private static final String TAG = "howl.term.runtime";

    public static int bindNativeMethods(Class<?> cls) {
        return BindNativeMethods(cls);
    }

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

    public Terminal() {
        this.handle = 0L;
        this.state = State.STOPPED;
        this.shell = null;
        this.command = null;
        this.cellWidthPx = 12;
        this.cellHeightPx = 24;
        this.telemetry = new RenderTelemetry();
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

    public int setPrimaryFontPath(String path) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.setPrimaryFontPath(handle, path);
        android.util.Log.i(TAG, "terminal.setPrimaryFontPath rc=" + rc + " path=" + path);
        return rc;
    }

    public int setFallbackFontPaths(String[] paths) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.setFallbackFontPaths(handle, paths);
        android.util.Log.i(TAG, "terminal.setFallbackFontPaths rc=" + rc + " count=" + (paths != null ? paths.length : 0));
        return rc;
    }

    public void stop() {
        if (handle != 0L) NativeBinding.destroy(handle);
        handle = 0L;
        state = State.STOPPED;
    }

    public int publishInputBytes(byte[] bytes) {
        if (handle == 0L || bytes == null || bytes.length == 0) return -1;
        final int rc = NativeBinding.publishInputBytes(handle, bytes);
        if (rc < 0) android.util.Log.e(TAG, "terminal.publishInputBytes rc=" + rc + " n=" + bytes.length);
        return rc;
    }

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

    public int presentAck() {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.presentAck(handle);
        if (rc < 0) android.util.Log.e(TAG, "terminal.presentAck rc=" + rc);
        return rc;
    }

    public int waitRenderWake(int timeoutMs) {
        if (handle == 0L) return -1;
        final int rc = NativeBinding.waitRenderWake(handle, timeoutMs);
        if (rc < 0) android.util.Log.e(TAG, "terminal.waitRenderWake rc=" + rc + " timeoutMs=" + timeoutMs);
        return rc;
    }

    public State state() {
        return state;
    }

    public boolean hasOutputProof() {
        if (handle == 0L) return false;
        return NativeBinding.hasOutputProof(handle) != 0;
    }

    public int currentScrollbackCount() {
        if (handle == 0L) return 0;
        return NativeBinding.currentScrollbackCount(handle);
    }

    public int currentScrollbackOffset() {
        if (handle == 0L) return 0;
        return NativeBinding.currentScrollbackOffset(handle);
    }

    public int setScrollbackOffset(int offsetRows) {
        if (handle == 0L) return -1;
        return NativeBinding.setScrollbackOffset(handle, Math.max(0, offsetRows));
    }

    public int followLiveBottom() {
        if (handle == 0L) return -1;
        return NativeBinding.followLiveBottom(handle);
    }

    public boolean isAlive() {
        if (handle == 0L) return false;
        return NativeBinding.isSessionAlive(handle) != 0;
    }

    private static native int BindNativeMethods(Class<?> cls);
}
