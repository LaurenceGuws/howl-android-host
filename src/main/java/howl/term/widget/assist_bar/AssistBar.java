package howl.term.widget.assist_bar;

import howl.term.service.android.WindowRuntime;
import howl.term.service.android.WidgetRuntime;

import java.util.Arrays;
import java.util.List;

/** Assist bar widget handle. */
public final class AssistBar {
    private final List<String> shortcuts = Arrays.asList("IME", "Esc", "Tab", "Ctrl", "Alt", "Up", "Down", "Left", "Right");
    private final WindowRuntime runtime;
    private final Object handle;
    private final Object row;

    public AssistBar(WindowRuntime runtime) {
        this.runtime = runtime;
        this.handle = WidgetRuntime.horizontalScroll(runtime.context());
        this.row = WidgetRuntime.linearHorizontal(runtime.context());
        WidgetRuntime.setGravityCenterVertical(row);
        WidgetRuntime.setPaddingDp(row, runtime.context(), 4, 0, 4, 0);
        WidgetRuntime.addChild(handle, row, WidgetRuntime.Size.WRAP, WidgetRuntime.Size.MATCH);
    }

    public void setShortcuts() {
        WidgetRuntime.removeAllChildren(row);
        for (String label : shortcuts) {
            final Object button = WidgetRuntime.borderlessButton(runtime.context(), label);
            WidgetRuntime.setMinWidthDp(button, runtime.context(), 48);
            WidgetRuntime.addChild(row, button, WidgetRuntime.Size.WRAP, WidgetRuntime.Size.MATCH);
        }
    }

    public Object handle() {
        return handle;
    }
}
