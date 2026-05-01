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
/** Presents a single gpu texture */
public class Gpu {
    public interface FrameHooks {
        void onSurfaceCreated();
        void onSurfaceChanged(int width, int height);
        void onDrawFrame();
        void onSurfaceDestroyed();
        void onInputBytes(byte[] bytes);
    }

    private int texture;
    private int program;
    private int posHandle;
    private int uvHandle;
    private int samplerHandle;
    private FloatBuffer quadBuffer;
    private int textureWidth;
    private int textureHeight;

    public Gpu() {
        this.texture = 0;
        this.program = 0;
        this.posHandle = -1;
        this.uvHandle = -1;
        this.samplerHandle = -1;
        this.quadBuffer = null;
        this.textureWidth = 1;
        this.textureHeight = 1;
    }

    public android.view.View surface(android.app.Activity activity, FrameHooks hooks) {
        final GLSurfaceView view = new GLSurfaceView(activity) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true;
            }

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

                    {
                        resetEditorState();
                    }

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
                        if (unicode == ' ') return false;
                        return unicode != 0 && !Character.isISOControl(unicode);
                    }

                    @Override
                    public boolean sendKeyEvent(KeyEvent event) {
                        if (shouldBypassImePrintableKeyEvent(event)) {
                            return true;
                        }
                        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
                        final byte[] mapped = mapKeyEvent(event);
                        if (mapped != null && mapped.length > 0) {
                            hooks.onInputBytes(mapped);
                            return true;
                        }
                        final int codepoint = event.getUnicodeChar();
                        if (codepoint > 0 && !Character.isISOControl(codepoint)) {
                            final String text = new String(Character.toChars(codepoint));
                            editorBuffer.insert(editorCursor, text);
                            editorCursor += text.length();
                            publishText(text);
                            return true;
                        }
                        return super.sendKeyEvent(event);
                    }
                };
            }
        };
        view.setEGLContextClientVersion(2);
        view.setPreserveEGLContextOnPause(true);
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
        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return view;
    }

    private static byte[] mapKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        final boolean ctrl = event.isCtrlPressed();
        final boolean alt = event.isAltPressed();

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) return new byte[] { '\r' };
        if (keyCode == KeyEvent.KEYCODE_DEL) return new byte[] { 0x7f };
        if (keyCode == KeyEvent.KEYCODE_TAB) return new byte[] { '\t' };
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) return new byte[] { 0x1b };
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return "\u001b[A".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return "\u001b[B".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return "\u001b[C".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return "\u001b[D".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_MOVE_HOME) return "\u001b[H".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_MOVE_END) return "\u001b[F".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) return "\u001b[5~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) return "\u001b[6~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) return "\u001b[3~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_INSERT) return "\u001b[2~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F1) return "\u001bOP".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F2) return "\u001bOQ".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F3) return "\u001bOR".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F4) return "\u001bOS".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F5) return "\u001b[15~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F6) return "\u001b[17~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F7) return "\u001b[18~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F8) return "\u001b[19~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F9) return "\u001b[20~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F10) return "\u001b[21~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F11) return "\u001b[23~".getBytes(StandardCharsets.UTF_8);
        if (keyCode == KeyEvent.KEYCODE_F12) return "\u001b[24~".getBytes(StandardCharsets.UTF_8);

        if (ctrl) {
            final int cp = event.getUnicodeChar(KeyEvent.META_CTRL_ON);
            if (cp > 0 && cp <= 0x1f) return new byte[] { (byte) cp };
        }
        if (alt) {
            final int cp = event.getUnicodeChar();
            if (cp > 0 && !Character.isISOControl(cp)) {
                final byte[] b = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8);
                final byte[] out = new byte[b.length + 1];
                out[0] = 0x1b;
                System.arraycopy(b, 0, out, 1, b.length);
                return out;
            }
        }
        return null;
    }

    public void requestRender(android.view.View view) {
        if (view instanceof GLSurfaceView glView) {
            glView.requestRender();
        }
    }

    public void resizeTexture(android.view.View view, int width, int height) {
        if (!(view instanceof GLSurfaceView glView)) return;
        final int w = Math.max(1, width);
        final int h = Math.max(1, height);
        glView.queueEvent(() -> ensureTextureSize(w, h));
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
        textureWidth = 1;
        textureHeight = 1;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public int texture() {
        return texture;
    }

    public void ensureTextureSize(int width, int height) {
        if (texture == 0) return;
        final int w = Math.max(1, width);
        final int h = Math.max(1, height);
        if (w == textureWidth && h == textureHeight) {
            return;
        }
        textureWidth = w;
        textureHeight = h;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                textureWidth,
                textureHeight,
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
                -1f, -1f, 0f, 0f,
                1f, -1f, 1f, 0f,
                -1f, 1f, 0f, 1f,
                1f, 1f, 1f, 1f
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
