package howl.term.widget.assist_bar;

import howl.term.service.android.WidgetRuntime;

import java.util.Arrays;
import java.util.List;

/** Assist bar widget handle. */
public final class AssistBar {
    private final List<String> shortcuts = Arrays.asList("IME", "Esc", "Tab", "Ctrl", "Alt", "Up", "Down", "Left", "Right");
    private final Object contextHandle;
    private final Object handle;
    private final Object row;

    public AssistBar(Object contextHandle) {
        this.contextHandle = contextHandle;
        this.handle = WidgetRuntime.horizontalScroll(contextHandle);
        this.row = WidgetRuntime.linearHorizontal(contextHandle);
        WidgetRuntime.setGravityCenterVertical(row);
        WidgetRuntime.setPaddingDp(row, contextHandle, 4, 0, 4, 0);
        WidgetRuntime.addChild(handle, row, WidgetRuntime.Size.WRAP, WidgetRuntime.Size.MATCH);
    }

    public void setShortcuts() {
        WidgetRuntime.removeAllChildren(row);
        for (String label : shortcuts) {
            final Object button = WidgetRuntime.borderlessButton(contextHandle, label);
            WidgetRuntime.setMinWidthDp(button, contextHandle, 48);
            WidgetRuntime.addChild(row, button, WidgetRuntime.Size.WRAP, WidgetRuntime.Size.MATCH);
        }
    }

    public Object handle() {
        return handle;
    }
}
