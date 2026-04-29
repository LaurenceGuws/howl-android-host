package howl.term.service;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import howl.term.service.TerminalRuntime;

/** Presents a single gpu texture */
public class GpuRuntime {
    private static final String TAG = "howl.term.runtime";
    private int texture;
    private final TerminalRuntime termRuntime;
    private boolean termStarted;
    private int lastCols;
    private int lastRows;

    public GpuRuntime() {
        this.texture = 0;
        this.termRuntime = new TerminalRuntime();
        this.termStarted = false;
        this.lastCols = 0;
        this.lastRows = 0;
    }

    public android.view.View surface(android.app.Activity activity) {
        final GLSurfaceView view = new GLSurfaceView(activity);
        view.setEGLContextClientVersion(2);
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
                initTexture();
                GLES20.glClearColor(0.06f, 0.09f, 0.14f, 1.0f);
                termStarted = termRuntime.start();
            }

            @Override
            public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
                final int cols = Math.max(1, width / 8);
                final int rows = Math.max(1, height / 16);
                if (termStarted && (cols != lastCols || rows != lastRows)) {
                    final int rc = termRuntime.resize(cols, rows);
                    android.util.Log.i(TAG, "runtime.resize cols=" + cols + " rows=" + rows + " rc=" + rc);
                    lastCols = cols;
                    lastRows = rows;
                }
            }

            @Override
            public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
                if (termStarted) {
                    final int drained = termRuntime.tick();
                    if (drained < 0) {
                        android.util.Log.e(TAG, "runtime.tick rc=" + drained);
                    }
                }
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
        });
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        return view;
    }

    private void initTexture() {
        if (texture != 0) return;
        final int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        texture = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        final byte[] pixel = new byte[] { 20, 28, 45, (byte) 255 };
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                1,
                1,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(pixel)
        );
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
