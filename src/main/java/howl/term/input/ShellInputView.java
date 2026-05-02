package howl.term.input;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

public final class ShellInputView extends View {
    public interface Host {
        void sendDirectText(String text);

        void sendDirectCodepoint(int codepoint);
    }

    private static final String SENTINEL = "........";

    private final Host host;
    private final StringBuilder editorBuffer = new StringBuilder();
    private int editorCursor = 0;
    private int editorComposingStart = -1;
    private int editorComposingEnd = -1;
    private String suppressedCommitText = null;

    public ShellInputView(Context context, Host host) {
        super(context);
        this.host = host;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                resetEditorState();
            } else {
                editorComposingStart = -1;
                editorComposingEnd = -1;
            }
        });
        resetEditorState();
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        configureEditorInfo(outAttrs);
        return new ShellInputConnection();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (handleHardwareKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean handleHardwareKeyEvent(KeyEvent event) {
        return handleKeyEvent(event);
    }

    private void configureEditorInfo(EditorInfo outAttrs) {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT
                | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | EditorInfo.IME_FLAG_NO_FULLSCREEN
                | EditorInfo.IME_ACTION_NONE;
        outAttrs.initialSelStart = editorCursor;
        outAttrs.initialSelEnd = editorCursor;
    }

    private final class ShellInputConnection extends BaseInputConnection {
        ShellInputConnection() {
            super(ShellInputView.this, false);
        }

        @Override
        public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
            final ExtractedText et = new ExtractedText();
            et.text = editorBuffer.toString();
            et.startOffset = 0;
            et.partialStartOffset = 0;
            et.partialEndOffset = et.text.length();
            et.selectionStart = editorCursor;
            et.selectionEnd = editorCursor;
            et.flags = 0;
            return et;
        }

        @Override
        public CharSequence getTextBeforeCursor(int n, int flags) {
            final int start = Math.max(0, editorCursor - n);
            return editorBuffer.substring(start, editorCursor);
        }

        @Override
        public CharSequence getTextAfterCursor(int n, int flags) {
            final int end = Math.min(editorBuffer.length(), editorCursor + n);
            return editorBuffer.substring(editorCursor, end);
        }

        @Override
        public boolean setSelection(int start, int end) {
            final int oldCursor = editorCursor;
            final int newCursor = Math.max(0, Math.min(start, editorBuffer.length()));
            if (newCursor == oldCursor) {
                return true;
            }
            emitCursorMoveEscapes(oldCursor, newCursor);
            resetEditorState();
            return true;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            return applyImeText(text == null ? "" : text.toString(), false);
        }

        @Override
        public boolean finishComposingText() {
            editorComposingStart = -1;
            editorComposingEnd = -1;
            return true;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            return applyImeText(text == null ? "" : text.toString(), true);
        }

        @Override
        public boolean performEditorAction(int actionCode) {
            host.sendDirectCodepoint('\r');
            resetEditorState();
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            deleteTextBeforeCursorAndSendBackspace(beforeLength);
            deleteTextAfterCursor(afterLength);
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (shouldBypassImePrintableKeyEvent(event)) {
                return true;
            }
            if (handleKeyEvent(event)) {
                return true;
            }
            return super.sendKeyEvent(event);
        }
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

    private void emitCursorMoveEscapes(int oldCursor, int newCursor) {
        final int from = Math.min(oldCursor, newCursor);
        final int to = Math.max(oldCursor, newCursor);
        final int newlinesCrossed = countNewlinesBetween(from, to);
        if (newlinesCrossed > 0) {
            sendRepeatedEscape((newCursor < oldCursor) ? "\u001b[A" : "\u001b[B", newlinesCrossed);
            return;
        }
        final int delta = newCursor - oldCursor;
        sendRepeatedEscape((delta < 0) ? "\u001b[D" : "\u001b[C", Math.abs(delta));
    }

    private int countNewlinesBetween(int from, int to) {
        int crossed = 0;
        for (int i = from; i < to; i++) {
            if (editorBuffer.charAt(i) == '\n') crossed++;
        }
        return crossed;
    }

    private void sendRepeatedEscape(String esc, int count) {
        for (int i = 0; i < count; i++) host.sendDirectText(esc);
    }

    private boolean applyImeText(String rawText, boolean commit) {
        final String text = normalizeImeCompositionText(rawText);
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            final String normalized = text.replace("\r", "\n");
            final String[] parts = normalized.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                final String part = parts[i];
                if (!part.isEmpty()) {
                    if (!applyImeText(part, commit)) return false;
                }
                if (i < parts.length - 1) {
                    host.sendDirectCodepoint('\r');
                    resetEditorState();
                }
            }
            return true;
        }
        if (commit && shouldSuppressCommitText(text)) return true;
        if (editorComposingStart >= 0 || !commit) {
            replaceComposition(text);
            if (commit) clearComposition();
            return true;
        }
        if (text.isEmpty()) return true;
        editorBuffer.insert(editorCursor, text);
        editorCursor += text.length();
        host.sendDirectText(text);
        return true;
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

    private void clearComposition() {
        editorComposingStart = -1;
        editorComposingEnd = -1;
    }

    private void replaceComposition(String next) {
        final String previous = currentCompositionText();
        final int composeStart = currentCompositionStart();
        final int oldEnd = currentCompositionEnd();
        final int commonPrefix = sharedPrefixLength(previous, next);
        for (int i = 0; i < (previous.length() - commonPrefix); i++) host.sendDirectCodepoint('\u007f');
        final String appended = next.substring(commonPrefix);
        if (!appended.isEmpty()) host.sendDirectText(appended);
        editorBuffer.delete(composeStart, oldEnd);
        editorBuffer.insert(composeStart, next);
        editorComposingStart = composeStart;
        editorComposingEnd = composeStart + next.length();
        editorCursor = editorComposingEnd;
        if (next.isEmpty()) clearComposition();
    }

    private String currentCommittedLinePrefix() {
        final int lineStart = editorLineStart();
        final int composeStart = currentCompositionStart();
        if (composeStart <= lineStart) return "";
        return editorBuffer.substring(lineStart, composeStart);
    }

    private String normalizeImeCompositionText(String text) {
        if (text.isEmpty()) return text;
        final String prefix = currentCommittedLinePrefix();
        if (prefix.isEmpty()) return text;
        if (!text.startsWith(prefix)) return text;
        return text.substring(prefix.length());
    }

    private static int sharedPrefixLength(String left, String right) {
        int n = 0;
        final int max = Math.min(left.length(), right.length());
        while (n < max && left.charAt(n) == right.charAt(n)) n++;
        return n;
    }

    private boolean shouldSuppressCommitText(String text) {
        if (suppressedCommitText == null || !suppressedCommitText.equals(text)) return false;
        suppressedCommitText = null;
        return true;
    }

    private void deleteTextBeforeCursorAndSendBackspace(int beforeLength) {
        if (beforeLength <= 0) return;
        final int delStart = Math.max(0, editorCursor - beforeLength);
        final int count = editorCursor - delStart;
        editorBuffer.delete(delStart, editorCursor);
        editorCursor = delStart;
        for (int i = 0; i < count; i++) host.sendDirectCodepoint('\u007f');
    }

    private void deleteTextAfterCursor(int afterLength) {
        if (afterLength <= 0) return;
        final int delEnd = Math.min(editorBuffer.length(), editorCursor + afterLength);
        editorBuffer.delete(editorCursor, delEnd);
    }

    private String mapKeyToEscape(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return "\u001b[A";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "\u001b[B";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "\u001b[C";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "\u001b[D";
            case KeyEvent.KEYCODE_MOVE_HOME: return "\u001b[H";
            case KeyEvent.KEYCODE_MOVE_END: return "\u001b[F";
            case KeyEvent.KEYCODE_INSERT: return "\u001b[2~";
            case KeyEvent.KEYCODE_FORWARD_DEL: return "\u001b[3~";
            case KeyEvent.KEYCODE_PAGE_UP: return "\u001b[5~";
            case KeyEvent.KEYCODE_PAGE_DOWN: return "\u001b[6~";
            default: return null;
        }
    }

    private Integer mapKeyToControlCodepoint(KeyEvent event, boolean ctrlActive) {
        if (!ctrlActive) return null;
        final int unicode = event.getUnicodeChar(KeyEvent.META_CTRL_ON);
        if (unicode != 0) {
            final Integer mapped = mapCodepointToControlCodepoint(unicode);
            if (mapped != null) return mapped;
        }
        if (event.getKeyCode() >= KeyEvent.KEYCODE_A && event.getKeyCode() <= KeyEvent.KEYCODE_Z) {
            return (event.getKeyCode() - KeyEvent.KEYCODE_A) + 1;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_LEFT_BRACKET: return 0x1b;
            case KeyEvent.KEYCODE_BACKSLASH: return 0x1c;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return 0x1d;
            case KeyEvent.KEYCODE_6: return 0x1e;
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_SLASH: return 0x1f;
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_2: return 0x00;
            default: return null;
        }
    }

    private Integer mapCodepointToControlCodepoint(int codepoint) {
        if (codepoint >= 'a' && codepoint <= 'z') return codepoint - 'a' + 1;
        if (codepoint >= 'A' && codepoint <= 'Z') return codepoint - 'A' + 1;
        switch (codepoint) {
            case '[': return 0x1b;
            case '\\': return 0x1c;
            case ']': return 0x1d;
            case '6': return 0x1e;
            case '-':
            case '/': return 0x1f;
            case ' ':
            case '2': return 0x00;
            default: return null;
        }
    }

    private boolean handleKeyEvent(KeyEvent event) {
        if (isActionMultiple(event.getAction())) {
            return handleActionMultipleKeyEvent(event);
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        final boolean ctrlActive = event.isCtrlPressed();
        final boolean altActive = event.isAltPressed();

        final Integer controlCodepoint = mapKeyToControlCodepoint(event, ctrlActive);
        if (controlCodepoint != null) {
            if (altActive) host.sendDirectCodepoint('\u001b');
            host.sendDirectCodepoint(controlCodepoint);
            return true;
        }

        final String esc = mapKeyToEscape(event.getKeyCode());
        if (esc != null) {
            if (altActive) host.sendDirectCodepoint('\u001b');
            host.sendDirectText(esc);
            return true;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL:
                host.sendDirectCodepoint('\u007f');
                if (editorCursor > editorLineStart()) {
                    editorBuffer.deleteCharAt(editorCursor - 1);
                    editorCursor--;
                }
                return true;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                host.sendDirectCodepoint('\r');
                resetEditorState();
                return true;
            case KeyEvent.KEYCODE_TAB:
                host.sendDirectCodepoint('\t');
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                host.sendDirectCodepoint('\u001b');
                return true;
            default:
                final int unicode = event.getUnicodeChar();
                if (unicode == 0 || Character.isISOControl(unicode)) return false;
                final String text = new String(Character.toChars(unicode));
                if (altActive) host.sendDirectCodepoint('\u001b');
                editorBuffer.insert(editorCursor, text);
                editorCursor += text.length();
                host.sendDirectText(text);
                return true;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isActionMultiple(int action) {
        return action == KeyEvent.ACTION_MULTIPLE;
    }

    @SuppressWarnings("deprecation")
    private boolean handleActionMultipleKeyEvent(KeyEvent event) {
        final String chars = event.getCharacters();
        if (chars == null || chars.isEmpty()) return false;
        host.sendDirectText(chars);
        return true;
    }

    private static boolean shouldBypassImePrintableKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (event.isCtrlPressed() || event.isAltPressed()) return false;
        if ((event.getFlags() & KeyEvent.FLAG_SOFT_KEYBOARD) == 0) return false;
        final int unicode = event.getUnicodeChar();
        return unicode != 0 && !Character.isISOControl(unicode);
    }
}
