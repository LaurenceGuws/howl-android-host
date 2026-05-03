package howl.term;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Responsibility: own the public GPU surface for the Android host.
 * Ownership: GL surface creation, texture management, and present flow.
 * Reason: keep GL details behind one boring host owner.
 */
public final class Gpu {
    private static final String TAG = "howl.term.runtime";
    /** Mutable GL state carried across surface callbacks. */
    public static final class State {
        public int texture;
        public int program;
        public int posHandle;
        public int uvHandle;
        public int samplerHandle;
        public FloatBuffer quad;
        public int textureWidth;
        public int textureHeight;
        public boolean frameReady;
        public int drawSkipLogs;

        /** Construct one zeroed GL state holder. */
        public State() {
            texture = 0;
            program = 0;
            posHandle = -1;
            uvHandle = -1;
            samplerHandle = -1;
            quad = null;
            textureWidth = 1;
            textureHeight = 1;
            frameReady = false;
            drawSkipLogs = 0;
        }
    }

    /** Host callback contract for GPU surface lifecycle events. */
    public interface Hooks {
        void onSurfaceCreated();
        void onSurfaceChanged(int width, int height);
        void onDrawFrame();
        void onSurfaceDestroyed();
    }

    /** Create the host GL surface and bind lifecycle hooks. */
    public android.view.View createSurface(android.app.Activity activity, State state, Hooks hooks) {
        final GLSurfaceView view = new GLSurfaceView(activity);
        view.setEGLContextClientVersion(2);
        view.setPreserveEGLContextOnPause(true);
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl, javax.microedition.khronos.egl.EGLConfig cfg) {
                initTexture(state);
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
                draw(state);
            }
        });
        view.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            @Override public void surfaceCreated(android.view.SurfaceHolder holder) {}
            @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) { hooks.onSurfaceDestroyed(); }
        });
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return view;
    }

    /** Queue one render request on the GL surface if available. */
    public void requestRender(android.view.View v) {
        if (v instanceof GLSurfaceView gl) gl.requestRender();
    }

    /** Queue one texture resize on the GL thread. */
    public void resizeTexture(State state, android.view.View v, int width, int height) {
        if (!(v instanceof GLSurfaceView gl)) return;
        final int w = Math.max(1, width);
        final int h = Math.max(1, height);
        gl.queueEvent(() -> ensureTextureSize(state, w, h));
    }

    /** Return the current texture handle. */
    public int texture(State state) { return state.texture; }

    /** Ensure the backing texture matches the requested size. */
    public void ensureTextureSize(State state, int width, int height) {
        if (state.texture == 0) return;
        final int w = Math.max(1, width);
        final int h = Math.max(1, height);
        if (w == state.textureWidth && h == state.textureHeight) return;
        state.textureWidth = w;
        state.textureHeight = h;
        state.frameReady = false;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, state.texture);
        final ByteBuffer zeros = ByteBuffer.allocateDirect(w * h * 4);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, zeros);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        android.util.Log.i(TAG, "gpu.ensureTextureSize tex=" + state.texture + " " + w + "x" + h);
    }

    private void initTexture(State state) {
        if (state.texture != 0) return;
        final int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        state.texture = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, state.texture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ByteBuffer.wrap(new byte[] {20, 28, 45, (byte) 255}));
        state.textureWidth = 1;
        state.textureHeight = 1;
        state.frameReady = false;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /** Mark whether a fresh frame is ready for present. */
    public void markFrameReady(State state, boolean ready) {
        state.frameReady = ready;
    }

    private void draw(State state) {
        if (state.program == 0) initProgram(state);
        if (state.program == 0 || state.texture == 0 || !state.frameReady) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            if (state.texture != 0 && state.drawSkipLogs < 20) {
                state.drawSkipLogs++;
                android.util.Log.i(TAG, "gpu.draw skipped program=" + state.program + " frameReady=" + state.frameReady + " tex=" + state.texture);
            }
            return;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(state.program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, state.texture);
        GLES20.glUniform1i(state.samplerHandle, 0);
        state.quad.position(0);
        GLES20.glEnableVertexAttribArray(state.posHandle);
        GLES20.glVertexAttribPointer(state.posHandle, 2, GLES20.GL_FLOAT, false, 16, state.quad);
        state.quad.position(2);
        GLES20.glEnableVertexAttribArray(state.uvHandle);
        GLES20.glVertexAttribPointer(state.uvHandle, 2, GLES20.GL_FLOAT, false, 16, state.quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(state.posHandle);
        GLES20.glDisableVertexAttribArray(state.uvHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        final int glErr = GLES20.glGetError();
        if (glErr != GLES20.GL_NO_ERROR) {
            android.util.Log.e(TAG, "gpu.draw glError=0x" + Integer.toHexString(glErr));
        }
    }

    private void initProgram(State state) {
        final String vsSrc = "attribute vec2 aPos;attribute vec2 aUv;varying vec2 vUv;void main(){gl_Position=vec4(aPos,0.0,1.0);vUv=aUv;}";
        final String fsSrc = "precision mediump float;varying vec2 vUv;uniform sampler2D uTex;void main(){gl_FragColor=texture2D(uTex,vUv);}";
        final int vs = compile(GLES20.GL_VERTEX_SHADER, vsSrc);
        final int fs = compile(GLES20.GL_FRAGMENT_SHADER, fsSrc);
        if (vs == 0 || fs == 0) return;

        state.program = GLES20.glCreateProgram();
        GLES20.glAttachShader(state.program, vs);
        GLES20.glAttachShader(state.program, fs);
        GLES20.glLinkProgram(state.program);
        final int[] linked = new int[1];
        GLES20.glGetProgramiv(state.program, GLES20.GL_LINK_STATUS, linked, 0);
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
        if (linked[0] == 0) {
            GLES20.glDeleteProgram(state.program);
            state.program = 0;
            android.util.Log.e(TAG, "gpu.initProgram link failed");
            return;
        }

        state.posHandle = GLES20.glGetAttribLocation(state.program, "aPos");
        state.uvHandle = GLES20.glGetAttribLocation(state.program, "aUv");
        state.samplerHandle = GLES20.glGetUniformLocation(state.program, "uTex");

        final float[] verts = {-1f,-1f,0f,0f, 1f,-1f,1f,0f, -1f,1f,0f,1f, 1f,1f,1f,1f};
        state.quad = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        state.quad.put(verts);
        state.quad.position(0);
        android.util.Log.i(TAG, "gpu.initProgram ok program=" + state.program + " tex=" + state.texture);
    }

    private int compile(int type, String src) {
        final int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, src);
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
