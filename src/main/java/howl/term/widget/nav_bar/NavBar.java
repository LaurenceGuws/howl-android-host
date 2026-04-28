package howl.term.widget.nav_bar;

import howl.term.service.android.WindowRuntime;
import howl.term.service.android.WidgetRuntime;
import howl.term.service.android.input.InputRuntime;

import java.util.Arrays;
import java.util.List;

/** Nav bar widget handle. */
public final class NavBar {
    private final List<String> tabs = Arrays.asList("Session 1", "Session 2");
    private final WindowRuntime runtime;
    private final Object handle;
    private final Object tabsColumn;

    public NavBar(WindowRuntime runtime) {
        this.runtime = runtime;
        this.handle = WidgetRuntime.linearVertical(runtime.context());
        WidgetRuntime.setPaddingDp(handle, runtime.context(), 12, 12, 12, 12);

        final Object title = WidgetRuntime.text(runtime.context(), "Terminal Controls");
        WidgetRuntime.setTextColor(title, 0xFFF5F7FA);
        WidgetRuntime.setTextSizeSp(title, 18f);
        WidgetRuntime.addChild(handle, title, WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP);

        final Object label = WidgetRuntime.text(runtime.context(), "Terminal sessions");
        WidgetRuntime.setTextColor(label, 0xFFC2CCD6);
        WidgetRuntime.addChild(handle, label, WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP);

        this.tabsColumn = WidgetRuntime.linearVertical(runtime.context());
        WidgetRuntime.addChildTopMarginDp(handle, tabsColumn, runtime.context(), WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP, 8);
    }

    public void setTabs() {
        WidgetRuntime.removeAllChildren(tabsColumn);
        for (String name : tabs) {
            final Object button = WidgetRuntime.button(runtime.context(), name);
            WidgetRuntime.addChildTopMarginDp(
                    tabsColumn,
                    button,
                    runtime.context(),
                    WidgetRuntime.Size.MATCH,
                    WidgetRuntime.Size.WRAP,
                    6
            );
        }
    }

    public void open() {
        runtime.openWidget(handle);
    }

    public void close() {
        runtime.closeWidget(handle);
    }

    public void bindSwipeOutClose(Runnable onClose) {
        InputRuntime.bindSwipeLeft(handle, onClose);
    }

    public void bindOpenFromLeftEdge(Object edgeHandle, Runnable onOpen) {
        InputRuntime.bindSwipeRight(edgeHandle, onOpen);
    }

    public Object handle() {
        return handle;
    }
}
