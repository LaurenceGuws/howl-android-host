package howl.term.widget.term_surface;

import howl.term.service.android.GpuRuntime;

/** Full-window GLES surface owner for host render lifecycle. */
public final class View extends android.opengl.GLSurfaceView {
    public View(android.content.Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new Renderer());
        setRenderMode(android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private static final class Renderer implements android.opengl.GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(
                javax.microedition.khronos.opengles.GL10 gl,
                javax.microedition.khronos.egl.EGLConfig config
        ) {
            GpuRuntime.clearColor();
        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            GpuRuntime.viewport(width, height);
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
            GpuRuntime.draw();
        }
    }
}
