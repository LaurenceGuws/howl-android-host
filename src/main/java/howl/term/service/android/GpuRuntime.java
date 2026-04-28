package howl.term.service.android;

import android.opengl.GLES20;

/** GLES calls for terminal surface drawing. */
public final class GpuRuntime {
    private GpuRuntime() {}

    public static void clearColor() {
        GLES20.glClearColor(0.06f, 0.09f, 0.14f, 1.0f);
    }

    public static void viewport(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public static void draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }
}
