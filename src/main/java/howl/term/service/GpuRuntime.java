package howl.term.service;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
/** Presents a single gpu texture */
public class GpuRuntime {
    public interface FrameHooks {
        void onSurfaceCreated();
        void onSurfaceChanged(int width, int height);
        void onDrawFrame();
        void onSurfaceDestroyed();
    }

    private int texture;
    private int program;
    private int posHandle;
    private int uvHandle;
    private int samplerHandle;
    private FloatBuffer quadBuffer;
    private int texWidth;
    private int texHeight;
    private ByteBuffer texPixels;

    public GpuRuntime() {
        this.texture = 0;
        this.program = 0;
        this.posHandle = -1;
        this.uvHandle = -1;
        this.samplerHandle = -1;
        this.quadBuffer = null;
        this.texWidth = 0;
        this.texHeight = 0;
        this.texPixels = null;
    }

    public android.view.View surface(android.app.Activity activity, FrameHooks hooks) {
        final GLSurfaceView view = new GLSurfaceView(activity);
        view.setEGLContextClientVersion(2);
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
                initTexture();
                GLES20.glClearColor(0.06f, 0.09f, 0.14f, 1.0f);
                hooks.onSurfaceCreated();
            }

            @Override
            public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
                hooks.onSurfaceChanged(width, height);
            }

            @Override
            public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
                hooks.onDrawFrame();
                draw();
            }
        });
        view.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(android.view.SurfaceHolder holder) {}

            @Override
            public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(android.view.SurfaceHolder holder) {
                hooks.onSurfaceDestroyed();
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
        texWidth = 1;
        texHeight = 1;
        texPixels = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
    }

    public void presentMask(int cols, int rows, byte[] mask, int used) {
        if (texture == 0 || cols <= 0 || rows <= 0 || mask == null || used <= 0) return;
        final int count = Math.min(used, cols * rows);
        ensureTexture(cols, rows);
        texPixels.position(0);
        for (int i = 0; i < count; i += 1) {
            final boolean on = mask[i] != 0;
            texPixels.put((byte) (on ? 210 : 20));
            texPixels.put((byte) (on ? 220 : 28));
            texPixels.put((byte) (on ? 230 : 45));
            texPixels.put((byte) 255);
        }
        for (int i = count; i < (cols * rows); i += 1) {
            texPixels.put((byte) 20);
            texPixels.put((byte) 28);
            texPixels.put((byte) 45);
            texPixels.put((byte) 255);
        }
        texPixels.position(0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                cols,
                rows,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                texPixels
        );
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void ensureTexture(int width, int height) {
        if (width == texWidth && height == texHeight && texPixels != null) {
            return;
        }
        texWidth = width;
        texHeight = height;
        texPixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                null
        );
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void draw() {
        if (program == 0) {
            initProgram();
        }
        if (program == 0 || texture == 0) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(samplerHandle, 0);
        quadBuffer.position(0);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 16, quadBuffer);
        quadBuffer.position(2);
        GLES20.glEnableVertexAttribArray(uvHandle);
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, 16, quadBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(posHandle);
        GLES20.glDisableVertexAttribArray(uvHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void initProgram() {
        final String vertex =
                "attribute vec2 aPos;\n" +
                "attribute vec2 aUv;\n" +
                "varying vec2 vUv;\n" +
                "void main() {\n" +
                "  gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                "  vUv = aUv;\n" +
                "}\n";
        final String fragment =
                "precision mediump float;\n" +
                "varying vec2 vUv;\n" +
                "uniform sampler2D uTex;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(uTex, vUv);\n" +
                "}\n";
        final int vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex);
        final int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        if (vs == 0 || fs == 0) return;
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        final int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        if (linked[0] == 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
            return;
        }
        posHandle = GLES20.glGetAttribLocation(program, "aPos");
        uvHandle = GLES20.glGetAttribLocation(program, "aUv");
        samplerHandle = GLES20.glGetUniformLocation(program, "uTex");
        final float[] verts = new float[] {
                -1f, -1f, 0f, 1f,
                1f, -1f, 1f, 1f,
                -1f, 1f, 0f, 0f,
                1f, 1f, 1f, 0f
        };
        quadBuffer = ByteBuffer.allocateDirect(verts.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        quadBuffer.put(verts);
        quadBuffer.position(0);
    }

    private int compileShader(int type, String source) {
        final int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        final int[] ok = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
