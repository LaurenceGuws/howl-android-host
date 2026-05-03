package howl.term.terminal;

import howl.term.Terminal;

/** JNI leaf for terminal native binding and runtime readiness. */
public final class NativeBinding {
    private static final String TAG = "howl.term.runtime";
    private static final boolean READY;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("howl_term");
            loaded = Terminal.bindNativeMethods(NativeBinding.class) == 0;
        } catch (UnsatisfiedLinkError err) {
            android.util.Log.e(TAG, "native load failed", err);
        }
        READY = loaded;
    }

    private NativeBinding() {}

    public static boolean ready() {
        return READY;
    }

    public static long create(String shell, String command, int cols, int rows, int cellWidth, int cellHeight) {
        return Create(shell, command, cols, rows, cellWidth, cellHeight);
    }

    public static void destroy(long handle) { Destroy(handle); }
    public static int renderFrameSized(long handle, int renderWidth, int renderHeight, int gridWidth, int gridHeight, int texture) { return RenderFrameSized(handle, renderWidth, renderHeight, gridWidth, gridHeight, texture); }
    public static int publishInputBytes(long handle, byte[] data) { return PublishInputBytes(handle, data); }
    public static int presentAck(long handle) { return PresentAck(handle); }
    public static int waitRenderWake(long handle, int timeoutMs) { return WaitRenderWake(handle, timeoutMs); }
    public static int setPrimaryFontPath(long handle, String path) { return SetPrimaryFontPath(handle, path); }
    public static int setFallbackFontPaths(long handle, String[] paths) { return SetFallbackFontPaths(handle, paths); }
    public static long renderMissingGlyphs(long handle) { return RenderMissingGlyphs(handle); }
    public static long renderFallbackHits(long handle) { return RenderFallbackHits(handle); }
    public static long renderFallbackMisses(long handle) { return RenderFallbackMisses(handle); }
    public static long renderShapedClusters(long handle) { return RenderShapedClusters(handle); }
    public static int renderResolveStage(long handle) { return RenderResolveStage(handle); }
    public static int hasOutputProof(long handle) { return HasOutputProof(handle); }
    public static int currentScrollbackCount(long handle) { return CurrentScrollbackCount(handle); }
    public static int currentScrollbackOffset(long handle) { return CurrentScrollbackOffset(handle); }
    public static int setScrollbackOffset(long handle, int offsetRows) { return SetScrollbackOffset(handle, offsetRows); }
    public static int followLiveBottom(long handle) { return FollowLiveBottom(handle); }
    public static int isSessionAlive(long handle) { return IsSessionAlive(handle); }

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
    private static native int CurrentScrollbackCount(long handle);
    private static native int CurrentScrollbackOffset(long handle);
    private static native int SetScrollbackOffset(long handle, int offsetRows);
    private static native int FollowLiveBottom(long handle);
    private static native int IsSessionAlive(long handle);
}
