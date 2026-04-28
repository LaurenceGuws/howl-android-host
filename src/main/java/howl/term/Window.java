package howl.term;

import howl.term.service.android.WidgetRuntime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.nav_bar.NavBar;
import howl.term.widget.term_surface.Surface;

/** Window lifecycle coordinator and widget owner. */
public final class Window {
    private final Object surfaceContainer;
    private final Object assistContainer;
    private final Object navContainer;
    private final Object scrim;
    private final Object leftEdge;
    private final Surface surface;
    private final AssistBar assistBar;
    private final NavBar navBar;

    public Window(
            Object contextHandle,
            Object surfaceContainer,
            Object assistContainer,
            Object navContainer,
            Object scrim,
            Object leftEdge
    ) {
        this.surfaceContainer = surfaceContainer;
        this.assistContainer = assistContainer;
        this.navContainer = navContainer;
        this.scrim = scrim;
        this.leftEdge = leftEdge;
        surface = new Surface(contextHandle);
        assistBar = new AssistBar(contextHandle);
        navBar = new NavBar(contextHandle, navContainer);
    }

    public void start() {

        assistBar.setShortcuts();
        navBar.setTabs();

        WidgetRuntime.mountFill(surfaceContainer, surface.handle());
        WidgetRuntime.mountFill(assistContainer, assistBar.handle());
        WidgetRuntime.mountFill(navContainer, navBar.handle());
        WidgetRuntime.hideOffscreenLeft(navContainer);
        WidgetRuntime.setGone(scrim);
        navBar.bindOpenFromLeftEdge(leftEdge, navBar::open);
        navBar.bindSwipeOutClose(navBar::close);
    }

    public void resume() {
        surface.resume();
    }

    public void pause() {
        surface.pause();
    }
}
