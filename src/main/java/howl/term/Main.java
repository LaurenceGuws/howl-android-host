package howl.term;

import howl.term.service.Config;
import howl.term.service.Input;
import howl.term.service.Userland;
import howl.term.service.Window;
import howl.term.widget.AssistBar;
import howl.term.widget.SidePanel;
import howl.term.widget.TermInstance;

/** App activity object. */
public final class Main extends android.app.Activity {
    private final Window window = new Window();

    private Userland userland;
    private TermInstance termInstance;
    private AssistBar assistBar;
    private SidePanel sidePanel;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        userland = new Userland(this);
        userland.start();
        final Config cfg = Config.load(this);

        final android.widget.FrameLayout root = window.root(this);
        final android.widget.FrameLayout app = window.container(this);
        final android.widget.FrameLayout overlay = window.container(this);
        final android.widget.FrameLayout surfaceBox = window.container(this);
        final android.view.View leftEdge = window.container(this);
        final android.view.View bottomEdge = window.container(this);
        final android.view.View scrim = window.container(this);

        termInstance = new TermInstance(userland, cfg);
        assistBar = new AssistBar(this, window);
        sidePanel = new SidePanel(this, window);
        final android.view.View termView = termInstance.view(this);

        final int assistDp = 66;
        final int assistPx = Math.round(assistDp * getResources().getDisplayMetrics().density);
        final int bottomEdgePx = Math.round(54 * getResources().getDisplayMetrics().density);

        window.mount(root, app, window.fill());
        window.mount(root, overlay, window.fill());
        window.mount(app, surfaceBox, window.fill());
        window.mount(app, assistBar.view(), window.bottomBar(this, assistDp));

        final android.widget.FrameLayout.LayoutParams leftEdgeParams = new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        window.mount(app, leftEdge, leftEdgeParams);

        final android.widget.FrameLayout.LayoutParams bottomParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                bottomEdgePx
        );
        bottomParams.gravity = android.view.Gravity.BOTTOM;
        window.mount(app, bottomEdge, bottomParams);

        window.mount(surfaceBox, termView, window.fill());
        assistBar.setImeAnchor(termView);

        window.setBackground(scrim, 0x55000000);
        window.mount(overlay, scrim, window.fill());
        window.mount(overlay, sidePanel.view(), window.leftPanel(this, 280));

        assistBar.view().setZ(20f);
        leftEdge.setZ(30f);
        scrim.setZ(40f);
        sidePanel.view().setZ(50f);

        Input.bindSwipeUp(bottomEdge, assistBar::show);
        Input.bindSwipeDown(assistBar.view(), assistBar::hide);
        assistBar.setVisibilityListener(visible -> {
            bottomEdge.setVisibility(visible ? android.view.View.GONE : android.view.View.VISIBLE);
            final android.widget.FrameLayout.LayoutParams p =
                    (android.widget.FrameLayout.LayoutParams) surfaceBox.getLayoutParams();
            p.bottomMargin = visible ? assistPx : 0;
            surfaceBox.setLayoutParams(p);
        });
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindOverlayScrim(scrim);
        sidePanel.bindClose();

        window.keepScreenOn(this);
        window.setContent(this, root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (termInstance != null) termInstance.onResume();
    }

    @Override
    protected void onPause() {
        if (termInstance != null) termInstance.onPause();
        super.onPause();
    }
}
