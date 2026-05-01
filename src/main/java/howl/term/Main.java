package howl.term;

import howl.term.service.Input;
import howl.term.service.Window;
import howl.term.service.Userland;
import howl.term.widget.AssistBar;
import howl.term.widget.SidePanel;
import howl.term.widget.TermInstance;

/** App activity. */
public final class Main extends android.app.Activity {
    private final Window win = new Window();
    private final Input input = new Input();
    private TermInstance termInst;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private Userland userland;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        userland = new Userland(this);
        userland.start();

        final android.widget.FrameLayout root = win.root(this);
        final android.widget.FrameLayout appWindow = win.container(this);
        final android.widget.FrameLayout overlayWindow = win.container(this);
        final android.widget.FrameLayout surfaceBox = win.container(this);
        final android.view.View leftEdge = win.container(this);
        final android.view.View bottomEdge = win.container(this);
        final android.view.View overlayScrim = win.container(this);
        final int assistBarDp = 66;
        final int assistBarPx = Math.round(assistBarDp * getResources().getDisplayMetrics().density);
        final int bottomEdgePx = Math.round(54 * getResources().getDisplayMetrics().density);

        termInst = new TermInstance(userland);
        assistBar = new AssistBar(this, win);
        sidePanel = new SidePanel(this, win);
        final android.widget.FrameLayout.LayoutParams surfaceParams = win.fill();
        surfaceParams.bottomMargin = 0;
        win.mount(root, appWindow, win.fill());
        win.mount(root, overlayWindow, win.fill());

        win.mount(appWindow, surfaceBox, surfaceParams);
        win.mount(appWindow, assistBar.view(), win.bottomBar(this, assistBarDp));
        win.mount(appWindow, leftEdge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        final android.widget.FrameLayout.LayoutParams bottomParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                bottomEdgePx
        );
        bottomParams.gravity = android.view.Gravity.BOTTOM;
        win.mount(appWindow, bottomEdge, bottomParams);
        win.mount(surfaceBox, termInst.view(this), win.fill());

        win.setBackground(overlayScrim, 0x55000000);
        win.mount(overlayWindow, overlayScrim, win.fill());
        win.mount(overlayWindow, sidePanel.view(), win.leftPanel(this, 280));

        assistBar.view().setZ(20f);
        leftEdge.setZ(30f);
        bottomEdge.setZ(5f);
        overlayScrim.setZ(40f);
        sidePanel.view().setZ(50f);

        input.bindSwipeUp(bottomEdge, assistBar::show);
        input.bindSwipeDown(assistBar.view(), assistBar::hide);
        assistBar.setVisibilityListener(visible -> {
            bottomEdge.setVisibility(visible ? android.view.View.GONE : android.view.View.VISIBLE);
            final android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) surfaceBox.getLayoutParams();
            params.bottomMargin = visible ? assistBarPx : 0;
            surfaceBox.setLayoutParams(params);
            surfaceBox.requestLayout();
        });
        bottomEdge.setVisibility(android.view.View.VISIBLE);
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindOverlayScrim(overlayScrim);
        sidePanel.bindClose();
        win.keepScreenOn(this);
        win.setContent(this, root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userland == null) {
            throw new IllegalStateException("userland runtime missing");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (termInst != null) termInst.onResume();
    }

    @Override
    protected void onPause() {
        if (termInst != null) termInst.onPause();
        super.onPause();
    }
}
