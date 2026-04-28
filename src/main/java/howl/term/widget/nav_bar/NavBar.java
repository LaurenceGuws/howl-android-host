package howl.term.widget.nav_bar;

import howl.term.service.android.WidgetRuntime;
import howl.term.service.android.input.InputRuntime;

import java.util.Arrays;
import java.util.List;

/** Nav bar widget handle. */
public final class NavBar {
    private final List<String> tabs = Arrays.asList("Session 1", "Session 2");
    private final Object contextHandle;
    private final Object containerHandle;
    private final Object handle;
    private final Object tabsColumn;

    public NavBar(Object contextHandle, Object containerHandle) {
        this.contextHandle = contextHandle;
        this.containerHandle = containerHandle;
        this.handle = WidgetRuntime.linearVertical(contextHandle);
        WidgetRuntime.setPaddingDp(handle, contextHandle, 12, 12, 12, 12);

        final Object title = WidgetRuntime.text(contextHandle, "Terminal Controls");
        WidgetRuntime.setTextColor(title, 0xFFF5F7FA);
        WidgetRuntime.setTextSizeSp(title, 18f);
        WidgetRuntime.addChild(handle, title, WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP);

        final Object label = WidgetRuntime.text(contextHandle, "Terminal sessions");
        WidgetRuntime.setTextColor(label, 0xFFC2CCD6);
        WidgetRuntime.addChild(handle, label, WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP);

        this.tabsColumn = WidgetRuntime.linearVertical(contextHandle);
        WidgetRuntime.addChildTopMarginDp(handle, tabsColumn, contextHandle, WidgetRuntime.Size.MATCH, WidgetRuntime.Size.WRAP, 8);
    }

    public void setTabs() {
        WidgetRuntime.removeAllChildren(tabsColumn);
        for (String name : tabs) {
            final Object button = WidgetRuntime.button(contextHandle, name);
            WidgetRuntime.addChildTopMarginDp(
                    tabsColumn,
                    button,
                    contextHandle,
                    WidgetRuntime.Size.MATCH,
                    WidgetRuntime.Size.WRAP,
                    6
            );
        }
    }

    public void open() {
        WidgetRuntime.showAtOrigin(containerHandle);
    }

    public void close() {
        WidgetRuntime.hideAnimatedLeft(containerHandle);
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
