package howl.term;

import howl.term.service.android.WindowRuntime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.nav_bar.NavBar;
import howl.term.widget.term_surface.Surface;

/** Window lifecycle coordinator and widget owner. */
public final class Window {
    private final WindowRuntime runtime;
    private final Surface surface;
    private final AssistBar assistBar;
    private final NavBar navBar;

    public Window(WindowRuntime runtime) {
        this.runtime = runtime;
        surface = new Surface(runtime);
        assistBar = new AssistBar(runtime);
        navBar = new NavBar(runtime);
    }

    public void start() {

        assistBar.setShortcuts();
        navBar.setTabs();

        runtime.mountSurface(surface.handle());
        runtime.mountAssistBar(assistBar.handle());
        runtime.mountNavBar(navBar.handle());
        runtime.initNavHidden();
        navBar.bindOpenFromLeftEdge(runtime.leftEdgeHandle(), navBar::open);
        navBar.bindSwipeOutClose(navBar::close);
    }

    public void resume() {
        surface.resume();
    }

    public void pause() {
        surface.pause();
    }
}
