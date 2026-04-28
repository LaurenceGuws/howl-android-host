package howl.term;

import howl.term.obj.android.WindowRuntime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.term_surface.Surface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowRuntime window = new WindowRuntime();
    private Surface surface;
    private AssistBar assistBar;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        final android.widget.FrameLayout root = window.root(this);
        final android.widget.FrameLayout surfaceBox = window.container(this);

        surface = new Surface();
        assistBar = new AssistBar(this, window);
        window.mount(root, surfaceBox, window.fill());
        window.mount(root, assistBar.view(), window.bottomBar(this, 44));
        window.mount(surfaceBox, surface.view(this), window.fill());
        window.keepScreenOn(this);
        window.setContent(this, root);
    }
}
