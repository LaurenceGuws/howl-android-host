package howl.term;

import howl.term.obj.android.WindowRuntime;
import howl.term.obj.android.userland.Runtime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.side_panel.SidePanel;
import howl.term.widget.term_surface.Surface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowRuntime window = new WindowRuntime();
    private Surface surface;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private Runtime runtime;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        runtime = new Runtime();

        final android.widget.FrameLayout root = window.root(this);
        final android.widget.FrameLayout surfaceBox = window.container(this);
        final android.view.View edge = window.container(this);

        surface = new Surface();
        assistBar = new AssistBar(this, window);
        sidePanel = new SidePanel(this, window);
        window.mount(root, surfaceBox, window.fill());
        window.mount(root, sidePanel.view(), window.leftPanel(this, 280));
        window.mount(root, assistBar.view(), window.bottomBar(this, 44));
        window.mount(root, edge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        window.mount(surfaceBox, surface.view(this), window.fill());
        sidePanel.bindOpen(edge);
        sidePanel.bindClose();
        window.keepScreenOn(this);
        window.setContent(this, root);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
