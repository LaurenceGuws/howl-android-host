package howl.term.service;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

/** GPU object: surface + texture present. */
public final class Gpu {
    private static final String TAG = "howl.term.runtime";
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

    public interface Hooks {
        void onSurfaceCreated();
        void onSurfaceChanged(int width, int height);
        void onDrawFrame();
        void onSurfaceDestroyed();
        void onInputBytes(byte[] bytes);
    }

    public android.view.View createSurface(android.app.Activity activity, State state, Hooks hooks) {
        final GLSurfaceView view = new GLSurfaceView(activity) {
            @Override
            public boolean onCheckIsTextEditor() { return true; }

            @Override
            public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
                outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
                outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                        | EditorInfo.IME_FLAG_NO_FULLSCREEN
                        | EditorInfo.IME_ACTION_NONE;
                return new BaseInputConnection(this, false) {
                    private static final String SENTINEL = "........";
                    private final StringBuilder editorBuffer = new StringBuilder();
                    private int editorCursor = 0;
                    private int editorComposingStart = -1;
                    private int editorComposingEnd = -1;
                    private String suppressedCommitText = null;

                    { resetEditorState(); }

                    private void publishText(CharSequence text) {
                        if (text == null || text.length() == 0) return;
                        hooks.onInputBytes(text.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    private void sendCodepoint(int cp) {
                        publishText(new String(Character.toChars(cp)));
                    }

                    private void resetEditorState() {
                        editorBuffer.setLength(0);
                        editorBuffer.append(SENTINEL).append('\n').append(SENTINEL).append('\n').append(SENTINEL);
                        editorCursor = SENTINEL.length() + 1;
                        editorComposingStart = -1;
                        editorComposingEnd = -1;
                    }

                    private int editorLineStart() {
                        int i = editorCursor - 1;
                        while (i >= 0 && editorBuffer.charAt(i) != '\n') i--;
                        return i + 1;
                    }

                    private String currentCompositionText() {
                        if (editorComposingStart >= 0 && editorComposingEnd >= editorComposingStart) {
                            return editorBuffer.substring(editorComposingStart, editorComposingEnd);
                        }
                        return "";
                    }

                    private int currentCompositionStart() {
                        return editorComposingStart >= 0 ? editorComposingStart : editorCursor;
                    }

                    private int currentCompositionEnd() {
                        return (editorComposingEnd >= editorComposingStart && editorComposingStart >= 0) ? editorComposingEnd : editorCursor;
                    }

                    private static int sharedPrefixLength(String left, String right) {
                        int n = 0;
                        final int max = Math.min(left.length(), right.length());
                        while (n < max && left.charAt(n) == right.charAt(n)) n++;
                        return n;
                    }

                    private String currentCommittedLinePrefix() {
                        final int lineStart = editorLineStart();
                        final int composeStart = currentCompositionStart();
                        if (composeStart <= lineStart) return "";
                        return editorBuffer.substring(lineStart, composeStart);
                    }

                    private String normalizeImeCompositionText(String text) {
                        if (text.isEmpty()) return text;
                        final String committedLinePrefix = currentCommittedLinePrefix();
                        if (committedLinePrefix.isEmpty()) return text;
                        if (!text.startsWith(committedLinePrefix)) return text;
                        return text.substring(committedLinePrefix.length());
                    }

                    private boolean shouldSuppressCommitText(String text) {
                        if (suppressedCommitText == null || !suppressedCommitText.equals(text)) return false;
                        suppressedCommitText = null;
                        return true;
                    }

                    private void clearComposition() {
                        editorComposingStart = -1;
                        editorComposingEnd = -1;
                    }

                    private void replaceComposition(String next) {
                        final String previous = currentCompositionText();
                        final int composeStart = currentCompositionStart();
                        final int oldEnd = currentCompositionEnd();
                        final int commonPrefix = sharedPrefixLength(previous, next);
                        final int backspaces = previous.length() - commonPrefix;
                        for (int i = 0; i < backspaces; i++) sendCodepoint('\u007f');
                        final String appended = next.substring(commonPrefix);
                        if (!appended.isEmpty()) publishText(appended);
                        editorBuffer.delete(composeStart, oldEnd);
                        editorBuffer.insert(composeStart, next);
                        editorComposingStart = composeStart;
                        editorComposingEnd = composeStart + next.length();
                        editorCursor = editorComposingEnd;
                        if (next.isEmpty()) clearComposition();
                    }

                    private boolean applyImeText(String rawText, boolean commit) {
                        final String text = normalizeImeCompositionText(rawText);
                        if (commit && shouldSuppressCommitText(text)) return true;
                        if (editorComposingStart >= 0 || !commit) {
                            replaceComposition(text);
                            if (commit) clearComposition();
                            return true;
                        }
                        if (text.isEmpty()) return true;
                        editorBuffer.insert(editorCursor, text);
                        editorCursor += text.length();
                        publishText(text);
                        return true;
                    }

                    @Override
                    public boolean setComposingText(CharSequence text, int newCursorPosition) {
                        return applyImeText(text == null ? "" : text.toString(), false);
                    }

                    @Override
                    public boolean finishComposingText() {
                        clearComposition();
                        return true;
                    }

                    @Override
                    public boolean commitText(CharSequence text, int newCursorPosition) {
                        return applyImeText(text == null ? "" : text.toString(), true);
                    }

                    @Override
                    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                        if (beforeLength > 0) {
                            final int delStart = Math.max(0, editorCursor - beforeLength);
                            final int count = editorCursor - delStart;
                            editorBuffer.delete(delStart, editorCursor);
                            editorCursor = delStart;
                            for (int i = 0; i < count; i++) sendCodepoint('\u007f');
                        }
                        if (afterLength > 0) {
                            final int delEnd = Math.min(editorBuffer.length(), editorCursor + afterLength);
                            editorBuffer.delete(editorCursor, delEnd);
                        }
                        return true;
                    }

                    private boolean shouldBypassImePrintableKeyEvent(KeyEvent event) {
                        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                        if (event.isCtrlPressed() || event.isAltPressed()) return false;
                        final int unicode = event.getUnicodeChar();
                        return unicode != 0 && !Character.isISOControl(unicode);
                    }

                    @Override
                    public boolean sendKeyEvent(KeyEvent event) {
                        if (shouldBypassImePrintableKeyEvent(event)) return true;
                        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                        final byte[] mapped = Input.mapKeyEvent(event);
                        if (mapped != null && mapped.length > 0) {
                            hooks.onInputBytes(mapped);
                            return true;
                        }
                        if (shouldBypassImePrintableKeyEvent(event)) return true;
                        return super.sendKeyEvent(event);
                    }
                };
            }
        };
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
        view.setOnKeyListener((v, code, event) -> {
            if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            byte[] mapped = Input.mapKeyEvent(event);
            if (mapped == null || mapped.length == 0) return false;
            hooks.onInputBytes(mapped);
            return true;
        });
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return view;
    }

    public void requestRender(android.view.View v) {
        if (v instanceof GLSurfaceView gl) gl.requestRender();
    }

    public void resizeTexture(State state, android.view.View v, int width, int height) {
        if (!(v instanceof GLSurfaceView gl)) return;
        final int w = Math.max(1, width);
        final int h = Math.max(1, height);
        gl.queueEvent(() -> ensureTextureSize(state, w, h));
    }

    public int texture(State state) { return state.texture; }

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
