package howl.term.service.howl_term;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

/** GLES calls for terminal surface drawing. */
public final class GpuRuntime {
    private GpuRuntime() {}

    public static Object createSurface(Object contextHandle) {
        final Context context = (Context) contextHandle;
        final GLSurfaceView surface = new GLSurfaceView(context);
        surface.setEGLContextClientVersion(2);
        surface.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
                clearColor();
            }

            @Override
            public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
                viewport(width, height);
            }

            @Override
            public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
                draw();
            }
        });
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        return surface;
    }

    public static void resumeSurface(Object surfaceHandle) {
        ((GLSurfaceView) surfaceHandle).onResume();
    }

    public static void pauseSurface(Object surfaceHandle) {
        ((GLSurfaceView) surfaceHandle).onPause();
    }

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
